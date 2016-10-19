/*
 * Copyright (c) Brian Grivna, 2016.
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

package psicat.stratcol

import au.com.bytecode.opencsv.CSVReader

import psicat.stratcol.StratColumnMetadataTypes as scmt
//import psciat.stratcol.StratColumnMetadata

import psicat.util.CustomFileFilter
import psicat.util.Dialogs

class StratColumnMetadataUtils {
	static int identifyMetadataFile(mdFile) throws Exception {
		def type = scmt.UnknownFile
		CSVReader reader = openMetadataFile(mdFile)
		def firstRow = reader.readNext()
		reader.close()
		if (firstRow.length == 3)
			type = scmt.SectionMetadataFile
		else if (firstRow.length == 15)
			 type = scmt.SpliceIntervalFile
		return type
	}
	
	static openMetadataFile(mdFile) throws Exception {
		CSVReader reader = null
		try {
			reader = new CSVReader(new FileReader(mdFile));
		} catch (e) {
			throw new Exception("Couldn't parse metadata file: ${e.getMessage()}", e)
		}
		return reader
	}
	
	static boolean isValidMetadataFile(path) {
		def format = identifyMetadataFile(path)
		def valid = (format == scmt.SectionMetadataFile || format == scmt.SpliceIntervalFile)
		return valid
	}
	
	static findSection(section, projSections, contains=false) {
		if (contains)
			return projSections.find { it.contains(section) }
		else
			return projSections.find { it.startsWith(section) }
	}
	
	static File chooseMetadataFile(parent=null) {
		def csvFilter = new CustomFileFilter(description: "CSV Files (*.csv)", extensions: [".csv"])
		return Dialogs.showOpenDialog("Select Section Metadata File", csvFilter, parent)
	}
}