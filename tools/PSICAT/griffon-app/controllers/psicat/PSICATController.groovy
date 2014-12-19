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
package psicat

import java.beans.PropertyChangeEvent
import java.util.prefs.Preferences

import org.andrill.coretools.Platform
import org.andrill.coretools.model.DefaultProject
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.edit.DeleteCommand
import org.andrill.coretools.dis.DISProject
import org.andrill.coretools.misc.io.ExcelReaderWriter
import org.andrill.coretools.misc.io.LegacyReader
import org.andrill.coretools.model.Project
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.graphics.util.Paper
import org.andrill.coretools.scene.DefaultScene
import org.andrill.coretools.misc.util.RenderUtils
import org.andrill.coretools.ui.ScenePanel
import org.andrill.coretools.ui.ScenePanel.Orientation
import org.andrill.coretools.ui.widget.Widget
import org.andrill.coretools.ui.widget.swing.SwingWidgetSet
import org.andrill.coretools.misc.util.LauncherUtils

import psicat.util.*

class PSICATController {
	def model
	def view

	private def prefs = Preferences.userNodeForPackage(PSICATController)
	
	void mvcGroupInit(Map args) {}

	// for working with MVC groups
	def withMVC(Map params = [:], String type, Closure closure) {
		def result
		try {
			result = closure(buildMVCGroup(params, type, type))
		} catch (e) {
			e.printStackTrace()
			Dialogs.showErrorDialog('Error', e.message)
		} finally {
			destroyMVCGroup(type)
		}
		return result
	}
	def getMVC(String id) {
		[model: app.models[id], view: app.views[id], controller: app.controllers[id]]
	}

	def ping(feature) { app.config?.usage?.ping(feature) }

	boolean canClose(evt) {
		return model.openDiagrams.inject(true) { flag, cur -> flag &= cur.controller.close() }
	}
	
	// 12/12/2014 brg: retain name to ensure we enable the panel that was actually disabled
	// (user could click on a different diagram's tab to activate window, so
	// we can't rely on currently active diagram's name)
	def disabledPanelName = "" 

	void enableMouse(evt) {
		// 12/12/2014 brg: fixes an obnoxious bug where clicking to activate window
		// created a new interval/symbol/unit if you happened to click in a track. 
		// Wait briefly to enable ScenePanel mouse handling so activate click is ignored. 
		doOutside {
			Thread.sleep(200);
			ScenePanel panel = getMVC(disabledPanelName)?.view?.contents
			if (panel) {
				panel.enableMouseHandling()
				disabledPanelName = ""
			}
		}
	}
	
	void disableMouse(evt) {
		ScenePanel panel = getMVC(model.diagramState.name)?.view?.contents
		if (panel) { 
			panel.disableMouseHandling()
			disabledPanelName = model.diagramState.name
		}
	}
	
	void openProject(project) {
		actions.closeAll()
		model.project = project
		getMVC('project').controller.project = project
		if (project) { model.status = "Opened project '${project.name}'" }
	}
	
	void closeProject() {
		actions.closeAll()
		def name = model.project.name
		model.project = null
		getMVC('project').controller.project = null
		model.status = "Closed project $name"
	}

	boolean closeDiagram(diagram) {
		if (diagram && diagram.controller.close()) {
			model.project.closeContainer(diagram.model.scene.models)
			int index = model.openDiagrams.indexOf(diagram)
			model.openDiagrams.remove(index)
			view.diagrams.removeTabAt(index)
			model.status = "Closed section '${diagram.model.name}'"
			return true
		} else {
			return false
		}
	}

	void activeDiagramChanged() {
		// deactivate previous diagram
		if (model.activeDiagram) { model.activeDiagram.controller.deactivate() }

		// figure out the new active diagram
		int index = view.diagrams.selectedIndex
		model.activeDiagram = index == -1 ? null : model.openDiagrams[index]
        if (model.activeDiagram) { 
        	model.activeDiagram.controller.activate(model.diagramState) 
        	view.rotateAction.putValue('Name', model.diagramState.vertical ? 'Horizontal' : 'Vertical')
	    }
		model.anyDirty = model.openDiagrams.inject(false) { dirty, cur -> dirty |= cur.model.dirty }

		// set the scene on the properties panel
        view.propertiesPanel.scene = model?.activeDiagram?.model?.scene
	}

	void setUnits(units) {
		ping("usingUnits $units")
		prefs.put("diagram.units", units)
		model.activeDiagram.controller.units = units
		model.status = "Changed units to $units"
	}

	void setZoom(pageSize) {
		prefs.putDouble("diagram.scaling", pageSize)
		model.activeDiagram.controller.zoom = pageSize
		model.status = "Set zoom to $pageSize ${model.activeDiagram.model.units}/page"
	}

	// our action implementations
	def actions = [
		'exit': { evt -> if (canClose(evt)) app.shutdown() },
		'newProject': { evt = null ->
			withMVC('NewProjectWizard') { mvc ->
				def project = mvc.controller.show()
				if (project && canClose(evt)) { 
					openProject(project)
					if (mvc.model.useCustomSchemes) {
						actions.chooseSchemes()
					} else {
						ProjectLocal.copyDefaultSchemes(project)
					}					
					if (mvc.model.importSections) {
						actions.importImage()
					}
				}
			}
		},
		'newSection': { evt = null ->
			withMVC('NewSectionWizard', project: model.project) { mvc ->
				def section = mvc.controller.show()
				if (section) { model.status = "Created new section '$section'" }
			}
		},
		'openProject': { evt = null ->
			def file = Dialogs.showOpenDirectoryDialog("Select Project Directory/Folder", null, app.appFrames[0])
			if (file && canClose(evt)) { openProject(new DefaultProject(file)) }
		},
		'closeProject': { evt = null ->
			if (canClose(evt)) closeProject()
		},
		'openSection': { evt = null ->
			// figure out our name and id
			def project = getMVC('project').view
			def sections = project.sections.selectedValues as List
			def id = sections.join('|')
			
			// check to make sure the diagram isn't open already
			def open = model.openDiagrams.find { it.model.id == id }
			if (open) {
				view.diagrams.selectedIndex = model.openDiagrams.indexOf(open)
			} else {
				def diagram = buildMVCGroup('Diagram', id, id: id, project: model.project, tabs: view.diagrams)
				if (diagram.controller.open()) {
					model.openDiagrams << diagram
					view.diagrams.addTab(diagram.model.name, diagram.view.viewer)
					view.diagrams.selectedIndex = model.openDiagrams.size() - 1
					diagram.model.scene.scalingFactor = (view.diagrams.size.height / diagram.model.scene.contentSize.height) * 4
					model.status = "Opened section '${diagram.model.name}'"
				} else {
					destroyMVCGroup(id)
				}
			}
		},
		'close': 	{ evt = null -> closeDiagram(model.activeDiagram) },
		'closeAll': { evt = null ->
			boolean canceled = false
			while (model.openDiagrams && !canceled) { canceled = !closeDiagram(model.openDiagrams[0]) } 
		},
		'save': 	{ evt = null -> 
			model.activeDiagram.controller.save()
			model.status = "Saved section '${model.activeDiagram.model.name}'"
		},
		'saveAll': 	{ evt = null -> 
			model.anyDirty = model.openDiagrams.inject(true) { dirty, diagram -> dirty &= diagram.controller.save() }
			model.status = "Saved all sections"
		},
		'delete': 	{ evt = null ->
			def active = model.activeDiagram.model
			active.scene.selection.selectedObjects.findAll { it instanceof Model }.each { m ->
				active.commandStack.execute(new DeleteCommand(m, active.scene.models))
				model.status = "Deleted $m"
			}
		},
		'undo':		{ evt = null -> model.diagramState.commandStack.undo() },
		'redo':		{ evt = null -> model.diagramState.commandStack.redo() },
		'zoomIn':	{ evt = null -> model.activeDiagram.model.scene.scalingFactor = model.activeDiagram.model.scene.scalingFactor * 1.2 },
		'zoomOut':	{ evt = null -> model.activeDiagram.model.scene.scalingFactor = model.activeDiagram.model.scene.scalingFactor * 0.8 },
		'zoom0':	{ evt = null -> setZoom(0.01) },
		'zoom1':	{ evt = null -> setZoom(0.10) },
		'zoom2':	{ evt = null -> setZoom(1) },
		'zoom3':	{ evt = null -> setZoom(10) },
		'zoom4':	{ evt = null -> setZoom(100) },
		'zoomOther':{ evt = null ->
			def other = Dialogs.showInputDialog('Zoom Level', 'Per page:', app.appFrames[0])
			if (other) {
				try { setZoom(other as Double) } catch (e) { /* ignore */ }
			}
		},
		'rotate': { evt = null ->
			def orientation = !model.diagramState.vertical
			prefs.put("diagram.orientation", orientation ? 'vertical' : 'horizontal')
			model.diagramState.vertical = orientation
			model.activeDiagram.controller.orientation = orientation
			view.rotateAction.putValue('Name', !orientation ? 'Vertical' : 'Horizontal')
			model.status = 'Diagram rotated'
		},
		'about': { evt = null ->
			Dialogs.showMessageDialog('About', """Welcome to PSICAT [${app.applicationProperties['app.version']}]

PSICAT is a graphical tool for creating and editing core description and stratigraphic column diagrams.
		""".toString(), app.appFrames[0])
		},
		'documentation': { evt = null ->
			LauncherUtils.openURL('http://dev.psicat.org/documentation/')
		},
		'feedback': { evt = null ->
			LauncherUtils.openURL('http://bitbucket.org/joshareed/coretools/issues/new/')
		},
		'exportDiagram': { evt = null -> ping('exportDiagram')
			withMVC('ExportDiagramWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'exportTabular': { evt = null -> ping('exportTabular')
			withMVC('ExportTabularWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'exportDIS': { evt = null -> ping('exportDIS')
			withMVC('ExportDISWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'importImage': { evt = null -> ping('importImage')
			withMVC('ImportImageWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'importLegacy': { evt = null -> ping('importLegacy')
			withMVC('ImportLegacyWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'importTabular': { evt = null -> ping('importTabular')
			withMVC('ImportTabularWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'chooseSchemes': { evt = null ->
			withMVC('ChooseSchemesDialog', project: model.project) { mvc ->
				if (mvc.controller.show())
					getMVC('project').controller.loadSchemes()
			}
		},
		'findAndReplace': { evt = null ->
			 def mockProp = new MockProp()
			 def sws = Platform.getService(SwingWidgetSet.class)
			 withMVC('FindReplace', project: model.project,
				 findWidget: sws.getWidget(mockProp, false),
				 replaceWidget: sws.getWidget(mockProp, false)) { mvc ->
				 mvc.controller.show()
			 }
		},
		'mUnits':  { evt = null -> setUnits('m') },
		'cmUnits': { evt = null -> setUnits('cm') },
		'ftUnits': { evt = null -> setUnits('ft') },
		'inUnits': { evt = null -> setUnits('in') },
		'openDIS': { evt = null ->
			def file = Dialogs.showOpenDialog("Open DIS Project", new CustomFileFilter(extensions: ['_dis.xml'], description: 'DIS files'), app.appFrames[0])
			if (file && canClose(evt)) { openProject(new DISProject(file)) }
		}
	]
}