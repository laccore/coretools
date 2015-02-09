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
package psicat.components

import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.prefs.Preferences

import javax.swing.JOptionPane

import org.andrill.coretools.Platform
import org.andrill.coretools.ResourceLoader;
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.model.edit.CommandStack
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.scene.Scene
import org.andrill.coretools.scene.Selection
import org.andrill.coretools.ui.ScenePanel.Orientation
import org.andrill.coretools.misc.util.SceneUtils

import psicat.PSICATController
import psicat.util.*

class DiagramController implements ModelContainer.Listener, Scene.SelectionListener, PropertyChangeListener, ComponentListener {
    def model
    def view
    private def prefs = Preferences.userNodeForPackage(PSICATController)
    private def tabs
	private boolean singleSection = false
	private def sectionTop
    
    void mvcGroupInit(Map args) {
    	// save our tabs
    	tabs = args.tabs
    	
    	// set our name
    	def sections = args.id.split('\\|') as List
    	switch (sections.size()) {
    		case 1: model.name = sections[0]; break
    		case model.project.containers.size(): model.name = model.project.name; break
    		default: model.name = "${model.project.name} [${sections.join(', ')}]"
    	}

    	// create our edit support object
    	model.commandStack = new CommandStack(sections.size() == 1)
    	model.commandStack.addPropertyChangeListener(this)
		
		model.units = model.project.configuration.units ?: prefs.get('diagram.units', 'm')
    }

    void activate(diagramState) {
    	model.diagramState = diagramState
    	setState('name', model.name)
    	setState('dirty', model.dirty)
    	setState('commandStack', model.commandStack)
    	setState('canUndo', model.commandStack.canUndo())
    	setState('canRedo', model.commandStack.canRedo())
    	setState('selection', model?.scene?.selection?.firstObject)
    	setState('vertical', model.vertical)
    	setState('units', model.units)
    }

    void deactivate() {
    	model.diagramState = null
    }

    boolean open() {
    	def project = model.project

    	// select a scene
		def scene
		if (project.scenes) {
			scene = SceneUtils.fromXML(project.scenes[0])
		}
		if (!scene) {
			scene = SceneUtils.fromXML(Platform.getService(ResourceLoader.class).getResource("rsrc:/templates/template.diagram"))
		}
	    			
    	// open our containers
		def models
    	def containers = model.id.split('\\|') as List
		if (containers.size() == 1) {
			models = project.openContainer(containers[0])
		} else {
			models = Platform.getService(ModelContainer.class)
			containers.each { section ->
				def container = project.openContainer(section)
				container.models.each { models.add(it) }
			}
		}
		
		// figure out the number of sections
		def sections = models.findAll { it.modelType == 'Section' }
		singleSection = sections.size() == 1 && sections[0]?.top && sections[0]?.base
		if (singleSection) {
			sectionTop = sections[0].top
			GeoUtils.adjustUp(models, sectionTop)
		}

    	// set our properties and listeners
		scene.models = models
    	scene.origin = (project.configuration.origin ?: 'top' == 'top') ? Scene.Origin.TOP : Scene.Origin.BASE
    	scene.commandStack = model.commandStack
    	scene.addSelectionListener(this)
    	scene.models.addListener(this)
		view.contents.addComponentListener(this)
    			
    	// setup the viewer
    	model.scene = scene
    	view.header.scene = model.scene
		view.contents.scene = model.scene
		if (model.commandStack.canExecute() && !singleSection) { view.contents.padding = 500 }

    	// set the orientation
    	setOrientation(prefs.get('diagram.orientation', 'vertical') == 'vertical')
    	setUnits(model.units)
    	
    	return true
    }

    boolean close() {
    	if (model.dirty) {
    		switch (JOptionPane.showConfirmDialog( app.appFrames[0], "Save changes to '${model.name}'?", "PSICAT", JOptionPane.YES_NO_CANCEL_OPTION)){
    			case JOptionPane.YES_OPTION: return save()
    			case JOptionPane.NO_OPTION: return true
    			default: return false
    		}
    	}
    	return true
    }

    boolean save() {
    	if (model.dirty) {
			if (singleSection && sectionTop) { GeoUtils.adjustDown(model.scene.models, sectionTop) }
    		model.project.saveContainer(model.scene.models)
    		if (singleSection && sectionTop) { GeoUtils.adjustUp(model.scene.models, sectionTop) }
    		markClean()
    	}
    	return true
    }

    void setOrientation(vertical) {
    	// set the orientation
    	model.vertical = vertical
    	def orientation = vertical ? Orientation.VERTICAL : Orientation.HORIZONTAL
    	view.header.orientation = orientation
    	view.contents.orientation = orientation

    	// sets the viewer
    	if (vertical) {
    		view.viewer.columnHeaderView = view.header
			view.viewer.rowHeaderView = null
    	} else {
    		view.viewer.columnHeaderView = null
			view.viewer.rowHeaderView = view.header
    	}
    }

    void setZoom(pageSize) {
		model.scene.scalingFactor = view.viewer.viewport.size.height / pageSize
		doLater {
			view.header.sceneChanged()
			view.contents.sceneChanged()
		}
	}

    void setUnits(units) {
		def factor = new Length(1, model.units).to(units).value
		if (model.scene) {
			model.scene.setRenderHint('preferred-units', units)
			model.scene.scalingFactor = model.scene.scalingFactor / factor
			doLater {
				view.header.sceneChanged()
				view.contents.sceneChanged()
			}
		}
		model.units = units
		setState('units', units)
	}

    // manage state 
    private void setState(name, value) {
    	if (model.diagramState && model.diagramState.hasProperty(name)) {
    		model.diagramState."$name" = value
    	}
    }
    private void markDirty() {
    	model.dirty = true
    	setState('dirty', true)
    	setTitle(model.name + "*")
    }
    private void markClean() {
    	model.dirty = false
    	setState('dirty', false)
    	setTitle(model.name)
    }
    private void setTitle(title) {
        int index = tabs.indexOfComponent(view.viewer)
        if (index != -1) {
            tabs.setTitleAt(index, title)
        }
    }
	
    // ModelContainer.Listener
	void modelAdded(Model m)	{ markDirty() }
	void modelRemoved(Model m)	{ markDirty() }
	void modelUpdated(Model m)	{ markDirty() }

	// Scene.SelectionListener
	void selectionChanged(Selection selection) { setState('selection', selection?.firstObject)  }

	// PropertyChangeListener
	void propertyChange(PropertyChangeEvent evt) {
		switch(evt.propertyName) {
			case "undo": setState('canUndo', evt.newValue); break
			case "redo": setState('canRedo', evt.newValue); break
		}
	}
	
	// ComponentListener - only interested in resize
	void componentResized(ComponentEvent e) {
		model.scene.preferredWidth = e.component.width
	}
	void componentHidden(ComponentEvent e) { }
	void componentMoved(ComponentEvent e) { }
	void componentShown(ComponentEvent e) { }
}