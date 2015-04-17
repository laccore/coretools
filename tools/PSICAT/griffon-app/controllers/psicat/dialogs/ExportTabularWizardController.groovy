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
import org.andrill.coretools.model.scheme.SchemeManager
import org.andrill.coretools.geology.io.GeologyExcelWriter

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
			return exportTabular(app.controllers['exportTabularSections'].copyContainers(), view.format.selectedItem)
		} else {
			return ''
		}
	}

	// export selected containers' contents into a single Workbook
	private def exportTabular(containers, format) {
		containers.each { k, v ->
			// assume single section per container - adjust model depths to section depth
			def section = v.find { it.modelType == "Section" }
			GeoUtils.adjustUp(v, section.top)
		}
		
		// determine output file path
		def defaultName = containers.size() > 1 ? model.project.name : (containers.keySet() as List)[0]
		def file = model.getFile(defaultName)
		def name = file.name
		int i = name.lastIndexOf('.')
		if (name.lastIndexOf('.') == -1) {
			name = "${name}.${format.toLowerCase()}"
		}
			
		// write
		def manager = Platform.getService(SchemeManager.class)
		def writer = Platform.getService(GeologyExcelWriter.class)
		if (writer) {
			writer.setSchemeManager(manager)
			new File(file.parentFile, name).withOutputStream { writer.write(containers, it) }
		}
		
		return "Exported " + (containers.size() == 1 ? (containers.keySet() as List)[0] : 'each section')
	}
}
