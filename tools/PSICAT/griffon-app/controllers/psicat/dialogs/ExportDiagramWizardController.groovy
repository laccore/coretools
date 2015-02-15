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
    		def filter
    		switch (view.format.selectedItem) {
    			case 'PDF':  filter = new CustomFileFilter(extensions:['.pdf'], description:'PDF Document (*.pdf)'); break
    			case 'PNG':  filter = new CustomFileFilter(extensions:['.png'], description:'PNG Image (*.png)'); break
    			case 'JPEG': filter = new CustomFileFilter(extensions:['.jpeg'], description:'JPEG Image (*.jpeg)'); break
    			case 'BMP':  filter = new CustomFileFilter(extensions:['.bmp'], description:'BMP Image (*.bmp)'); break
    			case 'SVG':  filter = new CustomFileFilter(extensions:['.svg'], description:'SVG IMage (*.svg)'); break
    		}
    		def file = Dialogs.showSaveDialog(model.title, filter, filter.extensions[0], app.appFrames[0])
    		if (file) { model.filePath = file.absolutePath }
    	}
    ]

    def show() {
    	if (Dialogs.showCustomDialog(model.title, view.root, app.appFrames[0])) {
    		def project = model.project

			// select a scene
			def scene
			if (project.scenes) {
				scene = SceneUtils.fromXML(project.scenes[0])
			}
			if (!scene) {
				scene = SceneUtils.fromXML(Platform.getService(ResourceLoader.class).getResource("rsrc:/templates/template.diagram"))
			}
    		scene.scalingFactor = 1000
			
    		// get our containers
    		def containers = app.controllers['exportDiagramSections'].containers
    		boolean appendName = containers.size() > 1

    		// export each containers
    		containers.each { k, v ->
    			// validate our scene
    			scene.models = v
    			scene.validate()
				
    			// figure out the extents
    			def start = model.exportAll ? scene.contentSize.minY / scene.scalingFactor : model.start as Double
    			def end = model.exportAll ? scene.contentSize.maxY / scene.scalingFactor : model.end as Double
    			def pageSize = model.pageSize ? model.pageSize as Double : Math.max(end - start, 1)
    			def paper = Paper.getDefault()	// TODO: handle DPI

    			// figure out the file name
    			def name = model.file?.name ?: k
    			int i = name.lastIndexOf('.')
    			if (i == -1) {
    				name = "${name}${appendName ? '_' + k : ''}.${view.format.selectedItem.toLowerCase()}"
    			} else {
    				name = name[0..<i] + (appendName ? "_$k" : '') + name[i..-1]
    			}

				// find section name
				def section = v.getModels().find { it.getModelType().equals("Section") }
				final String sectionName = section?.name 
				
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
					sectionName, new File(Dialogs.currentDir, name))
    		}
    		return "Exported " + (containers.size() == 1 ? (containers.keySet() as List)[0] : 'each section')
    	} else {
    		return ''
    	}
    }
}