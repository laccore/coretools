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

import javax.swing.JOptionPane

import org.andrill.coretools.model.edit.CommandStack
import org.andrill.coretools.model.edit.CreateCommand
import org.andrill.coretools.model.edit.CompositeCommand
import org.andrill.coretools.model.edit.DeleteCommand
import org.andrill.coretools.geology.GCommand

import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.geology.models.Occurrence
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeEntry
import org.andrill.coretools.ui.widget.swing.SchemeEntryWidget

import psicat.util.*

class AuditProjectController {
    def model
    def view

    void mvcGroupInit(Map args) {
		model.project = args.project
    }
	
	void log(str) {
		view.logArea.append(str + "\n")
	}
	
	void logIssue(str, lines) {
		lines << "   - $str"
	}
	
	void audit() {
		view.progress.string = "Auditing Project..."
		view.progress.indeterminate = true
		view.logArea.setText('')
		model.project.containers.each { containerName ->
			def container = model.project.openContainer(containerName)
			
			def secTop = null
			def intervals = []
			def symbols = []
			def units = []

			// gather section's models			
			def modelIterator = container.iterator()
			while (modelIterator.hasNext()) {
				GeologyModel mod = modelIterator.next()
				if (mod.modelType.equals("Section")) {
					secTop = mod.top
					GeoUtils.adjustUp(container, secTop, false)
				}
				if (mod.modelType.equals("Interval"))
					intervals << mod
				else if (mod.modelType.equals("Occurrence"))
					symbols << mod
				else if (mod.modelType.equals("Unit"))
					units << mod
			}
			
			// perform selected audits
			def logLines = []
			def undescribed = false
			if (model.undescribedSecs) {
				if (intervals.size() == 0 && symbols.size() == 0 && units.size() == 0) {
					logIssue("Section is undescribed (no intervals, symbols, or units)", logLines)
					undescribed = true
				}
			}
			
			if (model.noIntervalSecs && !undescribed) {
				if (intervals.size() == 0)
					logIssue("Has no lithologic intervals defined", logLines)
			}
			
			if (model.emptyUndescribedInts) {
				intervals.each { it ->
					if (it.lithology == null && it.description == null)
						logIssue("Interval $it is type 'None' with no description", logLines)
				}
			}
			
			if (model.emptyUndescribedSyms) {
				symbols.each { it ->
					if (it.scheme == null && it.description == null)
						logIssue("Symbol $it is type 'None' with no description", logLines)
				}
			}
			
			if (model.zeroLengthInts) {
				intervals.each { it ->
					if (it.top && it.base && it.top.compareTo(it.base) == 0)
						logIssue("Interval $it has zero length", logLines)
				}
			}
			
			if (model.invertedInts) {
				intervals.each { it ->
					if (it.top && it.base && it.top.compareTo(it.base) == 1)
						logIssue("Interval $it is inverted (top depth > base depth)", logLines)
				}
			}
			
			if (logLines.size() > 0) {
				log("$containerName:")
				logLines.each { log(it) }
			}
			
			GeoUtils.adjustDown(container, secTop, false)
			model.project.closeContainer(container)
		}
		
		if (view.logArea.text.isEmpty())
			log("No issues found, hooray!!!")
		
		view.progress.string = "Audit Complete"
		view.progress.indeterminate = false
		view.progress.value = 0
	}
	
    def actions = [
		'audit': { evt = null ->
			doOutside { audit() }
		},
    ]	

    def show() { Dialogs.showCustomOneButtonDialog("Audit Project", view.root, app.appFrames[0]) }
}