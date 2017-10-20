/*
 * Copyright (c) Brian Grivna, 2015
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

class GeologyExcelWriter {
	private workbook = null
	private sheets = [:]
	private headers = [:]
	private rowCounts = [:]
	private sheetCount = 0
	private schemeManager = null
		
	String getFormat() { 'xls' }
	void setSchemeManager(manager) { this.schemeManager = manager }
	
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
			if (['top', 'base'].contains(k)) // strip units from Lengths
				value = new Length(v).value
			sheet.addCell(cell(head.indexOf(k), row, value))
		}
		
		// include human-readable lithology/symbol name in addition to with scheme:code string
		if (modelHasScheme(model)) {
			def entry = getSchemeEntry(model)
			if (entry) {
				def colName = getSchemeColumnName(model)
				sheet.addCell(cell(head.indexOf(colName), row, entry.name))
			}
		}

		sheet.addCell(cell(head.indexOf("Section ID"), row, sectionID))
		rowCounts[model.modelType]++
	}
	
	def getSchemeEntry(model) {
		def entry = null
		def schStr = model.modelType.equals("Interval") ? "lithology" : "scheme"
		def scheme = model."$schStr"?.scheme
		def code = model."$schStr"?.code
		
		if (scheme && code)
			entry = schemeManager.getEntry(scheme, code)
			
		return entry
	}
	
	// get appropriate name for human-readable lithology/symbol column
	def getSchemeColumnName(model) { model.modelType.equals("Interval") ? "lithology name" : "symbol name" }
	
	boolean modelHasScheme(model) {	return ["Interval", "Occurrence"].contains(model.modelType)	}
	
	void createHeaders(model, sheet) {
		if (!headers[model.modelType]) {
			def newHeaders = []
			model.constraints.each { k, v ->
				newHeaders << k
			}
			
			if (modelHasScheme(model))
				newHeaders << getSchemeColumnName(model)
			
			newHeaders << "Section ID"
	
			// add header row with units to sheet
			newHeaders.eachWithIndex { name, col ->
				def h = name
				if (['top', 'base'].contains(name))
					h += ' (m)'
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