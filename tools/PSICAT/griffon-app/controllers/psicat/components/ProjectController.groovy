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

import java.beans.PropertyChangeListener
import java.beans.PropertyChangeEvent

import psicat.util.ProjectLocal

class ProjectController implements PropertyChangeListener {
    def model
    def view
    
    def clickHandler
    def openHandler

    void mvcGroupInit(Map args) {
    	clickHandler = args.clickHandler
    	openHandler = args.openHandler
    	setProject(args.project)
    }

    void setProject(project) {
    	if (model.project) {
			model.project.removePropertyChangeListener(this)
			ProjectLocal.unloadAllSchemes()
		}
    	model.project = project
    	model.name = project ? project.name : "< No Project >"
    	model.sections.clear()
    	if (project) {
    		model.project.addPropertyChangeListener(this)
    		model.sections.addAll(project.containers)
			loadSchemes()
    	}
    }

	void loadSchemes() {
		def schemeDir = new File(new File(model.project.path.toURI()), "schemes")
		if (schemeDir.exists() && schemeDir.isDirectory()) {
			def schemeFiles = []
			schemeDir.eachFile { 
				if (it.isFile() && it.name.endsWith(".jar"))
					schemeFiles.add(it)
			}
			ProjectLocal.loadSchemes(schemeFiles)
		}
	}
	
    void handleClick(evt = null) {
		if (!model.project) return
    	if (clickHandler) {
    		clickHandler(evt, [model: model, view: view, controller: this])
    	} else if (evt?.clickCount == 2 && openHandler) {
    		handleOpen(evt)
    	}
    }

    void handleOpen(evt = null) {
    	if (openHandler) {
    		openHandler(evt, [model: model, view: view, controller: this])
    	}
    }

    void propertyChange(PropertyChangeEvent event) {
    	model.sections.add(event.newValue)
    }
}