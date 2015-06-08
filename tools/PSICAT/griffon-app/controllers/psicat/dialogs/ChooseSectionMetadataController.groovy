/*
 * Copyright (c) Brian Grivna, 2015.
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

import psicat.util.CustomFileFilter
import psicat.util.Dialogs
 
class ChooseSectionMetadataController {
	def model
	def view
	
	void mvcGroupInit(Map args) {
		model.project = args.project
		model.metadataFile = args.metadataFile
	}
	
	def getFile() { model.metadataFile }
	
	def show() { Dialogs.showCustomDialog("Choose Section Metadata File", view.root, app.windowManager.windows[0]) }
	
	def actions = [
		'browse': { evt = null ->
			def csvFilter = new CustomFileFilter(description: "CSV Files (*.csv)", extensions: [".csv"])
			def file = Dialogs.showOpenDialog("Select Section Metadata File", csvFilter, app.windowManager.windows[0])
			if (file) { model.metadataFile = file }
		}
	]
}