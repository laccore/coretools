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

import java.util.regex.Pattern
import java.util.regex.Matcher

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
		
		logger.warn("Read ${modelMap.size()} sections from file")
		
		return modelMap
	}
	
	def parseSheet(sheet, modelMap) {
		logger.warn("Parsing sheet ${sheet.name}...")
		def modelType = getModelType(sheet.name)
		// (avoid certain sheets e.g. Images?)
		
		// create an empty GeologyModel to gather its properties to match against tabular data column names
		def model = factory.build(modelType, [:])
		if (!model) {
			logger.error("ERROR: Unknown model type '$modelType'. Valid types are ${GeologyFactory.TYPES}")
			return
		}
		
		def constraints = model.constraints.keySet()
		logger.warn("Model type is ${modelType}...seeking columns named $constraints")
		
		// find indices of required colums
		def propColumnMetadata = getPropColumnMetadata(sheet, constraints)
		if (!propColumnMetadata) { return }

		final colIdxStr = propColumnMetadata.collect { name, cmd -> "$name: ${cmd.index}" }.join(", ")
		logger.warn("Found required columns: $colIdxStr")

		// get section ID column
		def sectionColumn = getColumnMetadata(sheet, "Section ID")
		logger.warn("SectionID column index = ${sectionColumn.index}")
		
		// create model with each row after header
		def createCount = 0
		for (row in 1..<sheet.rows) {
			def newModel = createModelWithRow(sheet, row, modelType, propColumnMetadata)
			if (!newModel) {
				logger.error("ERROR: Couldn't create model from row $row")
				continue
			}
			def sectionID = sheet.getCell(sectionColumn.index, row).contents
			
			if (modelMap.containsKey(sectionID)) {
				modelMap[sectionID].add(newModel)
			} else {
				modelMap[sectionID] = [newModel]
			}
			createCount++
		}
		
		logger.warn("Found $createCount models of type $modelType")
	}
	
	// Create and return model with properties set to values of the contents of cells
	// in row that correspond to the columns in propColumnMetadata.
	def createModelWithRow(sheet, row, modelType, propColumnMetadata) {
		def props = [:]
		
		// for each required column, grab value in row and store in map
		propColumnMetadata.each { name, cmd ->
			def value = sheet.getCell(cmd.index, row).contents
			if (value.length() > 0) {
				if (cmd.units) { // add units to cell value for proper Length creation
					value += " ${cmd.units}"
				}
				props[name] = value
			}
		}
		
		logger.debug("Creating model type $modelType with props $props")
		def newModel = factory.build(modelType, props)
		
		return newModel
	}

	// Check for parenthesized units at the end of the header e.g. 'top (m)', 'base (cm)'.
	// If present, return unit String, otherwise null.
	private String getHeaderUnits(String header) {
		final units = Length.CONVERSIONS.keySet().join('|') // all the units Length handles: m, cm, mm, dm, hm, km, in, ft, yd
		
		Pattern unitPattern = Pattern.compile(".+\\((${units})\\)\$")
		Matcher m = unitPattern.matcher(header)
		if (m.matches()) {
			return m.group(1) // group 0 is always the entire input string, we want units
		}
		return null
	}
	
	// Find column header starting with name in sheet.
	// Return ColumnMetadata, or null if no match found.
	private ColumnMetadata getColumnMetadata(sheet, name) {
		ColumnMetadata cmd = null
		for (i in 0..<sheet.columns) {
			final String header = sheet.getCell(i, 0).contents
			if (header.startsWith(name)) {
				String units = getHeaderUnits(header)
				if (units) {
					logger.warn("Detected units '$units', using for all values in $header column")
				}
				if (!units && ['top', 'base'].contains(name)) {
					logger.warn("WARNING: no units detected for column $header, assuming meters")
				}
				cmd = new ColumnMetadata(index:i, units:units)
				break
			}
		}
		return cmd
	}
	
	// return map of ColumnMetadata objects keyed on property name (e.g. 'top', 'description')
	private def getPropColumnMetadata(sheet, propNames) {
		def success = true
		def propColumnMetadata = [:]
		propNames.each { name ->
			def cmd = getColumnMetadata(sheet, name)
			if (cmd) {
				propColumnMetadata[name] = cmd
			} else {
				logger.error("ERROR: No matching column found for property '$name', can't parse sheet")
				success = false
			}
		}
		success ? propColumnMetadata : null
	}
	
	// derive model name from sheet name by stripping trailing 's'
	private def getModelType(str) { str.endsWith('s') ? str[0..-2] : str }
}

class ColumnMetadata {
	int index = -1
	String units = null
}