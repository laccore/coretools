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

def section = buildMVCGroup('SectionCombo', 'exportDiagramSections', project: model.project, allSections:false).view.root

panel(id:'root', layout: new MigLayout('fill'), border: etchedBorder()) {
	widget(id:'section', section, constraints: 'span, growx, wrap')
	separator(constraints: 'span, growx, wrap')
	
	label('Range:', constraints: 'span 1 2')
	buttonGroup().with {
		add radioButton(text: 'all', selected: bind(source: model, sourceProperty:'exportAll', mutual:true), constraints: 'span, wrap')
		add radioButton(selected: bind { !model.exportAll }, constraints:'split')	
	}
	textField(columns:4, enabled: bind { !model.exportAll }, text: bind(source: model, sourceProperty:'start', mutual:true), inputVerifier: CustomVerifier.NUMBER)
	label("-")
	textField(columns:4, enabled: bind { !model.exportAll }, text: bind(source: model, sourceProperty:'end', mutual:true), inputVerifier: CustomVerifier.NUMBER)
	label("${model.units}", constraints: 'wrap')
	label('Per page:')
	textField(columns:4, text: bind(source: model, sourceProperty:'pageSize', mutual:true), inputVerifier: CustomVerifier.NUMBER, constraints: 'split')
	label("${model.units}", constraints:'wrap')
	separator(constraints: 'span, growx, wrap')

	checkBox(text: 'Render Header', selected: bind(source: model, sourceProperty:'renderHeader', mutual:true), constraints: 'span, wrap')
	checkBox(text: 'Render Footer', selected: bind(source: model, sourceProperty:'renderFooter', mutual:true), constraints: 'span, wrap')
	separator(constraints: 'span, growx, wrap')

	label('Format:')
	comboBox(id:'format', editable: false, items: ['PDF', 'PNG', 'JPEG', 'BMP', 'SVG'], constraints: 'span, wrap')
	
	label('File:')
	textField(text: bind(source: model, sourceProperty:'filePath', mutual:true), constraints:'width min(200px), growx')
	button(action: browseAction)
}