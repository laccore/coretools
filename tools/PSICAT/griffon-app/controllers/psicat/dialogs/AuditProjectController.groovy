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
	
	void exportLog() {
		def file = Dialogs.showSaveDialog("Save Audit Log", null, ".txt", view.auditProjectDialog)
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			model.auditResults.elements().each {
				out.write("$it")
				out.newLine()
			}
			out.close()
		} catch (Exception e) {
			Dialogs.showErrorDialog("Save Log Failed", "Log could not be saved: ${e.message}", view.auditProjectDialog)
		}
	}
	
	void audit() {
		view.auditButton.enabled = false
		view.progress.indeterminate = true
		
		edt { model.auditResults.clear() }
		
		model.project.containers.each { containerName ->
			edt { view.progressText.text = "Auditing $containerName..." }
			
			def container = model.project.openContainer(containerName)
			def auditResults = []
			def issues = []
			
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
			def undescribed = false
			if (model.undescribedSecs) {
				if (intervals.size() == 0 && symbols.size() == 0 && units.size() == 0) {
					def msg = "Section is undescribed (no intervals, symbols, or units)" 
					issues << msg
					undescribed = true
				}
			}
			
			if (model.noIntervalSecs && !undescribed) {
				if (intervals.size() == 0) {
					def msg = "Has no lithologic intervals defined"
					issues << msg
				}
			}
			
			if (model.emptyUndescribedInts) {
				intervals.each { it ->
					if (it.lithology == null && it.description == null) {
						def msg = "Interval $it is type 'None' with no description"
						issues << msg
					}
				}
			}
			
			if (model.emptyUndescribedSyms) {
				symbols.each { it ->
					if (it.scheme == null && it.description == null) {
						def msg = "Symbol $it is type 'None' with no description"
						issues << msg
					}
				}
			}
			
			if (model.zeroLengthInts) {
				intervals.each { it ->
					if (it.top && it.base && it.top.compareTo(it.base) == 0) {
						def msg = "Interval $it has zero length" 
						issues << msg
					}
				}
			}
			
			if (model.invertedInts) {
				intervals.each { it ->
					if (it.top && it.base && it.top.compareTo(it.base) == 1) {
						def msg = "Interval $it is inverted (top depth > base depth)" 
						issues << msg
					}
				}
			}
			
			if (issues.size() > 0) { auditResults << new AuditResult(containerName, issues)	}
			
			GeoUtils.adjustDown(container, secTop, false)
			model.project.closeContainer(container)
			
			edt {
				auditResults.each {	model.auditResults.addElement(it) }
			}
		}
		
		view.exportLogButton.enabled = model.auditResults.size > 0
		view.auditButton.enabled = true
		view.progress.indeterminate = false
		view.progress.value = 0
		view.progressText.text = "Audit complete, ${model.auditResults.size} issues found"
	}
	
    def actions = [
		'audit': { evt = null ->
			doOutside { audit() }
		},
		'close': { evt = null ->
			destroyMVCGroup('AuditProject')
		},
		'exportLog': { evt = null ->
			exportLog()
		}
    ]
}