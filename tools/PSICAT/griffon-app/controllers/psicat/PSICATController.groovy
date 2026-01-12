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
import java.io.FileWriter
import java.io.FileReader
import java.io.BufferedReader
import java.util.prefs.Preferences
import java.util.regex.Pattern

import javax.swing.*

import org.json.JSONObject

import org.apache.log4j.FileAppender
import org.apache.log4j.SimpleLayout

import org.andrill.coretools.Platform
import org.andrill.coretools.ResourceLoader
import org.andrill.coretools.model.DefaultProject
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.geology.models.csdf.*
import org.andrill.coretools.geology.ui.*
import org.andrill.coretools.geology.ui.csdf.*
import org.andrill.coretools.geology.models.*
import org.andrill.coretools.model.edit.*
import org.andrill.coretools.dis.DISProject
import org.andrill.coretools.misc.io.ExcelReaderWriter
import org.andrill.coretools.misc.io.LegacyReader
import org.andrill.coretools.misc.util.*
import org.andrill.coretools.model.Project
import org.andrill.coretools.geology.models.Interval
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.ui.Scale
import org.andrill.coretools.geology.edit.DeleteIntervalCommand
import org.andrill.coretools.geology.edit.SplitIntervalCommand
import org.andrill.coretools.graphics.util.Paper
import org.andrill.coretools.scene.DefaultScene
import org.andrill.coretools.scene.Scene
import org.andrill.coretools.scene.Scene.Origin
import org.andrill.coretools.ui.ScenePanel.Orientation
import org.andrill.coretools.ui.widget.Widget
import org.andrill.coretools.ui.widget.swing.SwingWidgetSet

import org.andrill.coretools.graphics.driver.ImageCache

import org.andrill.coretools.Platform

import psicat.stratcol.StratColumnMetadataUtils
import psicat.ui.GrainSizeDialog
import psicat.util.*

class PSICATController {
	def model
	def view

	private def prefs = Preferences.userNodeForPackage(PSICATController)

	public final String DIAGRAM_SCENE_FILE = "main.diagram"
	public final String STRATCOL_SCENE_FILE = "stratcolumn.diagram"
	
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
		// Platform.log("isProject(), looking in directory $projFile\npath=${projFile.absolutePath}")
		// Platform.log("is it a directory? ${projFile.isDirectory()}")
		// File[] files = projFile.listFiles()
		// Platform.log("directory contents:")
		// files.each {
			// Platform.log("  $it ${it.isDirectory() ? '(dir)' : '(file)'}")
		// }

		def propsFile = new File(projFile, "project.properties") // simple, reliable-ish check
		// Platform.log("Created project.properties File instance: $propsFile\npath=${propsFile.absolutePath}")
		def exists = propsFile.exists()
		// Platform.log("project.properties file exists? $exists")

		// Platform.log("Reading contents of project.properties file...")

        // try {
		// 	BufferedReader reader = new BufferedReader(new FileReader(propsFile))
        //     String line;
        //     while ((line = reader.readLine()) != null) {
        //         Platform.log("  $line");
        //     }
        // } catch (IOException e) {
        //     Platform.log("Error reading the file: " + e.getMessage());
        // }

		return exists
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

	void setFontSize(fontsize) {
		model.openDiagrams.each { it.controller.fontSize = fontsize }
		model.project.fontSize = fontsize
		model.project.saveConfiguration()
		model.status = "Set fontsize to $fontsize"
	}
	
	// void setUnits(units) {
	// 	prefs.put("diagram.units", units)
	// 	model.activeDiagram.controller.units = units
	// 	model.status = "Changed units to $units"
	// }

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
	// assumes semantic versioning: X.Y.Z-optional prerelease]
	// X.Y.Z is considered later than X.Y.Z-prerelease
	boolean isLatestVersion(version1, version2) {
		def latest = null
		def v1 = parseVersion(version1)
		def v2 = parseVersion(version2)

		for (int index = 1; index <= 3; index++) {
			def comp = Integer.parseInt(v1.group(index)) <=> Integer.parseInt(v2.group(index))
			if (comp != 0) {
				latest = (comp == 1) // implies v1 > v2, thus result = true
				break
			}
		}
		
		// if equivalent up to this point, check for prerelease label
		if (latest == null) {
			def v1pr = v1.group(5) // group 4 is the hyphen or underscore prefix, group 5 is the prerelease
			def v2pr = v2.group(5)
			if (v1pr && !v2pr) {
				latest = false
			} else {
				// latest = true because:
				// - v1 has no prerelease and v2 does implying v1 is latest, or
				// - neither has a prerelease so they're equal, thus v1 is latest, or
				// - both have a prerelease but we can't compare them, call v1 latest by default.
				latest = true
			}
		}
		return latest
	}
	
	private parseVersion(version) {
		// X.Y.Z are required. Version may include - or _ prefixed prerelease label.
		String regex = "([0-9]+)\\.([0-9]+)\\.([0-9]+)([-_])?([A-Za-z][A-Za-z0-9.+]+)?"
		def pattern = Pattern.compile(regex)
		def matcher = pattern.matcher(version)
		matcher.matches()
		return matcher
	}

	private addSceneToProject(project, scene, name) {
		File sceneDir = project.sceneDir
		sceneDir.mkdirs()
		File sceneFile = new File(sceneDir, name)
		SceneUtils.toXML(scene, new FileWriter(sceneFile))
		project.scenes.add(sceneFile.toURI().toURL())
	}

	Scene getDiagramScene(project) {
		def scene = null
		if (project.scenes) {
			scene = SceneUtils.fromXML(project.scenes[0])
		}
		if (!scene) {
			scene = SceneUtils.fromXML(Platform.getService(ResourceLoader.class).getResource("rsrc:/templates/main.diagram"))
			addSceneToProject(project, scene, DIAGRAM_SCENE_FILE)
		}
		return scene
	}

	Scene getStratColumnScene(project) {
		def scene = null
		if (project.scenes) {
			File scFile = new File(project.sceneDir, STRATCOL_SCENE_FILE)
			if (scFile.exists()) {
				URL scUrl = scFile.toURI().toURL()
				scene = SceneUtils.fromXML(scUrl)
			}
		}
		if (!scene) {
			scene = SceneUtils.fromXML(Platform.getService(ResourceLoader.class).getResource("rsrc:/templates/stratcolumn.diagram"))
			addSceneToProject(project, scene, STRATCOL_SCENE_FILE)
		}
		return scene
	}

	private List<String> gatherProjectModelTypes(List<String> excludeTypes) {
		HashSet modelTypeSet = new HashSet()
		model.project.containers.each { containerName ->
			def container = model.project.openContainer(containerName)
			container.models.each { model ->
				if (!excludeTypes.contains(model.modelType)) {
					modelTypeSet.add(model.modelType)
				}
			}
			model.project.closeContainer(container)
		}
		return modelTypeSet as List
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
						ProjectLocal.copyDefaultSchemes(project, mvc.model.defaultSchemePaths)
					}
					if (mvc.model.importSections) {
						actions.importImage()
					}
					// 6/9/2024: As of 1.2.0, all new projects are created with a diagrams dir and default diagram scene.
					def scene = SceneUtils.fromXML(Platform.getService(ResourceLoader.class).getResource("rsrc:/templates/main.diagram"))
					addSceneToProject(project, scene, DIAGRAM_SCENE_FILE)
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
					if (model.project) {
						if (!model.project.sceneDir.exists()) {
							// project has no sceneDir ("diagrams" by default). As of 1.2.0, all new projects
							// are created with a sceneDir and the default diagram template (main.diagram).
							// So this project must be an old legacy project. Create a sceneDir and add the old
							// default diagram template (template_pre120.diagram).
							Platform.log("$file contains a legacy PSICAT project, adding old template!")
							def scene = SceneUtils.fromXML(Platform.getService(ResourceLoader.class).getResource("rsrc:/templates/template_pre120.diagram"))
							addSceneToProject(model.project, scene, DIAGRAM_SCENE_FILE)
						}
					}
				} else {
					Dialogs.showErrorDialog("Open Project", "The selected directory is not a PSICAT project directory.", app.appFrames[0])
				}
			}
		},
		'closeProject': { evt = null ->
			if (canClose(evt)) closeProject()
		},
		'openSection': { evt = null, sectionToOpen = null ->
			// figure out our name and id
			def sections = sectionToOpen ? [sectionToOpen] : getSelectedSections()
			def id = sections.join('|')

			Platform.log("openSection $sections: Clearing Image Cache to force reload of images...")
			ImageCache cache = Platform.getService(ImageCache.class);
			cache.clear()

			// check to make sure the diagram isn't open already
			def open = model.openDiagrams.find { it.model.id == id }
			if (open) {
				view.diagrams.selectedIndex = model.openDiagrams.indexOf(open)
			} else {
				model.status = "Opening section '$id'..."
				
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
		'createStratColumn': { evt = null ->
			withMVC('OpenStratColumnDepths', project:model.project, metadataPath:null) { oscdMVC ->			
				oscdMVC = buildMVCGroup('OpenStratColumnDepths', project:model.project, metadataPath:null)
				oscdMVC.view.openSCMD.setLocationRelativeTo(app.appFrames[0])
				oscdMVC.view.openSCMD.setVisible(true)
				if (!oscdMVC.model.confirmed) { return }

				doOutside {
					def pb = ProgressBarFactory.create("Building list of project models...")
					pb.setVisible(true)
					pb.setLocationRelativeTo(app.appFrames[0])
					
					// exclude Section option in UI, it always exists and must included
					List<String> modelTypes = gatherProjectModelTypes(["Section"])

					pb.setVisible(false)

					HashMap<String,String> modelsAndHelpText = new HashMap<String,String>()
					modelTypes.each { modelsAndHelpText[it] = null }
					if (modelsAndHelpText.containsKey('Image')) {
						modelsAndHelpText['Image'] = '<html>Untrimmed images with features above or below the core (labels, color cards, etc.) will<br>yield inaccurate columns with differential compression.</html>'
					}
					if (modelsAndHelpText.containsKey('GrainSizeInterval')) {
						modelsAndHelpText['GrainSizeInterval'] = 'Include Grain Size to display scaled Lithology column.'
					}

					withMVC('CreateStratColumn',
							project:model.project,
							stratMetadata:oscdMVC.model.stratColumnMetadata,
							stratColumnName:FileUtils.removeExtension(new File(oscdMVC.model.metadataPath)),
							modelsAndHelpText:modelsAndHelpText) { cscMVC -> cscMVC.controller.show() }
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
				if (GeoUtils.isIntervalInstance(m)) {
					active.commandStack.execute(new DeleteIntervalCommand(m, active.scene.models, active.scene.origin == Origin.TOP))	
				} else {
					active.commandStack.execute(new DeleteCommand(m, active.scene.models))
				}
				model.status = "Deleted $m"
			}
		},
		'splitInterval': { evt = null ->
			def active = model.activeDiagram.model
			def interval = active.scene.selection.selectedObjects.find { GeoUtils.isIntervalInstance(it) }
			if (interval) {
				active.commandStack.execute(new SplitIntervalCommand(interval, active.scene.models))
			}
		},
		'createIntervals': { evt = null ->
			def diagram = model.activeDiagram.model
			def modelClasses = diagram.scene.getCreatedClasses()
			withMVC('CreateParallelIntervals', diagram:diagram, modelClasses:modelClasses) { mvc ->
				mvc.controller.show()
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

JRE Version: ${System.getProperty("java.version")} (${System.getProperty("sun.arch.data.model")}-bit)
JRE Vendor: ${System.getProperty("java.vendor")}
JRE Home: ${System.getProperty("java.home")}
Working Dir: ${System.getProperty("user.dir")}
		""".toString(), app.appFrames[0])
		},
		'documentation': { evt = null ->
			LauncherUtils.openURL('http://dev.psicat.org/documentation/')
		},
		'feedback': { evt = null ->
			final feedbackUrl = "https://docs.google.com/forms/d/e/1FAIpQLSdKJB-ayDo4btwBa-By4Cd4cL5_MxcE7vcu90K_CfYx03HwuA/viewform?usp=sf_link"
			LauncherUtils.openURL(feedbackUrl)
		},
		'exportDiagram': { evt = null ->
			withMVC('ExportDiagramWizard', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
		},
		'exportStratColumn': { evt = null ->
			def stratContainers = []
			def pb = ProgressBarFactory.create("Collecting stratigraphic column sections...")
			pb.setVisible(true)
			pb.setLocationRelativeTo(app.appFrames[0])
			doOutside {
				model.project.containers.each { containerName ->
					def c = model.project.openContainer(containerName)
					if (c.countModels("Section") > 1) {
						stratContainers.add(containerName)
					}
					model.project.closeContainer(c)
				}
				pb.setVisible(false)

				if (stratContainers.size() > 0) {
					withMVC('ExportStrat', project:model.project, stratColumnSections:stratContainers) { mvc ->
						mvc.controller.show()
					}
				} else {
					Dialogs.showMessageDialog("No Strat Columns", "The project contains no stratigraphic column sections.\nTo create one, use the File > Create New > Stratigraphic Column... menu item.")
				}
			}
		},
		'diagramOptions': { evt = null ->
			// Force save of all dirty diagrams before opening (avoid data loss on diagram close/reopen)
			def dirtyDiagrams = model.openDiagrams.findAll { it.model.dirty }
			if (dirtyDiagrams.size() > 0) {
				def msg = "The following sections have unsaved data:\n\n${(dirtyDiagrams.collect { it.model.id }).join('\n')}\n\nThey must be saved before editing Diagram Options."
				Dialogs.showMessageDialog("Unsaved Diagrams", msg, app.appFrames[0])
				return
			}
			
			def scene = null
			def activeDiagramId = null
			if (!model.activeDiagram) {	// no active diagram, fake out a scene
				scene = getDiagramScene(model.project)
				scene.models = Platform.getService(ModelContainer.class)
			} else {
				scene = model.activeDiagram.model.scene
				activeDiagramId = model.activeDiagram.model.id
			}

			final diagramTypeText = "<html>Changes will be reflected in the live diagram editing view and exported diagrams.<br>Stratigraphic column export diagrams will not be affected.</html>"
			withMVC('DiagramOptions', scene:scene, diagramTypeText:diagramTypeText) { mvc ->
				if (mvc.controller.show()) {
					if (mvc.model.sceneDirty) {
						if (model.project.scenes) {
							FileWriter writer = new FileWriter(new File(model.project.scenes[0].toURI()))
							SceneUtils.toXML(mvc.model.scene, writer)
						} else {
							println "Project has no diagrams, can't save"
						}
					} else {
						println "No changes to main diagram scene, not saving."
					}
				} else {
					if (mvc.model.sceneDirty) {
						// User cancelled after modifying the scene; revert changes by closing and reopening diagrams
						def diagramIds = model.openDiagrams.collect { it.model.id }
						while (model.activeDiagram != null) { closeDiagram(model.activeDiagram)	}
						diagramIds.each { app.controllers['PSICAT'].actions.openSection(null, it) }
						
						// restore last active diagram
						if (activeDiagramId) {
							for (int i = 0; i < view.diagrams.getTabCount(); i++) {
								if (view.diagrams.getTitleAt(i).equals(activeDiagramId)) {
									view.diagrams.setSelectedIndex(i)
									break
								}
							}
						}
					}
				}
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
			def gsdlg = new GrainSizeDialog(app.appFrames[0], "Grain Size Scale", getGrainSize())
			gsdlg.setVisible(true)
			if (gsdlg.okPressed) {
				def grainSizeCode = gsdlg.getGrainSizeCode()
				try {
					def testScale = new Scale(grainSizeCode)
					model.project.configuration.grainSizeScale = grainSizeCode
					model.project.saveConfiguration()
				} catch (NumberFormatException e) {
					Dialogs.showErrorDialog("Invalid Grain Size Scale", "Invalid grain size scale: ${e.message}", app.appFrames[0])
				}
			}
		},
		'auditProject': { evt = null ->
			if (app.views.AuditProject == null) {
				def pb = ProgressBarFactory.create("Building list of project models...")
				pb.setVisible(true)
				pb.setLocationRelativeTo(app.appFrames[0])

				doOutside {
					HashSet modelTypeSet = new HashSet()
					model.project.containers.each { containerName ->
						def container = model.project.openContainer(containerName)
						container.models.each { model ->
							if (!["Section", "Image"].contains(model.modelType)) {
								modelTypeSet.add(model.modelType)
							}
						}
						model.project.closeContainer(container)
					}
					pb.setVisible(false)
					createMVCGroup('AuditProject', project: model.project, modelTypes: modelTypeSet as List)
				}
			}
		},
		'projectStats': { evt = null ->
			def pb = ProgressBarFactory.create("Gathering project stats...")
			pb.setVisible(true)
			pb.setLocationRelativeTo(app.appFrames[0])

			doOutside {
				def modelCountMap = [:]
				model.project.containers.each { containerName ->
					def container = model.project.openContainer(containerName)
					container.models.each { model ->
						if (modelCountMap.containsKey(model.modelType)) {
							modelCountMap[model.modelType][0] += 1
							if (model.constraints.containsKey('scheme') && model.scheme) {
								modelCountMap[model.modelType][1].add(model.scheme.toString())
							}
						} else {
							modelCountMap[model.modelType] = [1, []] // [model count, list of scheme entries for modelType]
							if (model.constraints.containsKey('scheme') && model.scheme) {
								modelCountMap[model.modelType][1].add(model.scheme.toString())
							}
						}
					}
					model.project.closeContainer(container)
				}
				def uniqueTypesMap = [:]
				modelCountMap.each { type, data ->
					if (data[1].size() > 0) {
						uniqueTypesMap[type] = (data[1] as Set).size()
					}
				}
				def statsString = modelCountMap.collect { type, data -> "${StringUtils.humanizeModelName(type)}: ${data[0]} ${uniqueTypesMap.containsKey(type) ? '(' + uniqueTypesMap[type] + ' unique)' : ''}" }.sort().join('\n')
				pb.setVisible(false)
				Dialogs.showMessageDialog("Project Stats", "$statsString", app.appFrames[0])
			}
		},
		'openStratColumnDepths': { evt = null ->
			withMVC('OpenStratColumnDepths', project: model.project) { mvc ->
				model.status = mvc.controller.show()
			}
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
		},
		'smallFontSize': { evt = null -> setFontSize('9') },
		'mediumFontSize': { evt = null -> setFontSize('11') },
		'largeFontSize': { evt = null -> setFontSize('14') }
	]
}