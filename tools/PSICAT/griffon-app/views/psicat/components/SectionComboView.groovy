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
package psicat.components

import net.miginfocom.swing.MigLayout

panel(id:'sectionChooser', layout: new MigLayout('fill, insets 5, wrap')) {
	label("Select sections to export.")
	scrollPane(constraints:'grow, wrap, hmin 400') {
		list(id:'sectionList', items:model.project.containers)
	}
}

// build our items list
def items = []
if (model.allSections) { items.add(model.allSectionsText) }
if (model.eachSection) { items.add(model.eachSectionText) }
if (model.selectSections) { items.add(model.selectSectionsText) }
items.addAll(model.project.containers)

// the panel
panel(id: 'root', layout: new MigLayout('fill'), border: emptyBorder(0)) {
	label(id: 'label', text: bind { model.labelText }, constraints: 'split')
	comboBox(id:'section', editable: false, items: items, actionPerformed: { evt -> controller.selectedSectionChanged(evt) }, constraints:'growx')
}