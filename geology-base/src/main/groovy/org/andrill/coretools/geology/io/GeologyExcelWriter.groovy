/*
 * Copyright (c) CSD Facility, 2015
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
package org.andrill.coretools.geology.io

import jxl.*
import jxl.write.*
import jxl.write.Label

import org.andrill.coretools.Platform

import org.andrill.coretools.geology.models.*
import org.andrill.coretools.model.io.ModelWriter
import org.andrill.coretools.model.ModelContainer


private class ModelSchemeMetadata {
	String prop // name of Model's SchemeEntry property; should be "scheme" for all but old Andrill Interval, which uses "lithology"
	String type // Model's scheme type: bedding, features, grainsize, lithology, symbol, or texture
	String headerName // name of column header for human-readable scheme entry names

	final static schemeTypeToHeader = [
		'bedding': 'Bedding',
		'features': 'Feature',
		'grainsize': 'Grain Size',
		'lithology': 'Lithology',
		'symbol': 'Symbol',
		'texture': 'Texture',
 		'caementa': 'Caementa',
		'mortar': 'Mortar',
		'matrix': 'Matrix',
		'pores': 'Pores',
		'fractures': 'Fractures',
		'discontinuity': 'Discontinuity',
	]

	ModelSchemeMetadata(String prop, String type) {
		this.prop = prop
		this.type = type
		this.headerName = "${schemeTypeToHeader[type]} Name"
	}
}

class GeologyExcelWriter {
	private workbook = null
	private sheets = [:]
	private headers = [:]
	private rowCounts = [:]
	private sheetCount = 0
	private schemeManager = null
	private units = 'm'

	private final String SECTION_ID_HEADER = "Section ID"
	private final String SOURCE_SECTION_ID_HEADER = "Source Section ID"
	
	// Cache modelSchemeMetadata so we don't need to look it up 10k times.
	// k: Model.modelType (String), v: ModelSchemeMetadata
	private modelSchemeMetadata = [:]
		
	String getFormat() { 'xls' }
	void setSchemeManager(manager) { this.schemeManager = manager }
	void setUnits(units) { this.units = units }
	
	// containers: map of containers keyed on section ID
	void write(containerMap, stream, progressListener=null) {
		workbook = Workbook.createWorkbook(stream)
		containerMap.eachWithIndex { sectionID, container, index ->
			container.models.each {	
				addModelData(it, sectionID)
				updateProgress(progressListener, index / containerMap.size(), "Exporting $sectionID...")
			}
		}
		workbook.write()
		workbook.close()
	}
	
	void updateProgress(listener, text, pct) {
		if (listener) listener.progressChanged(text, pct)
	}
	
	void addModelData(model, sectionID) {
		def sheet = getSheet(model)
		addRow(model, sectionID, sheet)
	}
	
	void addRow(model, sectionID, sheet) {
		def head = headers[model.modelType]
		def row = rowCounts[model.modelType]
		model.modelData.each { k, v ->
			def value = v
			if (['top', 'base'].contains(k)) {
				// convert to project units, then strip units from Lengths
				value = new Length(v).to(units).value
			}

			// map sourceSection property name to user-facing header
			final headerName = k.equals('sourceSection') ? SOURCE_SECTION_ID_HEADER : k
			
			sheet.addCell(cell(head.indexOf(headerName), row, value))
		}
		
		def schemeMD = getModelSchemeMetadata(model)
		if (schemeMD) {
			def entry = getSchemeEntry(model, schemeMD.prop)
			if (entry) {
				def headerName = schemeMD.headerName
				sheet.addCell(cell(head.indexOf(headerName), row, entry.name))
			}
		}

		sheet.addCell(cell(head.indexOf(SECTION_ID_HEADER), row, sectionID))
		if (model.sourceSection) {
			sheet.addCell(cell(head.indexOf(SOURCE_SECTION_ID_HEADER), row, model.sourceSection))
		}
		rowCounts[model.modelType]++
	}
	
	def getSchemeEntry(model, schemeProp) {
		def entry = null
		def scheme = model."$schemeProp"?.scheme
		def code = model."$schemeProp"?.code
		
		if (scheme && code) {
			entry = schemeManager.getEntry(scheme, code)
		}
			
		return entry
	}
	
	private getModelSchemeMetadata(model) {
		if (model.modelType in modelSchemeMetadata) {
			return modelSchemeMetadata[model.modelType]
		}

		for (MetaProperty metaprop : model.metaClass.properties) {
			if (metaprop.type == SchemeRef.class) {
				def schemeProp = metaprop.name
				def schemeType = '[scheme type]' // default for Model (e.g. old Andrill Unit) without defined widgetProperties.schemeType
				def schemeConstraintsMap = model.constraints[schemeProp]
				if (schemeConstraintsMap) {
					// println("Got constraints for $schemeProp: $schemeConstraintsMap")
					def widgetProps = schemeConstraintsMap['widgetProperties']
					if (widgetProps) {
						// println("Got widgetProps: $widgetProps")
						if ('schemeType' in widgetProps.keySet()) {
							// println("Got schemeType ${widgetProps['schemeType']}")
							schemeType = widgetProps['schemeType']
						}
					}
				}
				modelSchemeMetadata[model.modelType] = new ModelSchemeMetadata(schemeProp, schemeType)
				break
			}
		}
		return modelSchemeMetadata[model.modelType]
	}

	void createHeaders(model, sheet) {
		if (!headers[model.modelType]) {
			def newHeaders = []
			model.constraints.each { k, v ->
				newHeaders << k

				// Add human-readable scheme type header/column immediately after scheme code header/column.
				// This column exists for convenience, and will not be consumed on tabular import.
				if (['scheme', 'lithology'].contains(k)) { // Interval is oddball Model with 'lithology' member/constraint instead of 'scheme'
					def schemeMD = getModelSchemeMetadata(model)
					if (schemeMD) {
						def schemeHeaderName = schemeMD.headerName
						if (schemeHeaderName) {
							newHeaders << schemeHeaderName
						}
					}
				}
			}
			
			newHeaders << SECTION_ID_HEADER
			newHeaders << SOURCE_SECTION_ID_HEADER
	
			// add header row with units to sheet
			newHeaders.eachWithIndex { name, col ->
				def h = name
				if (['top', 'base'].contains(name)) {
					h += " ($units)"
				}
				sheet.addCell(new Label(col, 0, h))
			}
			
			headers[model.modelType] = newHeaders
		}
	}
	
	private getSheet(model) {
		def type = model.modelType
		if (!sheets[type]) {
			def sheet = workbook.createSheet(type + 's', sheetCount++)
			createHeaders(model, sheet)
			sheets[type] = sheet
			rowCounts[type] = 1
		}
		return sheets[type]
	}
	
	private cell(c, r, value) {
		try {
			def number = new BigDecimal(value)
			return new jxl.write.Number(c, r, number)
		} catch (e) {
			return new Label(c, r, value)
		}
	}
}