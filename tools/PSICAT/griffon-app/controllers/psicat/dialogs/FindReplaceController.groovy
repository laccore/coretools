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

import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeEntry
import org.andrill.coretools.ui.widget.swing.SchemeEntryWidget

import psicat.util.*

class FindReplaceController {
    def model
    def view
    def project
	def SchemeEntryWidget findWidget, replaceWidget
	def findUI, replaceUI // transient objects for view creation - probably a better way

    void mvcGroupInit(Map args) {
    	project = args.project
		findWidget = args.findWidget
		replaceWidget = args.replaceWidget
		findUI = findWidget.getEditableUI()
		replaceUI = replaceWidget.getEditableUI()
    }

	void findAndReplace(findEntry, replaceEntry) {
		if (findEntry.scheme.type != replaceEntry.scheme.type) {
			Dialogs.showErrorDialog('Error', "Selected Find and Replace scheme types must match.")
			return
		}
		
		project.containers.each { containerName ->
			//print "searching container $containerName "
			def container = project.openContainer(containerName)
			def needSave = false
			
			def modelIterator = container.iterator()
			while (modelIterator.hasNext()) {
				GeologyModel mod = modelIterator.next()
				def modelType = (findEntry.scheme.type == "lithology" ? "Interval" : "Occurrence")
				def keyName = (findEntry.scheme.type == "lithology" ? "lithology" : "scheme")
				//print "found model ${mod.modelType}...scheme type = $modelType "
				if (mod.modelType.equals(modelType)) {
					def uid = findEntry.scheme.id + ':' + findEntry.code
					//print "found, lithology = ${mod.modelData.lithology}, uid = $uid..."
					if (mod.modelData[keyName].equals(uid)) {
						//println "match, updating!"
						mod.setProperty(keyName, replaceEntry.scheme.id + ':' + replaceEntry.code)
						needSave = true
						container.update(mod)
						//println "updated model props: ${mod.modelData}"
					}
				}
			}
			
			if (needSave) project.save(container)
			project.closeContainer(container)
		}
	}
	
    def actions = [
		'replace': { evt = null ->
			def proceed = Dialogs.showCustomDialog("Confirm Find and Replace",
				"This operation will modify all matching items in the project and cannot be undone. Are you sure you want to proceed?");
			if (proceed) {
				final String findCode = findWidget.getWidgetValue()
				final String replaceCode = replaceWidget.getWidgetValue()
				SchemeEntry findEntry = findWidget.getEntry(findCode)
				SchemeEntry replaceEntry = replaceWidget.getEntry(replaceCode)
				findAndReplace(findEntry, replaceEntry)
			}
		}
    ]	

    def show() {
    	if (Dialogs.showCustomDialog("Find and Replace", view.root, app.appFrames[0])) {
			println "okayed out of replace land!"
    	}
    }
}