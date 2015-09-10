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
	action(id: 'importAction', name:'Import', closure: controller.actions.doImport)
}

panel(id:'root', layout: new MigLayout('fill'), border: etchedBorder()) {	
	// directory
	label('Import File:', constraints: 'split')
	textField(text: bind(source: model, sourceProperty:'filePath', mutual:true), constraints:'width min(200px), growx')
	button(text:'...', action: browseAction, constraints: 'wrap')

	// options
	checkBox(text: 'Copy Images', selected:bind(source:model, sourceProperty:'copyImages', mutual:true), constraints:'wrap')
	
	separator(constraints: 'span, growx, wrap')
	
	// progress bar
	panel(layout:new MigLayout('','[grow][]',''), constraints:'span, growx, wrap') {
		progressBar(id:'progress', minimum:0, maximum:100, stringPainted:true, string:'', constraints:'growx, gapright 10px')
		button(id:'importBtn', action: importAction)
	}
	
	panel(border: titledBorder('Import Log')) {
		scrollPane {
			textArea(id:'logArea', text:'[Click Import to start]', lineWrap:true, wrapStyleWord:true,
				columns:50, rows:10, editable:false)
		}
	}
}