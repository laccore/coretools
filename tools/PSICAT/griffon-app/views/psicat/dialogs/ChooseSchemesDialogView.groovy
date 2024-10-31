/*
 * Copyright (c) CSD Facility, 2014.
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

import ca.odell.glazedlists.swing.EventListModel

actions {
	action(id: 'addAction', name: 'Add Scheme...', closure: controller.actions.add)
	action(id: 'deleteAction', name: 'Remove Selected Scheme', closure: controller.actions.delete)
}

panel(id:'root', layout: new MigLayout('fill'), border:etchedBorder()) {	
	scrollPane(constraints:'grow, wrap, span 2', border:etchedBorder()) {
		list(id:'schemeList', model: new EventListModel(model.schemes), constraints: 'wrap, span 2, growy')
	}

	button(action: addAction)
	button(id:'deleteButton', action: deleteAction) // init enabled to false once below bug is resolved

	// brgtodo 4/6/2014: This binding causes an exception to be thrown when the dialog is closed due
	// to a bug in Griffon 0.2. The bug is fixed in 0.3.2, try upgrading.	
	//bind(source:schemeList, sourceEvent:'valueChanged', sourceValue:{schemeList.selectedIndex != -1},
	//	target:deleteButton, targetProperty:'enabled')
}
