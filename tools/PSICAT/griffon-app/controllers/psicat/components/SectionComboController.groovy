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

class SectionComboController {
    def model
    def view

    void mvcGroupInit(Map args) {}

    /**
     * Gets the name of the selection.
     */
    String getSelection() { view.section.selectedItem }

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
        if (view.section.selectedIndex == 0) {
        	def container = Platform.getService(ModelContainer.class)
        	project.containers.each { c ->
        		project.openContainer(c).models.each { container.add(it) }
        	}
        	containers[project.name] = container
        } else if (view.section.selectedIndex == 1) {
        	project.containers.each { containers[it] = project.openContainer(it) }
        } else {
        	def section = view.section.selectedItem
        	containers[section] = project.openContainer(section)
        }
		return containers
    }
}