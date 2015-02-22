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

import org.andrill.coretools.model.edit.EditableProperty
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeEntry
import org.andrill.coretools.model.scheme.SchemeManager
import org.andrill.coretools.ui.widget.swing.SchemeEntryWidget

import net.miginfocom.swing.MigLayout
import psicat.util.*

actions {
	action(id: 'chooseMetadata', name:'...', closure: controller.actions.chooseMetadata)
	action(id: 'chooseExport', name:'...', closure: controller.actions.chooseExport)
	action(id: 'doExport', name:'Export', enabled:bind {model.metadataPath && model.exportPath}, closure: controller.actions.doExport)
}

panel(id:'root', layout: new MigLayout('fill, wrap'), border: etchedBorder()) {
	panel(border: titledBorder('Section Metadata File'), layout: new MigLayout('fill, wrap'), constraints:'growx') {
		label("Section metadata file requirements:")
		label("  - CSV (comma-separated values) format")
		label("  - Three columns, in order: section name, top depth (m), bottom depth (m).")
		label("  - No column header/label row", constraints:'gapbottom 10px')
	
		label("Metadata File:", constraints:"split 3")
		textField(text: bind(source:model, sourceProperty:'metadataPath', mutual:true), constraints:'width min(200px), growx')
		button(action:chooseMetadata)
	}
	
	panel(border: titledBorder("Export File"), layout: new MigLayout('','[][grow][]',''), constraints:'growx') {
		label('File:')
		textField(text: bind(source:model, sourceProperty:'exportPath', mutual:true), constraints:'growx')
		button(action:chooseExport)
	}
	
	panel(layout: new MigLayout('','[grow][]',''), constraints:'growx') {
		progressBar(id:'progress', minimum:0, maximum:100, stringPainted:true, string:'', constraints:'growx, gapright 10px')
		button(action:doExport)
	}
}