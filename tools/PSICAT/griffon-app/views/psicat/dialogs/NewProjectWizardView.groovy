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
}

panel(id:'root', layout: new MigLayout('fill'), border: etchedBorder()) {	
	// directory
	label('Directory:')
	textField(text: bind(source: model, sourceProperty:'filePath', mutual:true), constraints:'width min(200px), growx')
	button(action: browseAction, constraints: 'wrap')

	// project name
	label('Project Name:')
	textField(text: bind(source: model, sourceProperty:'name', mutual:true), constraints: 'growx, span, wrap')
	
	// 6/3/2024 Outcrops are broken and no one has ever asked for them in PSICAT. Disabling.
	// // project type
	// label('Type:')
	// buttonGroup().with {
	// 	add radioButton(text: 'Well', selected: bind(source: model, sourceProperty: 'originTop', mutual:true), constraints:'split')
	// 	add radioButton(text: 'Outcrop', selected: bind { !model.originTop }, constraints: 'wrap')	
	// }

	// units
	label('Units:')
	comboBox(id:'units', items: ['cm', 'm'], selectedItem: bind(source: model, sourceProperty: 'units', mutual:true), constraints: 'wrap')

	// schemes
	label('Schemes:')
	buttonGroup().with {
		add radioButton(text: 'Default', selected: bind { !model.useCustomSchemes }, constraints:'split')
		add radioButton(text: 'Custom', selected: bind(source: model, sourceProperty: 'useCustomSchemes', mutual:true), constraints:'wrap')
	}
	
	// default lithology and symbol scheme combos
	label('Default Lithologies:', enabled:bind { !model.useCustomSchemes }, constraints:'gapleft 15')
	comboBox(id:'lithologyScheme', items: model.lithologySchemes.keySet() as List, constraints:'wrap', enabled:bind { !model.useCustomSchemes })
	// label('Default Symbols:', enabled:bind { !model.useCustomSchemes }, constraints:'gapleft 15')
	// comboBox(id:'symbolScheme', items: model.symbolSchemes.keySet() as List, constraints:'wrap', enabled:bind { !model.useCustomSchemes })
	separator(constraints: 'growx, span, wrap')
	
	// import sections
	checkBox(text: 'Open Import Sections Wizard after project creation', selected: bind(source: model, sourceProperty: 'importSections', mutual:true), constraints: 'span')
}
