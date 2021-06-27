/*
 * Copyright (C) 2020 Luigi Sportelli.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gigiozzz.inmemory.jdk.compiler;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import io.github.classgraph.ClassGraph;

/**
 *
 * @author sportelli
 */
public class InMemoryCompiler {
	private static final Logger logger = LoggerFactory.getLogger(InMemoryCompiler.class);

	private JavaCompiler javaCompiler;
	private ImmutableList<Processor> processors;
	private ImmutableList<String> options;
	private Optional<ImmutableList<File>> classPath;
	private ClassLoader classLoader = this.getClass().getClassLoader();

	public InMemoryCompiler(JavaCompiler javaCompiler, ImmutableList<Processor> processors,
			ImmutableList<String> options, Optional<ImmutableList<File>> classPath) {

		this.javaCompiler = javaCompiler;
		this.processors = processors;
		this.options = options;
		this.classPath = classPath;
	}

	public JavaCompiler javaCompiler() {
		return javaCompiler;
	}

	public ImmutableList<Processor> processors() {
		return processors;
	}

	public ImmutableList<String> options() {
		return options;
	}

	/**
	 * The compilation class path. If not present, the system class path is used.
	 */
	public Optional<ImmutableList<File>> classPath() {
		return classPath;
	}

	/**
	 * Returns the {@code javac} compiler.
	 */
	public static InMemoryCompiler javac() {
		return compiler(getSystemJavaCompiler());
	}

	/**
	 * Returns a {@link Compiler} that uses a given {@link JavaCompiler} instance.
	 */
	public static InMemoryCompiler compiler(JavaCompiler javaCompiler) {
		return new InMemoryCompiler(javaCompiler, ImmutableList.of(), ImmutableList.of(), Optional.empty());
	}

	/**
	 * Uses annotation processors during compilation. These replace any previously
	 * specified.
	 *
	 * <p>
	 * Note that most annotation processors cannot be reused for more than one
	 * compilation.
	 *
	 * @return a new instance with the same options and the given processors
	 */
	public final InMemoryCompiler withProcessors(Processor... processors) {
		return withProcessors(ImmutableList.copyOf(processors));
	}

	/**
	 * Uses annotation processors during compilation. These replace any previously
	 * specified.
	 *
	 * <p>
	 * Note that most annotation processors cannot be reused for more than one
	 * compilation.
	 *
	 * @return a new instance with the same options and the given processors
	 */
	public final InMemoryCompiler withProcessors(Iterable<? extends Processor> processors) {
		return copy(ImmutableList.copyOf(processors), options(), classPath());
	}

	/**
	 * Passes command-line options to the compiler. These replace any previously
	 * specified.
	 *
	 * @return a new instance with the same processors and the given options
	 */
	public final InMemoryCompiler withOptions(Object... options) {
		return withOptions(ImmutableList.copyOf(options));
	}

	/**
	 * Passes command-line options to the compiler. These replace any previously
	 * specified.
	 *
	 * @return a new instance with the same processors and the given options
	 */
	public final InMemoryCompiler withOptions(Iterable<?> options) {
		return copy(processors(), FluentIterable.from(options).transform(toStringFunction()).toList(), classPath());
	}

	/**
	 * Uses the classpath from the passed on classloader (and its parents) for the
	 * compilation instead of the system classpath.
	 *
	 * @throws IllegalArgumentException if the given classloader had classpaths
	 *                                  which we could not determine or use for
	 *                                  compilation.
	 * @deprecated prefer {@link #withClasspath(Iterable)}. This method only
	 *             supports {@link URLClassLoader} and the default system
	 *             classloader, and {@link File}s are usually a more natural way to
	 *             expression compilation classpaths than class loaders.
	 */
	@Deprecated
	public final InMemoryCompiler withClasspathFrom(ClassLoader classloader) {
		this.classLoader = classloader;
		return copy(processors(), options(), Optional.of(getClasspathFromClassloader(classloader)));
	}

	/**
	 * Uses the given classpath for the compilation instead of the system classpath.
	 */
	public final InMemoryCompiler withClasspath(Iterable<File> classPath) {
		return copy(processors(), options(), Optional.of(ImmutableList.copyOf(classPath)));
	}

	/**
	 * Compiles Java source files.
	 *
	 * @return the results of the compilation
	 */
	public final InMemoryCompilerResult compile(JavaFileObject... files) {
		return compile(ImmutableList.copyOf(files));
	}

	/**
	 * Compiles Java source files.
	 *
	 * @return the results of the compilation
	 */
	public final InMemoryCompilerResult compile(Iterable<? extends JavaFileObject> files) {
		DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
		InMemoryJavaFileManager fileManager = new InMemoryJavaFileManager(
				javaCompiler().getStandardFileManager(diagnosticCollector, Locale.getDefault(), UTF_8), classLoader);
		classPath().ifPresent(cp -> {
			try {
				fileManager.setLocation(StandardLocation.CLASS_PATH, cp);
			} catch (IOException e) {
				// impossible by specification
				throw new UncheckedIOException(e);
			}
		});
		logger.debug("[InMemoryCompiler => compile] fileManager: '{}'", fileManager.getClass());

		CompilationTask task = javaCompiler().getTask(null, // use the default because old versions of javac log some
															// output on stderr
				fileManager, diagnosticCollector, options(), ImmutableSet.<String>of(), files);
		task.setProcessors(processors());
		boolean succeeded = task.call();
		logger.debug("[InMemoryCompiler => compile] fileManager: '{}'", fileManager.getClass());
		InMemoryCompilerResult compilation = new InMemoryCompilerResult(this, files, succeeded,
				diagnosticCollector.getDiagnostics(), fileManager);
		if (compilation.status().equals(InMemoryCompilerResult.Status.FAILURE) && compilation.errors().isEmpty()) {
			throw new InMemoryCompilerFailureException(compilation);
		}
		return compilation;
	}

	@VisibleForTesting
	static final ClassLoader platformClassLoader = getPlatformClassLoader();

	private static ClassLoader getPlatformClassLoader() {
		try {
			// JDK >= 9
			return (ClassLoader) ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
		} catch (ReflectiveOperationException e) {
			// Java <= 8
			return null;
		}
	}

	/**
     * Returns the current classpaths of the given classloader including its
     * parents.
     *
     * @throws IllegalArgumentException if the given classloader had classpaths
     * which we could not determine or use for compilation.
     */
    private static ImmutableList<File> getClasspathFromClassloader(ClassLoader currentClassloader) {
        long startExecution = System.currentTimeMillis();
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        // Concatenate search paths from all classloaders in the hierarchy 'till the system classloader.
        Set<String> classpaths = new LinkedHashSet<>();
        while (true) {
            if (currentClassloader == systemClassLoader) {
                Iterables.addAll(
                        classpaths,
                        Splitter.on(StandardSystemProperty.PATH_SEPARATOR.value())
                        .split(StandardSystemProperty.JAVA_CLASS_PATH.value()));
                break;
            }

//            if (currentClassloader == platformClassLoader) {
//                break;
//            }

			
			for (URL url : new ClassGraph().enableAnnotationInfo()
					.enableClassInfo()
					.enableExternalClasses().getClasspathURLs()) {
				if(url.getProtocol().equals("file")) {

					logger.debug("add path:'{}' url:'{}'", url.getPath(), url);
					classpaths.add(url.getPath());
				} else if (url.getProtocol().equals("jar")) {
					logger.debug("jar url:'{}'", url);

				} else {
					logger.debug("skipped protocol path:'{}' ", url);
					throw
					new IllegalArgumentException(
					"Given classloader consists of classpaths which are " +
					"unsupported for compilation."); }
				  
			}
			break;
			 
//            if (currentClassloader instanceof URLClassLoader) {
//                // We only know how to extract classpaths from URLClassloaders.
//                for (URL url : ((URLClassLoader) currentClassloader).getURLs()) {
//                    if (url.getProtocol().equals("file")) {
//                        classpaths.add(url.getPath());
//                    } else {
//                        throw new IllegalArgumentException(
//                                "Given classloader consists of classpaths which are "
//                                + "unsupported for compilation.");
//                    }
//                }
//            } else {
//                System.out.println("===============================================================");
//                System.out.println(""+currentClassloader.getClass().toString());
//                System.out.println("===============================================================");
//                throw new IllegalArgumentException(
//                        String.format(
//                                "Classpath for compilation could not be extracted "
//                                + "since %s is not an instance of URLClassloader",
//                                currentClassloader));
//            }

            //currentClassloader = currentClassloader.getParent();
                    
        }
                    
        long stopExecution = System.currentTimeMillis();
		logger.debug("Execution time ms:'{}' ", (stopExecution - startExecution));
        return classpaths.stream().map(File::new).collect(toImmutableList());
    }

	private InMemoryCompiler copy(ImmutableList<Processor> processors, ImmutableList<String> options,
			Optional<ImmutableList<File>> classPath) {
		return new InMemoryCompiler(javaCompiler(), processors, options, classPath);
	}
}
