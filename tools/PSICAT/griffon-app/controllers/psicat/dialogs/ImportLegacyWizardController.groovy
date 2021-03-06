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

import java.util.Map

import org.andrill.coretools.Platform
import org.andrill.coretools.misc.io.LegacyReader

import psicat.util.Dialogs

class ImportLegacyWizardController {
    def model
    def view

    void mvcGroupInit(Map args) {}

    def actions = [
	    browse: { evt = null ->
	    	def file = Dialogs.showOpenDialog("Image Directory", null, app.appFrames[0])
	    	if (file) { model.filePath = file.absolutePath }
    	}
    ]

    def show() {
    	if (Dialogs.showCustomDialog("Import Legacy", view.root, app.appFrames[0])) {
			return importLegacy(view.section.selectedItem)
    	}
    }
	
	def importLegacy(section) {
		if (model.file) {
			def container = model.project.openContainer(section)
			model.file.withInputStream { stream -> (Platform.getService(LegacyReader.class)).read(container, stream) }
			model.project.saveContainer(container)
			return "Imported legacy data to '$section'"
		} else {
			throw new IllegalStateException('No file specified')
		}
	}
}