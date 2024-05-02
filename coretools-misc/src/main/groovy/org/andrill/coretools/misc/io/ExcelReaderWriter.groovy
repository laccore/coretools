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
package org.andrill.coretools.misc.io

import com.google.inject.Inject

import groovy.text.GStringTemplateEngine

import jxl.*
import jxl.write.*

import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.ModelManager;
import org.andrill.coretools.model.io.ModelReader;
import org.andrill.coretools.model.io.ModelWriter;

/**
 * Reads and writes models as Excel workbooks.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class ExcelReaderWriter implements ModelReader, ModelWriter {
	private final ModelManager factory
	
	String getFormat() { "xls" }
	
	@Inject
	ExcelReaderWriter(ModelManager factory) {
		this.factory = factory
	}
	
	protected def createDescriptorFromSheet(Sheet sheet) {
		// parse our columns
		def descriptor = [:]
		for (i in 0..<sheet.columns) {
			descriptor[i] = parseField(sheet, i)
		}
		return descriptor
	}
	
	private def parseField(sheet, i) {
		def map = [:]
		def value = sheet.getCell(i, 0).contents
		
		// save the original cell contents
		map.label = value
			
		// save the units
		if (value.contains("[") && value.contains("]")) {
			map.unit = value[value.indexOf('[')+1 .. value.indexOf(']')-1]
			value = value[0..<value.indexOf("[")]
		} else {
			map.unit = ""
		}
		
		// convert the cell to a property name
		map.property = StringUtils.camel(value)
			
		// save our template
		def template = sheet.getCell(i, 1).contents
		if (template && template.startsWith("{") && template.endsWith("}")) {
			map.template = template
		} else {
			map.template = "{$value}"
		}
		
		// create a template object
		def format = map.template[1..-2]
		map.format = new GStringTemplateEngine().createTemplate(format)
		map.pattern = compile(format)
		
		return map
	}
	
	protected def createDescriptorFromModel(Model model) {
		def descriptor = [:]
		// parse our properties
		model.modelData.eachWithIndex { k, v, i ->
			def field = [:]
			field.label = StringUtils.uncamel(k)
			field.unit = ""
			field.property = k
			field.template = '{$value}'
			field.format = new GStringTemplateEngine().createTemplate('$value')
			field.pattern = compile('$value')
			descriptor[i] = field
		}
		return descriptor
	}
	
	private def compile(pattern) {
		if (pattern.startsWith('$value') || pattern.startsWith('$unit')) {
			pattern = '^' + pattern
		}
		if (pattern.endsWith('$value') || pattern.endsWith('$unit')) {
			pattern = pattern + '$'
		}
		pattern = pattern.replace('$value', '(.*)')
		pattern = pattern.replace('$unit', '(.*?)')
		java.util.regex.Pattern.compile(pattern)
	}
	
	private def modelType(str) {
		str.endsWith('s') ? str[0..-2] : str
	}
	
	protected void parseSheet(Sheet sheet, ModelContainer container) {
		// de-pluralize sheet name to get the model type
		String type = modelType(sheet.name)
		def descriptor = createDescriptorFromSheet(sheet)
		
		// parse our models
		for (r in 2..<sheet.rows) {
			def data = [:]
			for (c in 0..<sheet.columns) {
				def field = descriptor[c]
				def value = sheet.getCell(c, r).contents
				if (value && value?.trim() != '') {
					if (field.pattern.matcher(value).matches()) {
						data[field.property] = value
					} else {
						def formatted = field.format.make([value:value, unit:field.unit]).toString()
						if (field.pattern.matcher(formatted).matches()) {
							data[field.property] = formatted
						} else {
							data[field.property] = value
						}
					}
				}
			}
			if (data) {
				def m = factory.build(type, data)
				if (m) {
					container.add(m)
				}
			}
		}
	}
	
	void read(ModelContainer container, InputStream stream) {
		Workbook workbook = Workbook.getWorkbook(stream)
		workbook.sheets.each { parseSheet(it, container) }
		workbook.close()
	}
	
	void write(ModelContainer container, OutputStream stream) {
		def descriptors = [:]
		
		// get the set of model types
		Set modelTypes = new HashSet()
		container.models.each { modelTypes << it.modelType }
		
		// create our writable workbook
		def workbook = Workbook.createWorkbook(stream)
		modelTypes.eachWithIndex { type, i ->
			def sheet
			container.models.findAll { it.modelType == type }.eachWithIndex { model, r ->
				if (!descriptors[type]) { descriptors[type] = createDescriptorFromModel(model) }
				if (!sheet) {
					sheet = workbook.createSheet(type + 's', i)
					createSheetFromDescriptor(sheet, descriptors[type])
				}
					
				descriptors[type].eachWithIndex { k, field, c ->
					def pattern = compile(field.template[1..-2].replace('$unit', field.unit))
					def value = model.modelData[field.property]
					if (value) {
						def matcher = pattern.matcher(value)
						if (matcher.matches()) {
							sheet.addCell(cell(matcher[0][1], c, r+2))
						} else {
							sheet.addCell(cell(value, c, r+2))
						}
					}
				}
			}
		}
		workbook.write()
		workbook.close()
	}
	
	private def cell(value, c, r) {
		try {
			def number = new BigDecimal(value)
			return new jxl.write.Number(c, r, number)
		} catch (e) {
			return new Label(c, r, value)
		}
	}
	
	protected void createSheetFromDescriptor(sheet, descriptor) {
		descriptor.eachWithIndex { k, v, c ->
			sheet.addCell(new Label(c, 0, v.label))
			sheet.addCell(new Label(c, 1, v.template))
		}
	}
}
