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

import static javax.swing.SwingConstants.*

import java.awt.Color
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.swing.DefaultEventListModel
import ca.odell.glazedlists.swing.TextComponentMatcherEditor
import ca.odell.glazedlists.TextFilterator
import ca.odell.glazedlists.FilterList

import net.miginfocom.swing.MigLayout

import psicat.ui.FilterTextField

class SectionNameFilterator implements TextFilterator {
	public void getFilterStrings(List baseList, Object element) {
		baseList.add((String)element)
	}
}

def filterText = new FilterTextField(imageIcon('/magnifying-glass.png'), imageIcon('/reset-x.png'))
def filteredSectionList = new FilterList(model.sections, new TextComponentMatcherEditor(filterText, new SectionNameFilterator()))

actions {
	action(id: 'openAction', name: 'Open', closure: controller.&handleOpen)
}

panel(id:'root', layout: new MigLayout("fill, wrap, insets 0", "", "[][][grow]")) {
	label(id:'name', horizontalAlignment: CENTER, text: bind { model.name },
		border: compoundBorder(matteBorder(0, 0, 1, 0, color: Color.lightGray), emptyBorder(5)), constraints:'grow')
	widget(id:'filterText', filterText, enabled:bind { model.project != null }, mouseClicked: { evt -> filterText.mouseClicked(evt) }, constraints:'growx')
	scrollPane(constraints: 'grow') {
		list(id:'sections', model: new DefaultEventListModel(filteredSectionList), mouseClicked: { evt -> controller.handleClick(evt) })
	}
}

//handle enter on the section list
sections.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), 'openAction')
sections.actionMap.put('openAction', openAction)