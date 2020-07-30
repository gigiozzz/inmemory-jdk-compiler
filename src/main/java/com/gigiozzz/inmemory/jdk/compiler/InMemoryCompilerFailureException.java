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


/**
 * An exception thrown to indicate that compilation has failed for an unknown reason.
 */
public class InMemoryCompilerFailureException extends RuntimeException {
  
	public InMemoryCompilerFailureException(InMemoryCompilerResult compilation) {
	    super(
	        compilation
	            + " failed, but did not report any error diagnostics or throw any exceptions. "
	            + "This behavior has been observed in older versions of javac, which swallow "
	            + "exceptions and log them on System.err. Check there for more information.");
	}
}
