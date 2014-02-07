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
package psicat.dialogs

import org.andrill.coretools.Platform
import org.andrill.coretools.misc.io.ExcelReaderWriter

import psicat.util.*

class ExportTabularWizardController {
	def model
	def view
	
	void mvcGroupInit(Map args) { }
	void mvcGroupDestroy() { destroyMVCGroup('exportTabularSections') }
	
	def actions = [
		'browse': {
			def filter
			switch (view.format.selectedItem) {
				case 'XLS':  filter = new CustomFileFilter(extensions:['.xls'], description:'Excel Workbook (*.xls)'); break
			}
			def file = Dialogs.showSaveDialog(model.title, filter, filter.extensions[0], app.appFrames[0])
			if (file) { model.filePath = file.absolutePath }
		}
	]
	
	def show() {
		if (Dialogs.showCustomDialog(model.title, view.root, app.appFrames[0])) {
			return exportTabular(app.controllers['exportTabularSections'].containers, view.format.selectedItem)
		} else {
			return ''
		}
	}
	
	private def exportTabular(containers, format) {
		// export each container
		boolean appendName = containers.size() > 1
		containers.each { k, v ->
			// figure out the file name
			def file = model.file ?: new File(k)
			def name = file.name
			int i = name.lastIndexOf('.')
			if (i == -1) {
				name = "${name}${appendName ? '_' + k : ''}.${format.toLowerCase()}"
			} else {
				name = name[0..<i] + (appendName ? "_$k" : '') + name[i..-1]
			}
			
			// write
			def writer
			switch (format) {
				case 'XLS': writer = Platform.getService(ExcelReaderWriter.class)
			}
			if (writer) {
				new File(file.parentFile, name).withOutputStream { writer.write(v, it) }
			}
		}
		return "Exported " + (containers.size() == 1 ? (containers.keySet() as List)[0] : 'each section')
	}
}