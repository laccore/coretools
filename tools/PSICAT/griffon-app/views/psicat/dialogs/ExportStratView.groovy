/*
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

import org.andrill.coretools.graphics.util.Paper

actions {
	action(id: 'browseAction', name:'...', closure: controller.actions.browse)
	action(id: 'exportAction', name:'Export', closure: controller.actions.export)
	action(id: 'diagramOptionsAction', name:'Edit Strat Diagram Columns...', closure: controller.actions.diagramOptions)
}

def section = buildMVCGroup('SectionCombo', 'exportStratSections', project: model.project, allSections:false, eachSection:false, selectSections:false).view.root
final prefixToolTip = "Optional: prefix output files' names with specified text, e.g. 'my_[section_name].pdf'"

panel(id:'root', layout: new MigLayout('fill'), border: etchedBorder()) {
	widget(id:'section', section, constraints: 'span, growx, wrap')
	separator(constraints: 'span, growx, wrap')

	label('Units:')
	comboBox(id:'units', items: ['m', 'cm'], selectedItem: bind(source: model, sourceProperty: 'units', mutual:true), constraints: 'wrap')

	// 8/2/2024 Tabling for now, as PageableScene insists on starting from depth 0. End Depth does (almost) work as expected.
	// label('Start Depth (m):')
	// textField(id:'startDepth', constraints:'wmax 100, growx, wrap')
	// label('End Depth (m):')
	// textField(id:'endDepth', constraints:'wmax 100, growx, wrap')

	label('Page Format:', constraints: 'span 1 2')
	buttonGroup().with {
		add radioButton(text: 'Standard:', selected: bind(source:model, sourceProperty:'standardFormat', mutual:true), constraints: 'split')
		comboBox(id: 'paper', editable: false, items: (Paper.PAGES.values() as List).sort({ p1, p2 -> p1.name <=> p2.name }), constraints: 'wrap')
		add radioButton(text: 'Custom:', selected: bind { !model.standardFormat }, constraints: 'split')
		textField(id: 'paperWidth', enabled: bind { !model.standardFormat }, constraints: 'grow')
		label('x', enabled: bind { !model.standardFormat })
		textField(id: 'paperHeight', enabled: bind { !model.standardFormat }, constraints: 'grow')
		label('pixels', enabled: bind { !model.standardFormat }, constraints: 'wrap')
	}

	separator(constraints: 'span, growx, wrap')

	label('Title:')
	textField(id:'title', constraints:'growx, wrap')

	label('Included Columns:')
	panel(constraints:'wrap') {
		label(id: 'columnsLabel', constraints:'grow', text: bind(source:model, sourceProperty:'diagramColumns'))
		button(id:'diagramOptionsButton', action:diagramOptionsAction, constraints:'wrap')
	}

	separator(constraints: 'span, growx, wrap')

	checkBox(text: 'Draw Header', selected: bind(source: model, sourceProperty:'renderHeader', mutual:true))
	checkBox(text: 'Draw Footer', selected: bind(source: model, sourceProperty:'renderFooter', mutual:true), constraints: 'wrap')
	checkBox(text: 'Draw Column Borders', selected: bind(source: model, sourceProperty:'renderColumnBorders', mutual:true))
	checkBox(text: 'Draw Interval Outlines', selected: bind(source: model, sourceProperty:'renderIntervalOutlines', mutual:true), constraints: 'wrap')
	separator(constraints: 'span, growx, wrap')
	
	label('Output Directory:')
	textField(enabled:false, text: bind { model.filePath }, constraints:'width min(200px), growx')
	button(action: browseAction, constraints:'wrap')
	
	label(text:'Filename Prefix:', toolTipText:prefixToolTip)
	textField(text: bind(source:model, sourceProperty:'prefix', mutual:true), toolTipText:prefixToolTip, constraints:'growx, wrap')
	
	// label('Export Format:')
	// comboBox(id:'format', editable: false, items: ['PDF', 'PNG', 'JPEG', 'BMP', 'SVG'], constraints: 'span, wrap')

	// separator(constraints: 'span, growx, wrap')
	
	panel(layout:new MigLayout('','[grow][]',''), constraints:'span, growx') {
		progressBar(id:'progress', minimum:0, maximum:100, stringPainted:true, string:'', constraints:'growx, gapright 10px')
		button(id:'exportBtn', action: exportAction)
	}
}