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

import org.andrill.coretools.model.DefaultProject
import org.andrill.coretools.model.Project

import psicat.util.*

class NewProjectWizardController {
    def model
    def view

    void mvcGroupInit(Map args) {}

    def actions = [
	    'browse': { evt = null ->
	    	def file = Dialogs.showSaveDirectoryDialog("Select Root Project Directory", null, app.windowManager.windows[0])
	    	if (file) { 
	    		model.filePath = file.absolutePath
	    		if (!model.name) { model.name = file.name }
	    	}
    	},
		'create': {
			createProject()
			actions.close()
		},
		'close': { evt = null ->
			destroyMVCGroup('NewProjectWizard')
		}
    ]

	private def createProject() {
		if (model.file) {
			Project project = new DefaultProject(model.file)
			project.name = model.name ?: model.file.name
			project.origin = model.originTop ? 'top' : 'base'
			project.saveConfiguration()
			
			if (app.controllers.PSICAT.canClose(null)) {
				app.controllers.PSICAT.openProject(project)
				if (model.useCustomSchemes) {
					app.controllers.PSICAT.actions.chooseSchemes()
				} else {
					println "copying defaults"
					doLater {
						ProjectLocal.copyDefaultSchemes(project)
					}
				}
				if (model.importSections) {
					app.controllers.PSICAT.actions.importImage()
				}
			}
		} else {
			throw new IllegalStateException('A directory is required to create a new project')
		}
	}
}