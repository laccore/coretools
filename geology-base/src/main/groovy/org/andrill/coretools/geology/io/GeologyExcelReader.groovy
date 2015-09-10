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
import org.andrill.coretools.geology.GeologyFactory
import org.andrill.coretools.model.io.ModelWriter
import org.andrill.coretools.model.ModelContainer

class GeologyExcelReader {
	private workbook = null
	private factory = new GeologyFactory()
		
	String getFormat() { 'xls' }
	
	// read stream, return map of ModelContainers, one for each project-level section to be created
	def read(stream) {
		Workbook workbook = Workbook.getWorkbook(stream)
		
		def modelMap = [:] // all imported models are stored in lists keyed on section ID
		workbook.sheets.each { parseSheet(it, modelMap) }
		workbook.close()
		
		println "Created ${modelMap.size()} sections"
		
		return modelMap
	}
	
	def parseSheet(sheet, modelMap) {
		def modelType = getModelType(sheet.name)
		// (avoid certain sheets e.g. Images?)
		
		def model = factory.build(modelType, [:])
		println "Created ${modelType}...seeking columns named ${model.constraints.keySet()}"
		
		def constraints = model.constraints.keySet()
		
		// find indices of required colums
		println "Getting property columns for model type $modelType..."
		def modelPropIndices = getPropColumns(sheet, constraints)
		println "Result: ${modelPropIndices}"
		
		// get section ID column
		def sectionIDIndex = getColumnIndexByName(sheet, "Section ID")
		println "SectionID index = $sectionIDIndex"
		
		// create model with each row after header
		for (row in 1..<sheet.rows) {
			def newModel = createModelWithRow(sheet, row, modelType, modelPropIndices)
			def sectionID = sheet.getCell(sectionIDIndex, row).contents
			
			if (modelMap.containsKey(sectionID)) {
				modelMap[sectionID].add(newModel)
			} else {
				modelMap[sectionID] = [newModel]
			}
		}
	}
	
	// create and return model with properties in row's cells
	def createModelWithRow(sheet, row, modelType, modelPropIndices) {
		def props = [:]
		
		// for each required column, grab value in row and store in map
		modelPropIndices.each { key, col ->
			def value = sheet.getCell(col, row).contents
			if (value.length() > 0)
				props[key] = value
		}
		
		println "Creating model type $modelType with props $props"
		def newModel = factory.build(modelType, props)
		
		return newModel
	}
	
	// find column that starts with name in sheet, return column index or -1 if no match found
	private int getColumnIndexByName(sheet, name) {
		int index = -1
		for (i in 0..<sheet.columns) {
			if (sheet.getCell(i, 0).contents.startsWith(name)) {
				index = i
				break
			}
		}
		return index
	}
	
	// return map of property column indexes keyed on property name (e.g. 'top', 'description')
	private def getPropColumns(sheet, propNames) {
		def modelPropIndices = [:]
		propNames.each { it ->
			def colIndex = getColumnIndexByName(sheet, it)
			if (colIndex != -1) {
				modelPropIndices[it] = colIndex
			} else {
				println "   No matching column found for $it"
			}
		}
		return modelPropIndices
	}
	
	// derive model name from sheet name by stripping trailing 's'
	private def getModelType(str) { str.endsWith('s') ? str[0..-2] : str }
	
	// containers: map of containers keyed on section ID
	void write(containerMap, stream) {
		workbook = Workbook.createWorkbook(stream)
		containerMap.each { sectionID, container ->
			container.models.each {	addModelData(it, sectionID) }
		}
		workbook.write()
		workbook.close()
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
	
}