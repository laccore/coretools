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

import groovy.swing.factory.ButtonGroupFactory;

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
	action(id: 'chooseAlternateGrainSize', name:'...', enabled: bind { model.drawGrainSize && !model.useProjectGrainSize },
		closure: controller.actions.chooseAlternateGrainSize)
	action(id: 'doExport', name:'Export', enabled:bind { model.metadataPath && model.exportPath },
		closure: controller.actions.doExport)
}

panel(id:'root', layout: new MigLayout('fill, wrap'), border: etchedBorder()) {
	panel(border: titledBorder('Section Depths or Splice Interval File'), layout: new MigLayout('fill, wrap'), constraints:'growx') {
		label("File:", constraints:"split 3")
		label(text: bind(source:model, sourceProperty:'metadataPath'), constraints:'width min(200px), growx')
		button(action:chooseMetadata)
	}
	
	panel(border: titledBorder("Drawing Options"), layout: new MigLayout('','[][grow][]'), constraints:'growx') {
		hbox(constraints: 'growx, wrap') {
			label(text:"Start Depth: ")
			textField(text: bind(source:model, sourceProperty:'startDepth', mutual:true), constraints:'growx')
			hstrut(20)
			label(text:"End Depth: ")
			textField(text: bind(source:model, sourceProperty:'endDepth', mutual:true), constraints:'growx')
		}
		hbox(constraints: 'growx, wrap') {
			checkBox(text:"Draw Symbols in Intervals:", selected:bind(source:model, sourceProperty:'drawSymbols', mutual:true))
			buttonGroup().with {
				add radioButton(text:"Aggregated", selected:bind(source:model, sourceProperty:'aggregateSymbols', mutual:true),
					enabled:bind { model.drawSymbols })
				add radioButton(text:"Every Instance", selected:bind { !model.aggregateSymbols }, enabled:bind { model.drawSymbols })
			}
		}
		hbox(constraints:'growx, wrap') {
			checkBox(text:"Draw Interval Widths Based on:", selected:bind(source:model, sourceProperty:'drawGrainSize', mutual:true), constraints:'wrap')
			buttonGroup().with {
				add radioButton(text:"Project Grain Sizes", selected:bind(source:model, sourceProperty:'useProjectGrainSize', mutual:true),
					enabled:bind { model.drawGrainSize })
				add radioButton(text:"Grain Size File", selected:bind { !model.useProjectGrainSize }, enabled:bind { model.drawGrainSize })
			}
		}
		hbox(constraints:'growx, wrap') {
			hstrut(30)
			label(text:"File: ", enabled:bind { model.drawGrainSize && !model.useProjectGrainSize })
			textField(text: bind(source:model, sourceProperty:'alternateGrainSizePath', mutual:true),
				enabled: bind { model.drawGrainSize && !model.useProjectGrainSize },
				constraints:'growx')
			button(action:chooseAlternateGrainSize)
		}
		hbox(constraints:'growx,wrap') {
			hstrut(30)
			checkBox(text:"Draw Grain Size Scale Labels", enabled: bind { model.drawGrainSize },
				selected:bind(source:model, sourceProperty:'drawGrainSizeLabels', mutual:true))
		}
		hbox(constraints:'growx,wrap') {
			checkBox(text:"Draw Legend", selected: bind(source:model, sourceProperty:'drawLegend', mutual:true))
			checkBox(text:"Draw Section Names and Ranges", selected:bind(source:model, sourceProperty:'drawSectionNames', mutual:true))
		}
		hbox(constraints:'growx') {
			checkBox(text:"Draw Interval Borders", selected:bind(source:model, sourceProperty:'drawIntervalBorders', mutual:true))
			checkBox(text:"Draw dm Ruler Ticks", selected:bind(source:model, sourceProperty:'drawDms', mutual:true),
				toolTipText:"If space allows, draw decimeter ticks on ruler", constraints:'growx')
		}
	}
	
	panel(border: titledBorder("Export File"), layout: new MigLayout('','[][grow][]'), constraints:'growx') {
		label('File:')
		textField(text: bind(source:model, sourceProperty:'exportPath', mutual:true), constraints:'growx')
		button(action:chooseExport, constraints:'wrap')
		checkBox(text:"Include log detailing export process", selected:bind(source:model, sourceProperty:'exportLog', mutual:true), constraints:'span,wrap')
	}
	
	panel(layout: new MigLayout('','[grow][]',''), constraints:'growx') {
		progressBar(id:'progress', minimum:0, maximum:100, stringPainted:true, string:'', constraints:'growx, gapright 10px')
		button(action:doExport)
	}
}
