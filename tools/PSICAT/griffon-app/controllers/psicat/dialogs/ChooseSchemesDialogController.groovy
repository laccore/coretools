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

import org.andrill.coretools.Platform
import org.andrill.coretools.model.DefaultProject
import org.andrill.coretools.model.Project
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeManager

import psicat.util.*

class ChooseSchemesDialogController {
    def model
    def view

    void mvcGroupInit(Map args) {
		model.project = args.project
		def sm = Platform.getService(SchemeManager.class)
		model.schemes.addAll(sm.schemes)
	}

    def actions = [
	    'add': { evt = null ->
	    	def schemeFiles = Dialogs.showOpenMultipleDialog("Select Scheme", new CustomFileFilter(extensions: ['.jar'],
				description: 'Scheme Files (*.jar)'), app.appFrames[0])
			schemeFiles?.each {
				def newScheme = ProjectLocal.addScheme(it)
				if (newScheme) {
					model.addedSchemes.add(newScheme)
					model.schemes.add(newScheme)
				}
			}
    	},
		// todo: warn user if project contains entries that are part of the scheme to be deleted
		'delete': { evt = null ->
			def idx = view.schemeList.selectedIndex
			if (idx != -1) {
				def scheme = view.schemeList.selectedValue
				if (scheme in model.addedSchemes) {
					// deleted scheme is newly-added and thus not project-local yet, remove from list only
					model.addedSchemes.remove(scheme)
					model.schemes.remove(idx)
				} else {
					// scheme is project-local, mark for deletion on exit-confirm
					model.deletedSchemes.add(model.schemes.remove(idx))
				}
			}		
		}
    ]

    def show() {
    	if (Dialogs.showCustomDialog("Edit Project Schemes", view.root, app.appFrames[0])) {
			// copy added schemes to project
			model.addedSchemes.each {
				ProjectLocal.copySchemeToProject(it.getInput(), model.project.path)
			}
			
			// remove deleted schemes
			model.deletedSchemes.each { ProjectLocal.unloadScheme(it) }
			
			ProjectLocal.unloadAllSchemes()
			return true
		}
		return false
    }
}