/*
 * Copyright (c) Brian Grivna, 2015.
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

import au.com.bytecode.opencsv.CSVReader

import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.geology.ui.Scale

class GeoUtils {
	// parse section top/base depth metadata file contents, return as a list of metadata maps, one per section
	static parseMetadataFile(metadataFile, project) throws Exception {
		def metadata = []
		CSVReader reader = null
		try {
			reader = new CSVReader(new FileReader(metadataFile));
		} catch (e) {
			throw new Exception("Couldn't parse metadata file: ${e.getMessage()}", e)
		}
		def entries = reader.readAll()
		entries.eachWithIndex { row, index ->
			def section = row[0]
			def projSection = project.containers.find { it.startsWith(section) } 
			if (projSection) {
				def top = null, base = null
				try {
					top = row[1] as BigDecimal
					base = row[2] as BigDecimal
				} catch (e) {
					throw new Exception("parsing error at row ${index + 1}: ${e.toString()}", e)
				}
				def secdata = ['section':projSection, 'top':top, 'base':base]
				metadata.add(secdata)
			} else {
				println "Couldn't find matching PSICAT section for ${section}"
			}
		}
		reader.close()
		//metadata.each {	println "${it['section']} ${it['top']} ${it['base']} - length = ${it['base'].subtract(it['top'])}" }
		def sorted = metadata.sort { it.top }
		//sorted.each { println "${it['section']} ${it['top']} ${it['base']}" }
		return sorted
	}
	
	// parse alternate grain size CSV file: row 1 should be a valid Scale string, remaining rows
	// consist of code and grain size columns. Returns map with 'scale' for scale string, 'gs' map 
	// of grain size values keyed on code
	static parseAlternateGrainSizeFile(altGSFile) throws Exception {
		CSVReader reader = null
		try {
			reader = new CSVReader(new FileReader(altGSFile))
		} catch (e) {
			throw new Exception("read/parse error: ${e.getMessage()}", e)
		}

		def result = [:]
		def gsmap = [:]
		if (reader) {
			def entries = reader.readAll()
			entries.eachWithIndex { row, index ->
				//println "row ${index + 1}: $row"
				if (index == 0) { // parse and verify scale
					try {
						def testScale = new Scale(row[0])
					} catch (NumberFormatException e) {
						throw new Exception("Invalid grain size scale: ${e.getMessage()}", e)
					}
					result['scale'] = row[0]
				} else {
					def code = row[0]
					def gs = null
					try {
						gs = new BigDecimal(row[1])
					} catch (e) {
						throw new Exception("Row ${index + 1}: invalid grain size value '${row[1]}'", e)
					} 
					gsmap[code] = gs 
				}
			}
			result['gs'] = gsmap
			reader.close()
		}
		return result
	}
	
	// notify listeners of change by default, but provide option to avoid doing so
	// in cases where we need to temporarily adjust to section depth (e.g. auditing
	// project) without "modified" asterisk showing up in open diagrams. 
	static adjustUp(container, sectionTop, update=true) {
		container.models.each { m ->
			if (m.hasProperty('top') && m.top) {
				m.top = m.top - sectionTop
			}
			if (m.hasProperty('base') && m.base) {
				m.base = m.base - sectionTop
			}
			if (update)
				m.updated()
		}
	}
	
	static adjustDown(container, sectionTop, update=true) {
		container.models.each { m ->
			if (m.hasProperty('top') && m.top) {
				m.top = m.top + sectionTop
			}
			if (m.hasProperty('base') && m.base) {
				m.base = m.base + sectionTop
			}
			if (update)
				m.updated()
		}
	}
}