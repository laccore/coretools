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
package psicat

import static griffon.util.GriffonApplicationUtils.*

menuBar(id: 'menuBar') {
	menu(text: 'File', mnemonic: 'F') {
		menuItem(newProjectAction)
		menuItem(newSectionAction)
		menuItem(openProjectAction)
		menuItem(openDISAction)
		separator()
		menuItem(closeAction)
		menuItem(closeAllAction)
		separator()
		menuItem(saveAction)
		menuItem(saveAllAction)
		separator()
		menu(text: 'Import', enabled: bind { model.project != null }) {
			menuItem(importImageAction)
			menuItem(importTabularAction)
			menuItem(importLegacyAction)
		}
		menu(text: 'Export', enabled: bind { model.project != null }) {
			menuItem(exportTabularAction)
			menuItem(exportDiagramAction)
		}
		if (!isMacOSX) {
			separator()
			menuItem(exitAction)
		}
	}

	menu(text: 'Edit', mnemonic: 'E') {
		menuItem(deleteAction)
		menuItem(undoAction)
		menuItem(redoAction)
	}


	menu(text: 'View', mnemonic:'V') {
		menuItem(rotateAction)
		separator()
		menuItem(zoomInAction)
		menuItem(zoomOutAction)
		menu(text: 'Zoom Level', enabled: bind { model.scene != null }) {
			menuItem(zoom0Action, text: bind { ".01 ${model.diagramState.units}" })
			menuItem(zoom1Action, text: bind { ".10 ${model.diagramState.units}" })
			menuItem(zoom2Action, text: bind { "1 ${model.diagramState.units}" })
			menuItem(zoom3Action, text: bind { "10 ${model.diagramState.units}" })
			menuItem(zoom4Action, text: bind { "100 ${model.diagramState.units}" })
			menuItem(zoomOtherAction)
		}
		separator()
		menu(text:'Units') {
			radioButtonMenuItem(mUnitsAction,  selected: bind { model.diagramState.units == 'm'})
			radioButtonMenuItem(cmUnitsAction, selected: bind { model.diagramState.units == 'cm'})
			radioButtonMenuItem(ftUnitsAction, selected: bind { model.diagramState.units == 'ft'})
			radioButtonMenuItem(inUnitsAction, selected: bind { model.diagramState.units == 'in'})
		}
	}

	menu(text: 'Help', mnemonic:'H') {
		menuItem(aboutAction)
		menuItem(documentationAction)
		menuItem(feedbackAction)
	}
}