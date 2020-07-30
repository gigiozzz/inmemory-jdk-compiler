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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sportelli
 */
public class ForwardingStandardJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
        implements StandardJavaFileManager {

    private static final Logger logger = LoggerFactory.getLogger(ForwardingStandardJavaFileManager.class);

    /**
     * Creates a new instance of ForwardingStandardJavaFileManager.
     *
     * @param fileManager delegate to this file manager
     */
    public ForwardingStandardJavaFileManager(StandardJavaFileManager fileManager) {
        super(fileManager);
        logger.debug("[ForwardingStandardJavaFileManager] init Java File Manager Standard delegator");
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(
            Iterable<? extends File> files) {
        return fileManager.getJavaFileObjectsFromFiles(files);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
        return fileManager.getJavaFileObjects(files);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
        return fileManager.getJavaFileObjects(names);
    }

    @Override
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
        return fileManager.getJavaFileObjectsFromStrings(names);
    }

    @Override
    public void setLocation(Location location, Iterable<? extends File> path) throws IOException {
        fileManager.setLocation(location, path);
    }

    @Override
    public Iterable<? extends File> getLocation(Location location) {
        return fileManager.getLocation(location);
    }

    // @Override for JDK 9 only
	@Override
    public void setLocationFromPaths(Location location, Collection<? extends Path> searchpath)
            throws IOException {
        Method setLocationFromPaths;
        try {
            setLocationFromPaths
                    = fileManager
                    .getClass()
                    .getMethod("setLocationFromPaths", Location.class, Collection.class);
        } catch (ReflectiveOperationException e) {
            // JDK < 9
            return;
        }
        try {
            setLocationFromPaths.invoke(fileManager, location, searchpath);
        } catch (ReflectiveOperationException e) {
            throw new LinkageError(e.getMessage(), e);
        }
    }

    @Override
    public Iterable<JavaFileObject> list(Location location,
            String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse)
            throws IOException {
        logger.debug("[list] location:'{}' packageName:'{}'",location, packageName);
                
        return fileManager.list(location, packageName, kinds, recurse);
    }
}
