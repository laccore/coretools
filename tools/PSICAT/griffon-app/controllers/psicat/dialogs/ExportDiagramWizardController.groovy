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

import org.andrill.coretools.ResourceLoader
import org.andrill.coretools.Platform
import org.andrill.coretools.graphics.util.Paper
import org.andrill.coretools.misc.util.RenderUtils
import org.andrill.coretools.misc.util.SceneUtils
import org.andrill.coretools.geology.ui.ImageTrack

import psicat.util.*

class ExportDiagramWizardController {
    def model
    def view

    void mvcGroupInit(Map args) { }
    void mvcGroupDestroy() { destroyMVCGroup('exportDiagramSections') }

    def actions = [
	    'browse': {
			def file = Dialogs.showSaveDirectoryDialog("Select Export Directory", null, app.appFrames[0])
    		if (file) { model.filePath = file.absolutePath }
    	},
		'export': { 
			doOutside {
				view.exportBtn.enabled = false // disable to prevent starting a parallel Export
				export()
				view.exportBtn.enabled = true
			}
		}
    ]

	def export() {
		def project = model.project

		// get our containers
		def containers = app.controllers['exportDiagramSections'].copyContainers()
		boolean appendName = containers.size() > 1

		// select a scene
		def scene
		if (project.scenes) {
			scene = SceneUtils.fromXML(project.scenes[0])
		}
		if (!scene) {
			scene = SceneUtils.fromXML(Platform.getService(ResourceLoader.class).getResource("rsrc:/templates/template.diagram"))
		}
		scene.scalingFactor = 1000
		scene.setRenderHint("preferred-units", view.units.selectedItem)
		
		// export each container
		containers.eachWithIndex { k, v, index ->
			view.progress.value = (index / containers.size() * 100).intValue()
			view.progress.string = "Exporting $k"
			
			def sectionTop = 0.0
			def section = v.models.find { it.modelType == 'Section' }
			if (section) {
				sectionTop = section.top
				GeoUtils.adjustUp(v, sectionTop)
			}
			final String sectionName = section?.name ?: ""
			
			// validate our scene
			scene.models = v
			scene.validate()
			
			// figure out the extents
			def start = model.exportAll ? scene.contentSize.minY / scene.scalingFactor : model.start as Double
			def end = model.exportAll ? scene.contentSize.maxY / scene.scalingFactor : model.end as Double
			def pageSize = model.pageSize ? model.pageSize as Double : Math.max(end - start, 1)

			Paper paper = null
			if (model.standardFormat) {
				paper = view.paper.selectedItem
			} else {
				def paperWidth = Integer.parseInt(view.paperWidth.text)
				def paperHeight = Integer.parseInt(view.paperHeight.text)
				paper = new Paper("Custom", paperWidth, paperHeight, 36) // margin=36 i.e. 1/2" at 72dpi
			}

			// build file name
			def name = (model.prefix ?: "${model.prefix}") + "$k.${view.format.selectedItem.toLowerCase()}"
			
			// render
			def format
			switch (view.format.selectedItem) {	
				case 'PDF': format = 'PDF'; break
				case 'SVG': format = 'SVG'; break
				default: format = 'Raster'
			}
			
			// set rendering parameters
			if (!format.equals('Raster')) {
				// when rendering PDF or SVG, embed full-resolution image scaled to track bounds
				def imageTrack = scene.tracks.find { it instanceof ImageTrack }
				if (imageTrack) { imageTrack.setParameter("embed-image", "true") }
			}

			RenderUtils."render${format}"(scene, paper, start, end, pageSize, model.renderHeader, model.renderFooter,
				sectionName, new File(model.filePath, name))
		}
		
		view.progress.value = 100
		view.progress.string = "Export complete!"
	}
	
    def show() { 
		Dialogs.showCustomOneButtonDialog(model.title, view.root, app.appFrames[0])
		return ''
	}
}