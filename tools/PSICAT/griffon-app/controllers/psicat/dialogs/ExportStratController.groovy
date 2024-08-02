/*
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
import org.andrill.coretools.misc.util.StringUtils
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.geology.ui.ImageTrack
import org.andrill.coretools.geology.ui.RulerTrack
import org.andrill.coretools.geology.models.Length

import psicat.util.*

class ExportStratController {
    def model
    def view

    void mvcGroupInit(Map args) { 
		model.scene = app.controllers['PSICAT'].getStratColumnScene(args.project)
		model.diagramColumns = getDiagramColumnsText()
		model.scene.models = Platform.getService(ModelContainer.class) // must set a container
	}
    void mvcGroupDestroy() { destroyMVCGroup('exportStratSections') }

	private void updateDiagramColumns() {
		model.diagramColumns = getDiagramColumnsText()
	}

	private String getDiagramColumnsText() {
		return "<html>" + model.scene.tracks.collect { getColumnText(it) }.join("<br>") + "</html>"
	}

	private String getColumnText(def track) {
		if (track instanceof RulerTrack) {
			return "Ruler"
		} else {
			return track.header
		}
	}

    def actions = [
	    'browse': {
			def file = Dialogs.showSaveDirectoryDialog("Select Export Directory", null, view.root)
    		if (file) { model.filePath = file.absolutePath }
    	},
		'export': { 
			doOutside {
				view.exportBtn.enabled = false // disable to prevent starting a parallel Export
				export()
				view.exportBtn.enabled = true
			}
		},
		'diagramOptions': {
			final msg = "<html>Changes will be reflected only in exported stratigraphic columns.<br>Live diagram editing and exported diagrams will not be affected.</html>"
			app.controllers['PSICAT'].withMVC('DiagramOptions', scene:model.scene, diagramTypeText:msg) { mvc ->
				if (mvc.controller.show(view.root)) {
					if (mvc.model.sceneDirty) {
						updateDiagramColumns()
						if (model.project.scenes) {
							File stratColumnTemplate = new File(model.project.sceneDir, app.controllers['PSICAT'].STRATCOL_SCENE_FILE)
							if (stratColumnTemplate.exists()) {
								FileWriter writer = new FileWriter(stratColumnTemplate)
								SceneUtils.toXML(mvc.model.scene, writer)
							} else {
								println "Can't find stratcolumn.diagram, no dice!"
							}
						} else {
							println "Project has no diagrams, can't save"
						}
					} else {
						println "No changes to strat column scene, not saving."
					}
				} else {
					// User cancelled, no need to do anything here.
				}
			}			
		}
    ]

	def export() {
		def project = model.project

		// get our containers
		def containers = app.controllers['exportStratSections'].copyContainers()

		// prepare the scene
		def scene = model.scene
		scene.scalingFactor = 1000
		scene.setRenderHint("preferred-units", view.units.selectedItem)
		scene.setRenderHint("borders", Boolean.toString(model.renderColumnBorders))
		
		// export each container
		containers.eachWithIndex { k, v, index ->
			view.progress.value = (index / containers.size() * 100).intValue()
			view.progress.string = "Exporting $k"
			
			final String sectionName = view.title.text
			
			// validate our scene
			scene.models = v
			scene.validate()
			
			// figure out the extents
			// def start = model.exportAll ? scene.contentSize.minY / scene.scalingFactor : model.start as Double
			def start = scene.contentSize.minY / scene.scalingFactor
			// def end = model.exportAll ? scene.contentSize.maxY / scene.scalingFactor : model.end as Double
			def end = scene.contentSize.maxY / scene.scalingFactor
			// def pageSize = model.pageSize ? model.pageSize as Double : Math.max(end - start, view.units.selectedItem.equals("cm") ? 100 : 1)
			def pageSize = Math.max(end - start, view.units.selectedItem.equals("cm") ? 100 : 1)

			// println "scene.contentSize.maxY = ${scene.contentSize.maxY} start: $start, end: $end, end-start: ${end - start} pageSize: $pageSize"

			Paper paper = null
			if (model.standardFormat) {
				paper = view.paper.selectedItem
			} else {
				def paperWidth = Integer.parseInt(view.paperWidth.text)
				def paperHeight = Integer.parseInt(view.paperHeight.text)
				paper = new Paper("Custom", paperWidth, paperHeight, 36) // margin=36 i.e. 1/2" at 72dpi
			}

			// build file name
			// def name = (model.prefix ?: "${model.prefix}") + "$k.${view.format.selectedItem.toLowerCase()}"
			def name = (model.prefix ?: "${model.prefix}") + "${k}.pdf"
			
			// render
			def format = 'PDF'
			// switch (view.format.selectedItem) {	
			// 	case 'PDF': format = 'PDF'; break
			// 	case 'SVG': format = 'SVG'; break
			// 	default: format = 'Raster'
			// }
			
			// set rendering parameters
			if (!format.equals('Raster')) {
				// when rendering PDF or SVG, embed full-resolution image scaled to track bounds
				def imageTrack = scene.tracks.find { it instanceof ImageTrack }
				if (imageTrack) { imageTrack.setParameter("embed-image", "true") }
			}

			// Override the scene columns' draw-outline settings for now...
			scene.tracks.findAll { it.PARAMETERS.containsKey("draw-outline") }.each {
				it.setParameter("draw-outline", Boolean.toString(model.renderIntervalOutlines))
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