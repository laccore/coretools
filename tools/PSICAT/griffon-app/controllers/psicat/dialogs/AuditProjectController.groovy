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

import java.util.HashSet
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
import org.andrill.coretools.model.scheme.SchemeManager
import org.andrill.coretools.ui.widget.swing.SchemeEntryWidget

import org.andrill.coretools.Platform
import org.andrill.coretools.misc.util.StringUtils

import psicat.util.*

class AuditProjectController {
    def model
    def view

    void mvcGroupInit(Map args) {
		model.project = args.project
		model.modelTypes = args.modelTypes.sort()
    }
	
	void exportLog() {
		def file = Dialogs.showSaveDialog("Save Audit Log", null, ".txt", view.auditProjectDialog)
		if (file) {
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
	}

	List<String> undefinedModelsAudit(def container, def modelTypes) {
		List<String> issues = []
		def definedModelTypes = []
		container.models.each {
			if (it.modelType in modelTypes && !(it.modelType in definedModelTypes)) {
				definedModelTypes.add(it.modelType)
			}
		}
		def undefinedModelTypes = modelTypes - definedModelTypes
		if (undefinedModelTypes.size() > 0) {
			issues << "No defined ${undefinedModelTypes.collect { type -> StringUtils.humanizeModelName(type) }.join(', ')}"
		}

		return issues
	}

	List<String> undescribedModelsAudit(def container, def modelTypes) {
		List<String> issues = []
		def undescribedCounts = zeroInitMap(modelTypes)
		container.models.each {
			if (it.modelType in modelTypes && it.description == null) {
				undescribedCounts[it.modelType] += 1
			}
		}
		def undescStrings = undescribedCounts.findAll { type, count -> count > 0 }.collect { type, count -> "$count undescribed ${StringUtils.humanizeModelName(type)}" }

		if (undescStrings.size() > 0) {
			issues << "${undescStrings.join(', ')}"
		}

		return issues
	}

	List<String> bogusIntervalsAudit(def container, def modelTypes) {
		List<String> issues = []
		def invertedCounts = zeroInitMap(modelTypes)
		def zeroLengthCounts = zeroInitMap(modelTypes)
		container.models.each {
			if (it.modelType in modelTypes) {
				if (it.top.compareTo(it.base) == 1) { invertedCounts[it.modelType] += 1	}
				if (it.top.equals(it.base)) { zeroLengthCounts[it.modelType] += 1 }
			}
		}
		def invertedStrings = invertedCounts.findAll { type, count -> count > 0 }.collect { type, count -> "$count inverted ${StringUtils.humanizeModelName(type)}" }
		def zeroLengthStrings = zeroLengthCounts.findAll { type, count -> count > 0 }.collect { type, count -> "$count zero-length ${StringUtils.humanizeModelName(type)}" }
		if (invertedStrings.size() > 0) {
			issues << "${invertedStrings.join(', ')}"
		}
		if (zeroLengthStrings.size() > 0) {
			issues << "${zeroLengthStrings.join(', ')}"
		}
		return issues
	}

	List<String> noSelectedSchemeEntryAudit(def container, def modelTypes) {
		List<String> issues = []
		def noEntryCounts = zeroInitMap(modelTypes)
		container.models.each {
			if (it.modelType in modelTypes) {
				if (it.hasProperty("lithology") && it.lithology == null) { noEntryCounts[it.modelType] += 1	}
				if (it.hasProperty("scheme") && it.scheme == null) { noEntryCounts[it.modelType] += 1 }
			}
		}
		def noEntryStrings = noEntryCounts.findAll { type, count -> count > 0 }.collect { type, count -> "$count ${StringUtils.humanizeModelName(type)} with 'None' scheme" }
		if (noEntryStrings.size() > 0) {
			issues << "${noEntryStrings.join(', ')}"
		}
		return issues
	}

	List<String> missingSchemeEntryAudit(def container, def modelTypes) {
		List<String> issues = []
		def missingSchemeCounts = zeroInitMap(modelTypes)
		container.models.each {
			if (it.modelType in modelTypes) {
				def schemeProp = null
				if (it.hasProperty("lithology") && it.lithology != null) { 
					schemeProp = it.lithology
				} else if (it.hasProperty("scheme") && it.scheme != null) {
					schemeProp = it.scheme
				}
				if (schemeProp && !validSchemeEntry(schemeProp.scheme, schemeProp.code)) {
					def msg = "${StringUtils.humanizeModelName(it.modelType)} scheme entry ${schemeProp.scheme}:${schemeProp.code} not found"
					issues << msg
				}
			}
		}
		return issues
	}
	
	void audit() {
		view.auditButton.enabled = false
		view.progress.indeterminate = true
		
		edt { model.auditResults.clear() }

		def auditResults = []
		model.project.containers.each { containerName ->
			edt { view.progressText.text = "Auditing $containerName, ${auditResults.size} issues found so far..." }
			
			def container = model.project.openContainer(containerName)
			List<String> issues = []
			
			// Perform selected audits. 'this.&' syntax is required to pass methods around.
			[
				[this.&undefinedModelsAudit, view.undefinedModels],
				[this.&undescribedModelsAudit, view.undescribedModels],
				[this.&bogusIntervalsAudit, view.bogusIntervals],
				[this.&noSelectedSchemeEntryAudit, view.noSchemeEntry],
				[this.&missingSchemeEntryAudit, view.missingSchemeEntry]
			].each { auditMethod, modelTypePanel ->
				def selectedModels = modelTypePanel.getSelectedModels()
				if (selectedModels.size() > 0) {
					issues += auditMethod(container, selectedModels)
				}
			}

			if (issues.size() > 0) { auditResults << new AuditResult(containerName, issues)	}
			
			model.project.closeContainer(container)
		}
		
		edt {
			auditResults.each {	model.auditResults.addElement(it) }
		}
		
		view.exportLogButton.enabled = model.auditResults.size > 0
		view.auditButton.enabled = true
		view.progress.indeterminate = false
		view.progress.value = 0
		view.progressText.text = "Audit complete, ${model.auditResults.size} issues found."
	}
	
	private boolean validSchemeEntry(String schemeId, String code) {
		def valid = false
		def scheme = Platform.getService(SchemeManager.class)?.getScheme(schemeId)
		if (scheme) {
			def entry = scheme.getEntry(code)
			if (entry) valid = true
		}
		return valid
	}

	// If we weren't running against 2010-era Groovy this would not be necessary [crying face]
	private zeroInitMap(def keys) {
		def map = [:]
		keys.each { key -> map[key] = 0 }
		return map
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