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

import net.miginfocom.swing.MigLayout
import psicat.util.*

actions {
	action(id: 'browseAction', name:'...', closure: controller.actions.browse)
	action(id: 'exportAction', name:'Export', closure: controller.actions.export)
}

// dirty trick here: use eachSection item but label it "All Sections" to indicate everything will end up in the same spreadsheet
def section = buildMVCGroup('SectionCombo', 'exportTabularSections', project: model.project, allSections: false, eachSectionText:"All Sections").view.root

panel(id:'root', layout: new MigLayout('fill'), border: etchedBorder()) {
	widget(id:'section', section, constraints: 'span, growx, wrap')
	separator(constraints: 'span, growx, wrap')

	label('Format:')
	comboBox(id:'format', editable: false, items: ['XLS'], constraints: 'span, wrap')
	
	label('File:')
	textField(text: bind(source: model, sourceProperty:'filePath', mutual:true), constraints:'width min(200px), growx')
	button(action: browseAction, constraints:'wrap')
	
	separator(constraints: 'span, growx, wrap')
	
	panel(layout:new MigLayout('insets 0','[grow][]',''), constraints:'span, growx') {
		progressBar(id:'progress', minimum:0, maximum:100, stringPainted:true, string:'', constraints:'growx, gapright 10px')
		button(id:'exportBtn', action: exportAction)
	}
}
