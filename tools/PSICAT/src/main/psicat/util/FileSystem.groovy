/*
 * Copyright (c) Brian Grivna, 2014
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

package psicat.util

class FileSystem {
	static void copy(src, dest) {
		def ant = new AntBuilder()
		ant.copy(file:"$src.canonicalPath", tofile:"$dest.canonicalPath")
	}
	
	static File copyImageFile(imageFile, projUrl) {
		def destDir = new File(new File(projUrl.toURI()), "images")
		if (!destDir.exists())
			destDir.mkdirs()
		def destFile = new File(destDir, imageFile.name)
		FileSystem.copy(imageFile, destFile)
		
		return destFile
	}
}