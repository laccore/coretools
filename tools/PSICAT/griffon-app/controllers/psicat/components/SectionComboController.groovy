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

import org.andrill.coretools.Platform
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.model.DefaultModelManager

class SectionComboController {
    def model
    def view

    void mvcGroupInit(Map args) {}

    /**
     * Gets the name of the selection.
     */
    String getSelection() { view.section.selectedItem }

	/**
	 * Creates a new container with copies of all input containers' models
	 * (model data only). The copies can be freely manipulated without fear of
	 * corrupting the "true" set of models maintained in the project or
	 * disrupting listeners dependent on model/container association.
	 */
	private ModelContainer copy(List containerNames) {
		def modelManager = Platform.getService(DefaultModelManager.class)
		def container = Platform.getService(ModelContainer.class)
		containerNames.each { name ->
			model.project.openContainer(name).models.each { m ->
				container.add(modelManager.build(m.modelType, m.modelData))
			}
		}
		container.project = model.project // need project for e.g. grain size
		return container
	}
	
	/**
	 * Convenience copy method accepting a single container name.
	 */
	private ModelContainer copy(String containerName) {
		copy([containerName])
	}
	
	/**
	 * Gets copies of containers and data-only copies of models for the selection.
	 * This may be:
	 *  - a container with all sections
	 *  - a container for each section
	 *	- a container for a specific section
	 */
	def copyContainers() {
		def project = model.project
		def containers = [:]
		if (model.allSections && view.section.selectedItem == model.allSectionsText) {
			containers[project.name] = copy(project.containers)
		} else if (model.eachSection && view.section.selectedItem == model.eachSectionText) {
			project.containers.each { c ->
				containers[c] = copy(c)
			}
		} else {
			def section = view.section.selectedItem
			containers[section] = copy(section)
		}
		return containers
	}
	
    /**
     * Gets the containers for the selection.  
     * This may be: 
     *  - a container with all sections
     *  - a container for each section
	 *	- a container for a specific section
     */
    def getContainers() {
    	def project = model.project
		def containers = [:]
        if (model.allSections && view.section.selectedItem == model.allSectionsText) {
			def container = Platform.getService(ModelContainer.class)
        	project.containers.each { c ->
        		project.openContainer(c).models.each { container.add(it) }
        	}
        	containers[project.name] = container
        } else if (model.eachSection && view.section.selectedItem == model.eachSectionText) {
			project.containers.each { containers[it] = project.openContainer(it) }
        } else {
			def section = view.section.selectedItem
			containers[section] = project.openContainer(section)
        }
		return containers
    }
}