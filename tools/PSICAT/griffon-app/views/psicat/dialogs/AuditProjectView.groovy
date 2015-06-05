/*
 * Copyright (c) Brian Grivna, 2015.
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
	action(id:'auditAction', name:'Audit', closure: controller.actions.audit)
}

panel(id:'root', layout: new MigLayout('fill, wrap 1'), border: etchedBorder()) {
	label('Check Project for Selected Problems:', constraints:'gapbottom 10px')
	checkBox(text:'Undescribed Sections', selected: bind(source:model, sourceProperty:'undescribedSecs', mutual:true))
	checkBox(text:'Sections With No Defined Intervals', selected: bind(source:model, sourceProperty:'noIntervalSecs', mutual:true))
	checkBox(text:'"None" Intervals Without Descriptions', selected: bind(source:model, sourceProperty:'emptyUndescribedInts', mutual:true))
	checkBox(text:'"None" Symbols Without Descriptions', selected: bind(source:model, sourceProperty:'emptyUndescribedSyms', mutual:true))
	checkBox(text:'Zero-Length Intervals', selected: bind(source:model, sourceProperty:'zeroLengthInts', mutual:true))
	checkBox(text:'Inverted Intervals (base above top)', selected: bind(source:model, sourceProperty:'invertedInts', mutual:true))
	separator()
	panel(border: titledBorder('Audit Report Log')) {
		scrollPane {
			textArea(id:'logArea', text:'Click "Audit" to check project for selected problems', lineWrap:true, wrapStyleWord:true,
				columns:50, rows:10, editable:false)
		}
	}
	
	panel(layout:new MigLayout('', '[grow][]', ''), constraints:'span 2, growx') {
		progressBar(id:'progress', minimum:0, maximum:100, stringPainted:true, string:'', constraints:'growx, gapright 10px')
		button(action:auditAction, constraints:'align right')
	}
}
