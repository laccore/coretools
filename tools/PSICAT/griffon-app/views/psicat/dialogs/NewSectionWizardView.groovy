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
	action(id: 'createAction', name:'Create', closure: controller.actions.create)
	action(id: 'closeAction', name:'Cancel', closure: controller.actions.close)
}

dialog(id:'newSectionDialog', title:'New Section', owner:app.windowManager.windows[0], pack:true, modal:true) {
	vbox {
		panel(id:'root', layout: new MigLayout('fill'), border: etchedBorder()) {
			// section name
			label('Name:')
			textField(text: bind(source: model, sourceProperty:'name', mutual: true), constraints: 'width min(200px), growx, span, wrap')
		
			// range
			label('Range:')
			textField(columns: 4, text: bind(source: model, sourceProperty:'top', mutual: true), inputVerifier: CustomVerifier.NUMBER, constraints: 'split')
			label('-', constraints: 'split')
			textField(columns: 4, text: bind(source: model, sourceProperty:'base', mutual: true), inputVerifier: CustomVerifier.NUMBER, constraints: 'split, wrap')
			separator(constraints: 'growx, span, wrap')
		
			label('Image:')
			textField(text: bind(source: model, sourceProperty:'filePath', mutual:true), constraints:'width min(200px), growx')
			button(action: browseAction, constraints: 'wrap')
		}
		panel(layout:new MigLayout('fill')) {
			button(action:closeAction, constraints:'align center')
			button(action:createAction, constraints:'align center')
		}
	}
}

//newSectionDialog.show()