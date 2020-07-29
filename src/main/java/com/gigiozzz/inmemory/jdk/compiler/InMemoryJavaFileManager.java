/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.gigiozzz.inmemory.jdk.compiler;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;

/**
 *
 * @author sportelli
 */
public class InMemoryJavaFileManager extends ForwardingStandardJavaFileManager {
    private static final Logger logger = LoggerFactory.getLogger(ForwardingStandardJavaFileManager.class);
    private final ClassLoader classLoader;
    private final Map<URI, JavaFileObject> inMemoryFileObjects = new HashMap<>();
    /*
    private final LoadingCache<URI, JavaFileObject> inMemoryFileObjects
            = CacheBuilder.newBuilder().build(new CacheLoader<URI, JavaFileObject>() {
                @Override
                public JavaFileObject load(URI key) {
                    System.out.println("load from cache first:"+key);                    
                    return new GoogleInMemoryJavaFileObject(key);
                }
            });
    */
    InMemoryJavaFileManager(StandardJavaFileManager fileManager) {
        super(fileManager);
        this.classLoader = this.getClass().getClassLoader();
    }

    InMemoryJavaFileManager(StandardJavaFileManager fileManager,ClassLoader cl) {
        super(fileManager);
        this.classLoader = cl;
    }

    public static URI uriForFileObject(Location location, String packageName, String relativeName) {
        StringBuilder uri = new StringBuilder("mem:///").append(location.getName()).append('/');
        if (!packageName.isEmpty()) {
            uri.append(packageName.replace('.', '/')).append('/');
        }
        uri.append(relativeName);
        logger.debug("[InMemoryJavaFileManager => uriForFileObject]from location:'{}' packageName:'{}' relativeName:'{}' uri is:'{}'",
                new Object[]{location, packageName, relativeName, uri.toString()});
        return URI.create(uri.toString());
    }

        @Override
    public ClassLoader getClassLoader(Location location) {
        logger.debug("[InMemoryJavaFileManager => getClassLoader] with location:'{}'",location);
        return new ClassLoader(classLoader) {
            
            private URI uriForFileObject(Location location, String className) {
                int index = className.lastIndexOf(".");
                String  packageName = index > 0 ? className.substring(0, index) : "";
                className =  className.substring(index+1);
                return InMemoryJavaFileManager.uriForFileObject(location, packageName, className);
            }            
            
            @Override
            protected Class<?> findClass(final String className) throws ClassNotFoundException {
                logger.debug("[InMemoryJavaFileManager => getClassLoader.findClass] search for className:'{}'",className);
                
                URI key = this.uriForFileObject(location,className);
                logger.debug("[InMemoryJavaFileManager => getClassLoader.findClass] search with key:'{}'",key);
                final JavaFileObject jfo = inMemoryFileObjects.get(key);
                if(jfo == null){
					logger.debug("[InMemoryJavaFileManager => ClassLoader.findClass] "
							+ "JavaFileObject is null for\n key:'{}'\n from inMemoryFileObjects:'{}'\n size:'{}'",
							new Object[] { key, inMemoryFileObjects, inMemoryFileObjects.size() });
                    /*
                    ClassLoader parent = Optional.ofNullable(classLoader).orElseThrow(() -> new ClassNotFoundException(className));
                    return parent.loadClass(className);
                    */
                    return classLoader.loadClass(className);
                }
				logger.debug("[InMemoryJavaFileManager => ClassLoader.findClass] jfo not null for key:'{}'", key);
                
                //final ByteArrayOutputStream bos = buffers.get(className);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        IOUtils.copy(jfo.openInputStream(), bos);
                    } catch(IOException ex) {
                        throw new RuntimeException(ex);
                    }
                final byte[] b = bos.toByteArray();
                return super.defineClass(className, b, 0, b.length);
            }
        };
    }
    
    private static URI uriForJavaFileObject(Location location, String className, Kind kind) {
        return URI.create(
//                "mem:///" + location.getName() + '/' + className.replace('.', '/') + kind.extension);
                "mem:///" + location.getName() + '/' + className.replace('.', '/'));
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        /* This check is less strict than what is typically done by the normal compiler file managers
         * (e.g. JavacFileManager), but is actually the moral equivalent of what most of the
         * implementations do anyway. We use this check rather than just delegating to the compiler's
         * file manager because file objects for tests generally cause IllegalArgumentExceptions. */
        return a.toUri().equals(b.toUri());
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName,
            String relativeName) throws IOException {
        if (location.isOutputLocation()) {
            FileObject fo = inMemoryFileObjects.get(
                    uriForFileObject(location, packageName, relativeName));
            if(fo != null){
                return fo;
            } else {
				logger.debug("jfm [getFileForInput] not fonud for relativeName:'{}'", relativeName);
                throw new FileNotFoundException();
            }
        } else {
            return super.getFileForInput(location, packageName, relativeName);
        }
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind)
            throws IOException {
        if (location.isOutputLocation()) {
            JavaFileObject fo = inMemoryFileObjects.get(
                    uriForJavaFileObject(location, className, kind));
            if(fo != null){
                return fo;
            } else {
				logger.debug("jfm [getJavaFileForInput] id:'{}'", className);
                throw new FileNotFoundException();
            }

        } else {
            return super.getJavaFileForInput(location, className, kind);
        }
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName,
            String relativeName, FileObject sibling) throws IOException {
        URI uri = uriForFileObject(location, packageName, relativeName);
		logger.debug("[InMemoryJavaFileManager => getFileForOutput] Insert uri:'{}'", uri);
        FileObject fo = inMemoryFileObjects.get(uri);
        if(fo == null){
            fo = new InMemoryJavaFileObject(uri);
            inMemoryFileObjects.put(uri, (InMemoryJavaFileObject)fo);
        }
        return fo;
        /*
        return inMemoryFileObjects.getUnchecked(uri);
        */
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, final Kind kind,
            FileObject sibling) throws IOException {
        URI uri = uriForJavaFileObject(location, className, kind);
		logger.debug("[InMemoryJavaFileManager => getJavaFileForOutput] Insert uri:'{}'", uri);
        JavaFileObject fo = inMemoryFileObjects.get(uri);
        if(fo == null){
            fo = new InMemoryJavaFileObject(uri);
            inMemoryFileObjects.put(uri, fo);
        }
        return fo;
        /*
        return inMemoryFileObjects.getUnchecked(uri);
                */
    }

    ImmutableList<JavaFileObject> getGeneratedSources() {
        ImmutableList.Builder<JavaFileObject> result = ImmutableList.builder();
//        for (Entry<URI, JavaFileObject> entry : inMemoryFileObjects.asMap.entrySet()) {
        for (Entry<URI, JavaFileObject> entry : inMemoryFileObjects.entrySet()) {
            if (entry.getKey().getPath().startsWith("/" + StandardLocation.SOURCE_OUTPUT.name())
                    && (entry.getValue().getKind() == Kind.SOURCE)) {
                result.add(entry.getValue());
            }
        }
        return result.build();
    }

    ImmutableList<JavaFileObject> getOutputFiles() {
		logger.debug("[InMemoryJavaFileManager => getOutputFiles] JavaFileOutput  id:'{}'", this.toString());

//        return ImmutableList.copyOf(inMemoryFileObjects.asMap().values());
        return ImmutableList.copyOf(inMemoryFileObjects.values());
    }

    private static final class InMemoryJavaFileObject extends SimpleJavaFileObject
            implements JavaFileObject {

        private long lastModified = 0L;
        private Optional<ByteSource> data = Optional.empty();

        InMemoryJavaFileObject(URI uri) {
            super(uri, JavaFileObjectUtils.deduceKind(uri));
        }

        @Override
        public InputStream openInputStream() throws IOException {
            if (data.isPresent()) {
                return data.get().openStream();
            } else {
                throw new FileNotFoundException();
            }
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            return new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    super.close();
					logger.debug("openOutputStream byet lenght:'{}'", this.toByteArray().length);
                    data = Optional.of(ByteSource.wrap(toByteArray()));
					logger.debug("openOutputStream data lenght:'{}'", data.get().size());
                    lastModified = System.currentTimeMillis();
                }
            };
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            if (data.isPresent()) {
                return data.get().asCharSource(Charset.defaultCharset()).openStream();
            } else {
                throw new FileNotFoundException();
            }
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors)
                throws IOException {
            if (data.isPresent()) {
                return data.get().asCharSource(Charset.defaultCharset()).read();
            } else {
                throw new FileNotFoundException();
            }
        }

        @Override
        public Writer openWriter() throws IOException {
            return new StringWriter() {
                @Override
                public void close() throws IOException {
                    super.close();
					logger.debug("openWriter lenght:'{}'", this.toString().length());
                    data
                            = Optional.of(ByteSource.wrap(toString().getBytes(Charset.defaultCharset())));
                    lastModified = System.currentTimeMillis();
                }
            };
        }

        @Override
        public long getLastModified() {
            return lastModified;
        }

        @Override
        public boolean delete() {
            this.data = Optional.empty();
            this.lastModified = 0L;
            return true;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("uri", toUri())
                    .add("kind", kind)
                    .toString();
        }
    }
}
