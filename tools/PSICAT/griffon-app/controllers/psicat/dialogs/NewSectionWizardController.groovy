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

import org.andrill.coretools.geology.models.Image
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.models.Section

import psicat.util.*

class NewSectionWizardController {
    def model
    def view
    def project

    void mvcGroupInit(Map args) {
    	project = args.project
    }

    def actions = [
        'browse': { evt = null ->
        	def file = Dialogs.showOpenDialog('Select Image File', CustomFileFilter.IMAGES, app.appFrames[0])
        	if (file) { 
        		model.filePath = file.absolutePath
        		if (!model.name) {
					model.name = FileUtils.removeExtension(file)
				}
        	}
    	}
    ]

    def show() {
    	if (Dialogs.showCustomDialog("Create New Section", view.root, app.appFrames[0])) {
    		return createSection()
    	}
    }
	
	private def createSection() {
		if (model.name && model.top && model.base) {
			def container = project.createContainer(model.name)

			// create a section
			def top = model.top ? model.top as BigDecimal : model.base as BigDecimal
			def base = model.base ? model.base as BigDecimal : model.top as BigDecimal
			container.add(new Section(top: new Length(top, "m"), base: new Length(base, "m"), name: model.name))

			if (model.file) { // add image to section
				def url = ProjectLocal.copyImageFile(model.file, project.path).toURI().toURL()
				container.add(new Image(top: new Length(top, "m"), base: new Length(base, "m"), path: url))
			}

			project.saveContainer(container)
			return model.name	
		} else {
			throw new IllegalStateException('Section name, top and base depths are required to create a new section')
		}
	}
}