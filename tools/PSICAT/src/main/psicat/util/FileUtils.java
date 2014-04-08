/*
 * Copyright (c) Brian Grivna, 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package psicat.util;

import java.io.File;

/**
 * File-related utility methods.
 * 
 * @author Brian Grivna
 */
public class FileUtils {
	static String getExtension(final File f) {
		String extension = f.getName();
		int i = extension.lastIndexOf('.');
		if (i != -1) {
			extension = extension.substring(i + 1);
		}
		return extension;
	}
	
	static String removeExtension(final File f) {
		return f.getName().replaceFirst("[.][^.]+$", "");
	}
	
	private FileUtils() {
		// not to be instantiated
	}
}
