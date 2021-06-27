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

import static javax.tools.JavaFileObject.Kind.CLASS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import javax.tools.JavaFileObject;

import org.junit.jupiter.api.Test;


public class JavaFileObjectUtilsTest {

	@Test
	public void forResource_inJarFile() {
		JavaFileObject resourceInJar = JavaFileObjectUtils
				.forResource("com/gigiozzz/inmemory/jdk/compiler/JavaFileObjectUtilsTest.class");
		assertEquals(CLASS, resourceInJar.getKind());
		assertTrue(resourceInJar.toUri().getPath().endsWith("/com/gigiozzz/inmemory/jdk/compiler/JavaFileObjectUtilsTest.class"));
		assertTrue(resourceInJar.getName().endsWith("/com/gigiozzz/inmemory/jdk/compiler/JavaFileObjectUtilsTest.class"));
		assertTrue(resourceInJar.isNameCompatible("JavaFileObjectUtilsTest", CLASS));
	}

	@Test
	public void forSourceLines() throws IOException {
		JavaFileObject fileObject = JavaFileObjectUtils.forSourceLines("example.HelloWorld", "package example;", "",
				"final class HelloWorld {", "  void sayHello() {", "    System.out.println(\"hello!\");", "  }", "}");
		assertEquals("package example;\n" + "\n" + "final class HelloWorld {\n" + "  void sayHello() {\n"
				+ "    System.out.println(\"hello!\");\n" + "  }\n" + "}", fileObject.getCharContent(false));
	}

	@Test
	public void forSourceLinesWithoutName() {
		assertThrows(IllegalArgumentException.class, () -> {
			JavaFileObjectUtils.forSourceLines("package example;", "", "final class HelloWorld {",
					"  void sayHello() {",
				"    System.out.println(\"hello!\");", "  }", "}");
		});
	}
}
