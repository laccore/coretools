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
	private logger = null
	private factory = new GeologyFactory()
		
	String getFormat() { 'xls' }
	
	// read stream, return map of ModelContainers, one for each project-level section to be created
	def read(stream, logger) {
		Workbook workbook = Workbook.getWorkbook(stream)
		this.logger = logger
		
		def modelMap = [:] // all imported models are stored in lists keyed on section ID
		workbook.sheets.each { parseSheet(it, modelMap) }
		workbook.close()
		
		logger.info("Read ${modelMap.size()} sections from file")
		
		return modelMap
	}
	
	def parseSheet(sheet, modelMap) {
		logger.warn("Parsing sheet ${sheet.name}...")
		def modelType = getModelType(sheet.name)
		// (avoid certain sheets e.g. Images?)
		
		def model = factory.build(modelType, [:])
		if (!model) {
			logger.error("Error: Unknown model type '$modelType'. Valid types are ${GeologyFactory.TYPES}")
			return
		}
		
		def constraints = model.constraints.keySet()
		logger.warn("Model type is ${modelType}...seeking columns named $constraints}")
		
		// find indices of required colums
		def modelPropIndices = getPropColumns(sheet, constraints)
		if (!modelPropIndices) {
			return
		}
		logger.warn("Found required columns: $modelPropIndices")
		
		// get section ID column
		def sectionIDIndex = getColumnIndexByName(sheet, "Section ID")
		logger.info("SectionID column index = $sectionIDIndex")
		
		// create model with each row after header
		def createCount = 0
		for (row in 1..<sheet.rows) {
			def newModel = createModelWithRow(sheet, row, modelType, modelPropIndices)
			if (!newModel) {
				logger.error("Error: Couldn't create model from row $row")
				continue
			}
			def sectionID = sheet.getCell(sectionIDIndex, row).contents
			
			if (modelMap.containsKey(sectionID)) {
				modelMap[sectionID].add(newModel)
			} else {
				modelMap[sectionID] = [newModel]
			}
			createCount++
		}
		
		logger.warn("Found $createCount models of type $modelType")
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
		
		logger.debug("Creating model type $modelType with props $props")
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
		def success = true
		def modelPropIndices = [:]
		propNames.each { it ->
			def colIndex = getColumnIndexByName(sheet, it)
			if (colIndex != -1) {
				modelPropIndices[it] = colIndex
			} else {
				logger.error("Error: No matching column found for $it, can't parse sheet")
				success = false
			}
		}
		success ? modelPropIndices : null
	}
	
	// derive model name from sheet name by stripping trailing 's'
	private def getModelType(str) { str.endsWith('s') ? str[0..-2] : str }
}