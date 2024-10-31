/*
 * Copyright (c) CSD Facility, 2014.
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
 * @author CSD Facility
 */
public class FileUtils {
	static String getExtension(final File f) {
		return getExtension(f, false);
	}
	
	static String getExtension(final File f, final boolean includeDot) {
		String extension = f.getName();
		int i = extension.lastIndexOf('.');
		if (i != -1) {
			int startIndex = includeDot ? i : i + 1;
			extension = extension.substring(startIndex);
		}
		return extension;
	}
	
	static String removeExtension(final File f) {
		return removeExtension(f.getName());
	}
	
	static String removeExtension(final String path) {
		return path.replaceFirst('[.][^.]+$', "");
	}
	
	private FileUtils() {
		// not to be instantiated
	}
}
