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

import ca.odell.glazedlists.swing.EventListModel

actions {
	action(id: 'openAction', name: 'Open', closure: controller.&handleOpen)
}

scrollPane(id:'root', columnHeaderView: label(id:'name', horizontalAlignment: CENTER, text: bind { model.name }, 
		mouseClicked: { evt = null ->
			switch (evt.clickCount) {
				case 1: sections.setSelectionInterval(0, model.sections.size() - 1); break
				case 2: controller.handleClick(evt); break
			}
		}, 
		border: compoundBorder(matteBorder(0, 0, 1, 0, color: Color.lightGray), emptyBorder(5)))) {
	
	// brg 3/31/2014: Binding enable property to model.sections so list GUI updates properly on project load.
	// A little kludgy but jives with modern (0.9.4+) Griffon convention better than edt block in controller. 
	list(id:'sections', model: new EventListModel(model.sections), enable: bind { model.sections }, mouseClicked: { evt -> controller.handleClick(evt) })
}

//handle enter on the section list
sections.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), 'openAction')
sections.actionMap.put('openAction', openAction)