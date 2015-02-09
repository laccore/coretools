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

class GeoUtils {
	// parse section top/base depth metadata file contents, return as a list of metadata maps, one per section
	static parseMetadataFile(metadataFile) {
		def metadata = []
		CSVReader reader = new CSVReader(new FileReader(metadataFile));
		def entries = reader.readAll()
		entries.eachWithIndex { row, index ->
			def secdata = ['section':row[0], 'top':row[1] as BigDecimal, 'base':row[2] as BigDecimal]
			metadata.add(secdata)
		}
		//metadata.each {	println "${it['section']} ${it['top']} ${it['base']} - length = ${it['base'].subtract(it['top'])}" }
		def sorted = metadata.sort { it.top }
		//sorted.each { println "${it['section']} ${it['top']} ${it['base']}" }
		return sorted
	}
		
	// reconcile metadata section IDs with those in current project: because PSICAT section IDs
	// come from image names in our workflow, some are appended with info about the image,
	// e.g "_lighter", so will not be a perfect match with metadata section IDs. Consider
	// a section that starts with the metadata section ID to be a match.
	// brgtodo: notify user when sections in metadata file are missing in project
	static reconcileSectionIDs(metadata, project) {
		metadata.each { sec ->
			def projSec = project.containers.find { it.startsWith(sec.section) }
			if (!projSec) {
				println "Couldn't find match for ${sec.section}"
			} else if (!projSec.equals(sec.section)) {
				println "Found match for ${sec.section}: $projSec"
				sec.section = projSec
			}
		}
	}
	
	static adjustUp(container, sectionTop) {
		container.models.each { m ->
			if (m.hasProperty('top') && m.top) {
				m.top = m.top - sectionTop
			}
			if (m.hasProperty('base') && m.base) {
				m.base = m.base - sectionTop
			}
			m.updated()
		}
	}
	
	static adjustDown(container, sectionTop) {
		container.models.each { m ->
			if (m.hasProperty('top') && m.top) {
				m.top = m.top + sectionTop
			}
			if (m.hasProperty('base') && m.base) {
				m.base = m.base + sectionTop
			}
			m.updated()
		}
	}
}