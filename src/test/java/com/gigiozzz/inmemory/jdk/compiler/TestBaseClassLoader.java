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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class TestBaseClassLoader {
	/* https://stackoverflow.com/questions/46494112/classloaders-hierarchy-in-java-9 */
	
	private static ClassLoader getPlatformClassLoader() {
        try {
            // JDK >= 9
            return (ClassLoader) ClassLoader.class.getMethod("getPlatformClassLoader").invoke(null);
        } catch (ReflectiveOperationException e) {
            // Java <= 8
            return null;
        }
    }
	
	public static void main(String[] args) {		
		
		 System.out.println("Classloader of this class: "+TestBaseClassLoader.class.getClassLoader());
		 System.out.println("SystemClassLoader: "+ClassLoader.getSystemClassLoader());
		 //System.out.println("Classloader of Logger: "+Logger.class.getClassLoader());
		 
		 System.out.println("PlatformClassLoader: "+getPlatformClassLoader());
		 
		 System.out.println("java.ext.dirs: "+System.getProperty("java.ext.dirs"));
		 System.out.println("java.system.class.loader: "+System.getProperty("java.system.class.loader"));
		 
		 
		 //System.out.println("Classloader of ZipCoder: "+ZipInfo.class.getClassLoader());
		 System.out.println("Classloader of ArrayList: "+ArrayList.class.getClassLoader());
		 
		 System.out.println("fully qualified class name:"+String.class.getName());
		 System.out.println("fully qualified class name:"+String.class.getSimpleName());
		 System.out.println("fully qualified class name:"+String.class.getCanonicalName());	
		 
		 try {
		 	ClassLoader.getSystemClassLoader().loadClass(""+String.class.getName());
		 } catch(ClassNotFoundException ex) {
			 ex.printStackTrace();
		 }
		 
		 
		 
		//primitive
		 System.out.println(int.class.getName());
		 System.out.println(int.class.getCanonicalName());
		 System.out.println(int.class.getSimpleName());

		 System.out.println();

		 //class
		 System.out.println(String.class.getName());
		 System.out.println(String.class.getCanonicalName());
		 System.out.println(String.class.getSimpleName());

		 System.out.println();

		 //inner class
		 System.out.println(HashMap.SimpleEntry.class.getName());
		 System.out.println(HashMap.SimpleEntry.class.getCanonicalName());
		 System.out.println(HashMap.SimpleEntry.class.getSimpleName());        

		 System.out.println();

		 //anonymous inner class
		 System.out.println(new Serializable(){}.getClass().getName());
		 System.out.println(new Serializable(){}.getClass().getCanonicalName());
		 System.out.println(new Serializable(){}.getClass().getSimpleName());
	}

		
}
