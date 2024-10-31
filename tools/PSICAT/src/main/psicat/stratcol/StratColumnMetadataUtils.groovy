/*
 * Copyright (c) CSD Facility, 2016.
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
import org.andrill.coretools.misc.io.BOMAwareCSVReader

import psicat.stratcol.SpliceIntervalMetadata
import psicat.stratcol.StratColumnMetadataTypes as scmt

import psicat.util.CustomFileFilter
import psicat.util.Dialogs

class StratColumnMetadataUtils {
	static int identifyMetadataFile(mdFile) throws Exception {
		def type = scmt.UnknownFile
		def mdRows = openMetadataFile(mdFile)
		if (mdRows[0].length == 3)
			type = scmt.SectionMetadataFile
		else if (SpliceIntervalMetadata.isValid(mdRows))
			 type = scmt.SpliceIntervalFile
		return type
	}
	
	static List<String[]> openMetadataFile(mdFile) throws Exception {
		def rows = []
		try {
			BOMAwareCSVReader reader = new BOMAwareCSVReader(new CSVReader(new FileReader(mdFile)))
			reader.readAll().eachWithIndex { curRow, rowIndex ->
				rows << curRow
			}
			reader.close()
		} catch (e) {
			throw new Exception("Couldn't parse metadata file: ${e.getMessage()}", e)
		}

		return rows
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
		return Dialogs.showOpenDialog("Select a Section Metadata or Splice Interval File", csvFilter, parent)
	}
}