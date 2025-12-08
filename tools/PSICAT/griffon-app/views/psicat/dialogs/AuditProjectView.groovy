/*
 * Copyright (c) CSD Facility, 2015.
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

import java.awt.Component
import java.awt.LayoutManager
import javax.swing.*

import groovy.swing.SwingBuilder
import net.miginfocom.swing.MigLayout
import org.andrill.coretools.misc.util.StringUtils

import psicat.ui.DefaultModelListPanel


class AuditElementRenderer implements ListCellRenderer {
	public AuditElementRenderer() { }
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		def auditElt = (AuditResult)value
		return makePanel(auditElt, isSelected)
	}
	
	def makePanel(auditResult, isSelected) {
		def selectColor = javax.swing.UIManager.getDefaults().getColor("List.selectionBackground")
		def bgcolor = isSelected ? selectColor : java.awt.Color.WHITE
		def border = BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.BLACK)
		return new SwingBuilder().panel(layout:new MigLayout("fill, wrap 1, insets 5"), background:bgcolor, border:border) {
			label("${auditResult.section}")
			auditResult.issues.each {
				label("$it", constraints:'gapleft 10, gaptop 0')
			}
		}
	}
}

class AuditModelListPanel extends DefaultModelListPanel {
	static AuditModelListPanel create(List<String> models, boolean check, LayoutManager layout=null) {
		if (!layout) { layout = new MigLayout("fillx, insets 5")}
		AuditModelListPanel panel = new AuditModelListPanel(models.sort(), check, layout)
		return panel
	}

	private AuditModelListPanel(List<String> models, boolean check, LayoutManager layout) {
		super(layout)
		models.each { modelType ->
			def cb = new JCheckBox(StringUtils.humanizeModelName(modelType), check)
			this.add(cb)
			this.modelMap.put(modelType, cb)
		}
	}
}

final openSection = { section ->
	app.controller.actions.openSection(null, section)
}

class AuditLogMouseListener extends java.awt.event.MouseAdapter {
	// todo: better way to deal with app being outside of listener's scope?
	def app = null
	public AuditLogMouseListener(app) { this.app = app }
	
	public void mouseClicked(java.awt.event.MouseEvent event) {
		def list = event.getSource()
		if (event.getClickCount() == 2) {
			def index = list.locationToIndex(event.getPoint())
			def elt = (AuditResult)list.model.elementAt(index)
			app.controllers["PSICAT"].actions.openSection(null, elt.section)
		}
	}
}

actions {
	action(id:'auditAction', name:'Start Audit', closure:controller.actions.audit)
	action(id:'closeAction', name:'Close', closure:controller.actions.close)
	action(id:'exportLogAction', name:'Export Log to File...', closure:controller.actions.exportLog)
}

dialog(id:'auditProjectDialog', title:'Audit Project', owner:app.appFrames[0], pack:true, modal:false,
		resizable:true, windowClosing:controller.actions.close) {
	panel(id:'root', layout: new MigLayout('fill, wrap 1', '', '[]15[][][grow][]')) {
		label("Check Project for the Selected Problems:")
		panel(layout:new MigLayout('fillx, wrap 1, insets 0')) {
			panel(border: titledBorder("Sections without defined"), layout:new MigLayout('fillx, wrap 1, insets 5'), constraints:'grow') {
				widget(AuditModelListPanel.create(model.modelTypes, false), id:'undefinedModels')
				button('Check All', constraints:'align right, split 2', actionPerformed: { evt -> undefinedModels.selectAll(true) })
				button('Check None', constraints:'align right', actionPerformed: { evt -> undefinedModels.selectAll(false) })
			}
			panel(border: titledBorder("Sections with undescribed"), layout:new MigLayout('fillx, wrap 1, insets 5'), constraints:'grow') {
				widget(AuditModelListPanel.create(model.modelTypes, false), id:'undescribedModels')
				button('Check All', constraints:'align right, split 2', actionPerformed: { evt -> undescribedModels.selectAll(true) })
				button('Check None', constraints:'align right', actionPerformed: { evt -> undescribedModels.selectAll(false) })
			}
			panel(border: titledBorder("Inverted or zero-length intervals"), layout:new MigLayout('fillx, wrap 1, insets 5'), constraints:'grow') {
				widget(AuditModelListPanel.create(model.modelTypes, false), id:'bogusIntervals')
				button('Check All', constraints:'align right, split 2', actionPerformed: { evt -> bogusIntervals.selectAll(true) })
				button('Check None', constraints:'align right', actionPerformed: { evt -> bogusIntervals.selectAll(false) })
			}
			panel(border: titledBorder("No selected scheme entry ('None')"), layout:new MigLayout('fillx, wrap 1, insets 5'), constraints:'grow') {
				widget(AuditModelListPanel.create(model.modelTypes - ["UnitInterval"], false), id:'noSchemeEntry')
				button('Check All', constraints:'align right, split 2', actionPerformed: { evt -> noSchemeEntry.selectAll(true) })
				button('Check None', constraints:'align right', actionPerformed: { evt -> noSchemeEntry.selectAll(false) })
			}
			panel(border: titledBorder("Unknown scheme entry"), layout:new MigLayout('fillx, wrap 1, insets 5'), constraints:'grow') {
				widget(AuditModelListPanel.create(model.modelTypes - ["UnitInterval"], false), id:'missingSchemeEntry')
				button('Check All', constraints:'align right, split 2', actionPerformed: { evt -> missingSchemeEntry.selectAll(true) })
				button('Check None', constraints:'align right', actionPerformed: { evt -> missingSchemeEntry.selectAll(false) })
			}		
		}
		
		panel(layout:new MigLayout('insets 5', '[grow][]', ''), constraints:'growx') {
			progressBar(id:'progress', minimum:0, maximum:100, stringPainted:true, string:'', constraints:'growx, gapright 10px')
			button(id:'auditButton', action:auditAction, constraints:'align right, wrap')
			label(" ", id:'progressText')
		}
	
		panel(border: titledBorder('Audit Log - double-click an issue to open section diagram'), layout:new MigLayout('fill, wrap, insets 5'), constraints:'grow') {
			scrollPane(constraints:'grow') {
				auditLog = list(id:'logList', model:bind(source:model, sourceProperty:'auditResults', mutual:true),
					cellRenderer:new AuditElementRenderer(), constraints:'grow', toolTipText:"Double-click an issue to open the associated section's diagram")
				auditLog.addMouseListener(new AuditLogMouseListener(app))
			}
			button(id:'exportLogButton', action:exportLogAction, constraints:'align right, dock south', enabled:false)
		}
		button(action:closeAction, constraints:'align right')
	}
}

auditProjectDialog.setLocationRelativeTo(app.appFrames[0])
auditProjectDialog.show()