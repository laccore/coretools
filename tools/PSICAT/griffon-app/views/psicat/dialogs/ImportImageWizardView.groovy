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
	action(id: 'browseAction', name:'Select Image Files...', closure: controller.actions.browse)
}

panel(id:'root', layout: new MigLayout('fill'), border: etchedBorder()) {	
	// select image files
	button(text: '...', action: browseAction, constraints: 'growx, wrap')
	label(id: 'fileCountLabel', text: '[no image files selected]', constraints: 'wrap')
	separator(constraints: 'width min(300px), growx, wrap')

	// top
	buttonGroup().with { group ->
		radioButton(selected: bind { !model.parseTop }, text: 'Top:', buttonGroup: group, constraints:'split')
		textField(columns:4, enabled: bind { !model.parseTop }, text: bind(source: model, sourceProperty:'top', mutual:true), 
				inputVerifier: CustomVerifier.NUMBER, constraints: 'wrap')
		radioButton(text: 'Parse top from filename', selected: bind(source: model, sourceProperty:'parseTop', mutual:true), buttonGroup: group, constraints: 'wrap')
	}
	separator(constraints: 'growx, wrap')

	// base
	buttonGroup().with { group ->
		radioButton(selected: bind { !model.parseBase }, text:'DPI:', buttonGroup: group, constraints:'split')
		textField(columns:4, enabled: bind { !model.parseBase }, text: bind(source: model, sourceProperty:'dpi', mutual:true), 
					inputVerifier: CustomVerifier.NUMBER, constraints: 'wrap')
		radioButton(text: 'Parse base from filename', selected: bind(source: model, sourceProperty:'parseBase', mutual:true), buttonGroup: group, constraints: 'wrap')
	}
}

panel(id: 'tablePanel', layout: new MigLayout('fill')) {
	scrollPane(constraints: 'grow, wrap') { table(id: 'table') }
	buttonGroup().with { group ->
		radioButton(text: 'Create a new section for each image', selected: bind { !model.addToSection }, buttonGroup: group, constraints:'growx, wrap')
		radioButton(text: 'Add to existing section:', selected: bind(source:model, sourceProperty:'addToSection', mutual:true), buttonGroup: group, constraints:'split')
		comboBox(id:'section', editable: true, enabled: bind { model.addToSection }, items: model.project.containers, constraints:'growx')
	}
}