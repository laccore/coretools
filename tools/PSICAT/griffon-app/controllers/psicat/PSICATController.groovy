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

import java.awt.Desktop

import java.beans.PropertyChangeEvent
import java.util.prefs.Preferences

import javax.swing.JOptionPane

import org.json.JSONObject

import org.andrill.coretools.Platform
import org.andrill.coretools.model.DefaultProject
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.edit.DeleteCommand
import org.andrill.coretools.dis.DISProject
import org.andrill.coretools.misc.io.ExcelReaderWriter
import org.andrill.coretools.misc.io.LegacyReader
import org.andrill.coretools.model.Project
import org.andrill.coretools.geology.models.Interval
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.ui.Scale
import org.andrill.coretools.geology.edit.DeleteIntervalCommand
import org.andrill.coretools.geology.edit.SplitIntervalCommand
import org.andrill.coretools.graphics.util.Paper
import org.andrill.coretools.scene.DefaultScene
import org.andrill.coretools.misc.util.RenderUtils
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

	boolean canClose(evt) {
		return model.openDiagrams.inject(true) { flag, cur -> flag &= cur.controller.close() }
	}
	
	// Is the selected directory a PSICAT project directory?
	boolean isProject(projFile) {
		def propsFile = new File(projFile, "project.properties") // simple, reliable-ish check
		propsFile.exists()	
	}
	
	Scale getGrainSize() {
		String code = model.project?.configuration?.grainSizeScale ?: Scale.DEFAULT
		return new Scale(code)
	}
	
	def getGrainSizeCode() {
		model.project?.configuration?.grainSizeScale ?: Scale.DEFAULT
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
		prefs.put("diagram.units", units)
		model.activeDiagram.controller.units = units
		model.status = "Changed units to $units"
	}

	void setZoom(pageSize) {
		prefs.putDouble("diagram.scaling", pageSize)
		model.activeDiagram.controller.zoom = pageSize
		model.status = "Set zoom to $pageSize ${model.activeDiagram.model.units}/page"
	}
	
	void deleteSection(section) {
		model.project.deleteContainer(section)
		model.status = "Deleted section $section"
	}
	
	List getSelectedSections() {
		def project = getMVC('project').view
		return project.sections.selectedValues as List
	}

	// silent: stifle messages if up-to-date or network error on auto-check at startup
	def versionCheck(silent) {
		def jsonObj = getLatestVersion(silent)
		if (jsonObj) {
			def cur = app.applicationProperties['app.version']
			def latest = jsonObj.getString("tag_name")
			if (isLatestVersion(cur, latest)) {
				if (!silent) { Dialogs.showMessageDialog("Up To Date", "PSICAT $cur is up to date.", app.appFrames[0]) }
			} else {
				if (JOptionPane.showConfirmDialog(null, "PSICAT $latest is available, open download page in default browser?",
					"New Version Available", JOptionPane.YES_NO_OPTION) == 0)
				{
					Desktop.getDesktop().browse(new URI(jsonObj.getString("html_url")))
				}
			}
		}
	}
	
	def getLatestVersion(silent) {
		def urlStr = 'https://api.github.com/repos/laccore/coretools/releases/latest'
		def reader = null
		def jsonObj = null
		try {
			reader = new BufferedReader(new InputStreamReader(new URL(urlStr).openStream()))
			def jsonResponse = ''
			def inputLine = null
			while ((inputLine = reader.readLine()) != null) {
				jsonResponse += inputLine
			}
			// brg 6/10/2015: Our ancient version of Griffon (0.2) is based on a version of Groovy
			// (1.6.4) in which griffon.json.JsonSlurper didn't exist (introduced in 1.8). Using
			// org.json library for now.
			jsonObj = new JSONObject(jsonResponse)
		} catch (IOException ioe) {
			if (!silent) { Dialogs.showErrorDialog("Error", "Latest version data unavailable") }
		} finally {
			if (reader) reader.close()
		}
		
		return jsonObj
	}
	
	// is version1 >= version2?
	boolean isLatestVersion(version1, version2) {
		def latest = null
		
		def v1 = version1.split('\\.')
		def v2 = version2.split('\\.')

		int index = 0
		def comp = 0
		while (index < v1.size() && index < v2.size()) {
			comp = Integer.parseInt(v1[index]) <=> Integer.parseInt(v2[index])
			if (comp != 0) {
				latest = (comp == 1) // implies v1 > v2, thus result = true
				break
			}
			index++
		}
		
		// if equivalent up to this point, longer version is greater
		if (latest == null)
			latest = v1.size() >= v2.size()
			
		latest
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
			if (file && canClose(evt)) {
				if (isProject(file)) { 
					openProject(new DefaultProject(file))
				} else {
					Dialogs.showErrorDialog("Open Project", "The selected directory is not a PSICAT project directory.", app.appFrames[0])
				}
			}
		},
		'closeProject': { evt = null ->
			if (canClose(evt)) closeProject()
		},
		'openSection': { evt = null ->
			// figure out our name and id
			def sections = getSelectedSections()
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
					
					// Force contentHeight to integer meters, then convert back to current units to compute scalingFactor.
					// This ensures initial ImageTrack width is consistent regardless of current units. Resolves issue
					// of ImageTrack using entire width of diagram when current unit is cm or in.  
					def contentHeight = diagram.model.scene.contentSize.height
					def intMeterHeight = Math.ceil(new Length(contentHeight, diagram.model.units).to('m').value)
					def normalizedHeight = new Length(intMeterHeight, 'm').to(diagram.model.units).value
					diagram.model.scene.scalingFactor = (view.diagrams.size.height / normalizedHeight) * 4
					
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
		'deleteSection': { evt = null ->
			def sections = getSelectedSections()
			if (sections.size() == 0) {
				Dialogs.showErrorDialog("Delete Section(s)", "No sections selected", app.appFrames[0])
				return
			}
			def msg = sections.size() > 1 ? "Delete ${sections.size()} selected sections?" : "Delete section ${sections[0]}?"
			def ret = JOptionPane.showConfirmDialog(app.appFrames[0], msg, "PSICAT", JOptionPane.YES_NO_OPTION)
			if (ret == JOptionPane.YES_OPTION) {
				sections.each { sectionName ->
					def indexToClose = model.openDiagrams.findIndexOf { it.model.id == sectionName }
					if (indexToClose != -1)
						closeDiagram(model.openDiagrams[indexToClose])
					deleteSection(sectionName)
				}
			}
		},
		'delete': 	{ evt = null ->
			def active = model.activeDiagram.model
			active.scene.selection.selectedObjects.findAll { it instanceof Model }.each { m ->
				if (m instanceof Interval) {
					active.commandStack.execute(new DeleteIntervalCommand(m, active.scene.models))	
				} else {
					active.commandStack.execute(new DeleteCommand(m, active.scene.models))
				}
				model.status = "Deleted $m"
			}
		},
		'splitInterval': { evt = null ->
			def active = model.activeDiagram.model
			def interval = active.scene.selection.selectedObjects.find { it instanceof Interval }
			if (interval) {
				active.commandStack.execute(new SplitIntervalCommand(interval, active.scene.models))
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
			Dialogs.showMessageDialog('About', """Welcome to PSICAT ${app.applicationProperties['app.version']}!

PSICAT is a graphical tool for creating and editing core description and stratigraphic column diagrams.

JRE Version: ${System.getProperty("java.version")}
JRE Vendor: ${System.getProperty("java.vendor")}
JRE Home: ${System.getProperty("java.home")}
		""".toString(), app.appFrames[0])
		},
		'documentation': { evt = null ->
			LauncherUtils.openURL('http://dev.psicat.org/documentation/')
		},
		'feedback': { evt = null ->
			LauncherUtils.openURL('https://docs.google.com/forms/d/1Jn--CnpLSXFeiW3DrULAw25r8IYHF0zshimBx4Utm6c/')
		},
		'exportDiagram': { evt = null ->
			withMVC('ExportDiagramWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'exportStratColumn': { evt = null ->
			withMVC('ExportStratColumnWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'exportTabular': { evt = null ->
			withMVC('ExportTabularWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'exportDIS': { evt = null ->
			withMVC('ExportDISWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'importImage': { evt = null ->
			withMVC('ImportImageWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'importLegacy': { evt = null ->
			withMVC('ImportLegacyWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'importTabular': { evt = null ->
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
		'grainSizeScale': { evt = null ->
			def result = JOptionPane.showInputDialog(app.appFrames[0], "Current grain size scale:", getGrainSizeCode())
			if (result) {
				try {
					def testScale = new Scale(result)
					model.project.configuration.grainSizeScale = result
					model.project.saveConfiguration()
				} catch (NumberFormatException e) {
					Dialogs.showErrorDialog("Invalid Grain Size Scale", "Invalid grain size scale: ${e.message}", app.appFrames[0])
				}
			}
		},
		'auditProject': { evt = null ->
			if (app.views.AuditProject == null)
				createMVCGroup('AuditProject', project: model.project)
		},
		'versionCheckSilent': { evt = null ->
			versionCheck(true)
		},
		'versionCheck': { evt = null ->
			versionCheck(false)
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