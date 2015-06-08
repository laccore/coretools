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

import javax.swing.JOptionPane

import org.andrill.coretools.model.edit.CommandStack
import org.andrill.coretools.model.edit.CompositeCommand
import org.andrill.coretools.geology.GCommand

import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeEntry
import org.andrill.coretools.ui.widget.swing.SchemeEntryWidget

import psicat.util.*

class FindReplaceController {
    def model
    def view
	def SchemeEntryWidget findWidget, replaceWidget
	def findUI, replaceUI // transient objects for view creation - probably a better way

    void mvcGroupInit(Map args) {
		findWidget = args.findWidget
		replaceWidget = args.replaceWidget
		findUI = findWidget.getEditableUI()
		replaceUI = replaceWidget.getEditableUI()

		model.project = args.project
		model.commandStack = new CommandStack(true)
    }
	
	def openContainer(containerName) {
		def container = null
		if (containerName in model.containers) {
			container = model.containers[containerName]
		} else {
			container = model.project.openContainer(containerName)
			model.containers[containerName] = container
		}
		return container
	}
	
	void findAndReplace(findEntry, replaceEntry) {
		def commands = []
		model.project.containers.each { containerName ->
			def container = openContainer(containerName)
			def modelIterator = container.iterator()
			while (modelIterator.hasNext()) {
				GeologyModel mod = modelIterator.next()
				def modelType = (findEntry.scheme.type == "lithology" ? "Interval" : "Occurrence")
				def keyName = (findEntry.scheme.type == "lithology" ? "lithology" : "scheme")
				if (mod.modelType.equals(modelType)) {
					def uid = findEntry.scheme.id + ':' + findEntry.code
					if (mod.modelData[keyName].equals(uid)) {
						
						// using GCommand rather than directly modifying model grants undo/redo powers! 
						def replaceId = replaceEntry.scheme.id + ':' + replaceEntry.code
						commands << new GCommand(source: mod, prop: keyName, value: replaceId, old: uid)
						
						model.containersToSave << container
					}
				}
			}
		}

		if (commands.size() > 0) {
			def compCommand = new CompositeCommand("Find and Replace", *commands)
			model.commandStack.execute(compCommand)
			updateUndoButton()
		}
	}
	
	def getFindEntry() { return findWidget.getEntry(findWidget.getWidgetValue()) }
	def getReplaceEntry() { return replaceWidget.getEntry(replaceWidget.getWidgetValue()) }
	def updateUndoButton() { view.undoButton.enabled = model.commandStack.canUndo() }
	
    def actions = [
		'replace': { evt = null ->
			SchemeEntry findEntry = getFindEntry()
			SchemeEntry replaceEntry = getReplaceEntry()
			if (findEntry.scheme.type != replaceEntry.scheme.type) {
				Dialogs.showErrorDialog("Error", "Find and replace items must be the same type.")
				return
			}
			view.replaceButton.enabled = false
			findAndReplace(findEntry, replaceEntry)
			view.replaceButton.enabled = true
		},
		'undo': { evt = null ->
			model.commandStack.undo()
			updateUndoButton()
		}
    ]	

    def show() {
		def saveChanges = JOptionPane.showOptionDialog(app.windowManager.windows[0], view.root, "Find and Replace", JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.PLAIN_MESSAGE, null, ['Save Changes and Close', 'Cancel Changes and Close'].toArray(), null) == JOptionPane.OK_OPTION
		if (saveChanges) {
			model.containersToSave.each { 
				model.project.save(it)
			}
		} else {
			while (model.commandStack.canUndo()) { model.commandStack.undo() }
		}
		model.containers.each { k, v -> model.project.closeContainer(v) }
	}
}