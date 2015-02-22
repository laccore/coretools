/*
 * Copyright (c) Josh Reed, 2009.
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

import javax.swing.filechooser.FileFilter

class CustomFileFilter extends FileFilter {
	static final CustomFileFilter IMAGES = new CustomFileFilter(description: "Image files", extensions: [".bmp", ".jpg", ".jpeg", ".png", ".tif", ".tiff"])
	static final CustomFileFilter PDF = new CustomFileFilter(description: "PDF Document", extensions: [".pdf"])
	static final CustomFileFilter CSV = new CustomFileFilter(description: "Comma-seprated values (CSV)", extensions: [".csv"])
	static final CustomFileFilter EXCEL = new CustomFileFilter(description: "Excel Spreadsheet", extensions: [".xls"])
	
	List extensions = []
	String description
	
	boolean accept(File file) {
		file.isDirectory() || extensions.any { file.name.toLowerCase().endsWith(it) }
	}
}