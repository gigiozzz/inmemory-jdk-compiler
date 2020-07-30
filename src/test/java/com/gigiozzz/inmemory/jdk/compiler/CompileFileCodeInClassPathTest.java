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


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 *
 * @author sportelli
 */
public class CompileFileCodeInClassPathTest {
    //@Test
    public void test1_classLoader(){
        try {
            Thread.currentThread().getContextClassLoader().loadClass("com.hscbt.wr.compiler.jdk.TestInterface");
            System.out.println("Current TestInterface ClassLoader is capable of loading external class");
        } catch (ClassNotFoundException ex) {
            System.out.println("Current TestInterface ClassLoader is NOT capable of loading external class");
            ex.printStackTrace();
        }
        
        try {
            this.getClass().getClassLoader().loadClass("com.hscbt.wr.compiler.jdk.TestInterface");
            System.out.println("Class TestInterface ClassLoader is capable of loading external class");
        } catch (ClassNotFoundException ex) {
            System.out.println("Class TestInterface ClassLoader is NOT capable of loading external class");
            ex.printStackTrace();
        }

    }
    
    @Test
    public void test2bis_hello() throws IOException {
        System.out.println("=======================================================================================");
        System.out.println("=======================================================================================");
        System.out.println("=======================================================================================");
        Reader reader = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("test/Hello.java"));
        String s = IOUtils.toString(reader);
        ClassLoader cl = null;
        try {
             cl = Thread.currentThread().getContextClassLoader();
             cl.loadClass("com.gigiozzz.inmemory.jdk.compiler.TestInterface");
				System.out.println("ClassLoader is capable of loading external class");
        } catch (ClassNotFoundException ex) {
            System.out.println("ClassLoader is NOT capable of loading external class");
            cl = this.getClass().getClassLoader();
        }

        final InMemoryCompiler uCompiler = InMemoryCompiler.javac();
        //uCompiler.withClasspathFrom(getClass().getClassLoader());
        uCompiler.withClasspathFrom(cl);
        
        InMemoryCompilerResult gc = uCompiler.compile(Arrays.asList(JavaFileObjectUtils.forResource("test/Hello.java")));

        //assertEquals(GoogleCompilation.Status.SUCCESS,gc.status());        
        if(InMemoryCompilerResult.Status.SUCCESS.equals(gc.status())){
            try {
            	/*
                for(JavaFileObject jfo: gc.generatedFiles()){
                    System.out.println("jfo source:"+jfo);
                    SimpleJavaFileObject k = (SimpleJavaFileObject)jfo;
                    System.out.println("jfo name:"+k.getName()+" "+k.toUri());
                    URI uri = InMemoryJavaFileManager.uriForFileObject(StandardLocation.CLASS_OUTPUT,"","test.Hello");
                }
                */
            	System.out.println("Compilation OK: "+gc.describeGeneratedSourceFiles());

                final Class<?> hello = gc.loadCompiledClass("test.Hello");
                Method mainMethod = hello.getDeclaredMethod("run", Map.class);
                Object instance = hello.newInstance();
                mainMethod.invoke(instance, new Object[]{new HashMap<>()});
                
            } catch(Exception ex){
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        } else {
        	System.out.println("Compilation KO: "+gc.toString());
        	System.out.println(""+gc.describeFailureDiagnostics());
        }
        
        
        assertEquals(true, InMemoryCompilerResult.Status.SUCCESS.equals(gc.status()));
    }
    
     
}
