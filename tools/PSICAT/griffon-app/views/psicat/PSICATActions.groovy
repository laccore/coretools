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

import org.andrill.coretools.geology.models.Interval

actions {
	action(
		id: 'exitAction',
		name: 'Exit',
		closure: controller.actions['exit']
	)
	action(
		id: 'newProjectAction',
		name: 'Project...',
		closure: controller.actions['newProject'],
		accelerator: shortcut('N', 1),
		shortDescription: 'Create a new project'
	)
	action(
		id: 'importImageAction',
		name: 'Section(s) from Images...',
		enabled: bind { model.project != null },
		closure: controller.actions['importImage'],
		accelerator: shortcut('I'),
		shortDescription: 'Create sections from image data'
	)
	action(
		id: 'newSectionAction',
		name: 'Section...',
		closure: controller.actions['newSection'],
		enabled: bind { model.project != null },
		accelerator: shortcut('N'),
		shortDescription: 'Create a new section'
	)
	action(
		id: 'openProjectAction',
		name: 'Open Project...',
		closure: controller.actions['openProject'],
		accelerator: shortcut('O'),
		shortDescription: 'Open a project'
	)
	action(
		id: 'closeProjectAction',
		name: 'Close Project',
		closure: controller.actions['closeProject'],
		enabled: bind { model.project != null },
		shortDescription: 'Close the current project'
	)
	action(
		id: 'openSectionAction',
		name: 'Open Section',
		closure: controller.actions['openSection']
	)
	action(
		id: 'closeAction',
		name: 'Close Section',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['close'],
		accelerator: shortcut('W'),
		shortDescription: 'Close the active diagram'
	)
	action(
		id: 'closeAllAction',
		name: 'Close All Sections',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['closeAll'],
		accelerator: shortcut('W', 1),
		shortDescription: 'Close all diagrams'
	)
	action(
		id: 'saveAction',
		name: 'Save Section',
		enabled: bind { model.activeDiagram != null && model.diagramState.dirty },
		closure: controller.actions['save'],
		accelerator: shortcut('S'),
		shortDescription: 'Save the diagram'
	)
	action(
		id: 'saveAllAction',
		name: 'Save All Sections',
		enabled: bind { model.diagramState.dirty || model.anyDirty },
		closure: controller.actions['saveAll'],
		accelerator: shortcut('S', 1),
		shortDescription: 'Save all diagrams'
	)
	action(
		id: 'deleteSectionAction',
		name: 'Delete Section(s)...',
		enabled: bind { model.project != null },
		closure: controller.actions['deleteSection'],
		shortDescription: 'Delete selected section(s)'
	)
	action(
		id: 'deleteAction',
		name: 'Delete',
		enabled: bind { model.diagramState.selection != null },
		closure: controller.actions['delete'],
		accelerator: javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0)
	)
	action(
		id: 'splitAction',
		name: 'Split Interval',
		enabled: bind { model.diagramState?.selection instanceof Interval },
		closure: controller.actions['splitInterval']
	)
	action(
		id: 'chooseSchemesAction',
		name: 'Choose Schemes...',
		enabled: bind { model.project != null },
		closure: controller.actions['chooseSchemes'],
		shortDescription: 'Add/remove schemes used in the current project'
	)
	action(
		id: 'grainSizeScaleAction',
		name: 'Grain Size Scale...',
		enabled: bind { model.project != null },
		closure: controller.actions['grainSizeScale'],
		shortDescription: "Edit the current project's grain size scale"
	)
	action(
		id: 'undoAction',
		name: 'Undo',
		enabled: bind { model.diagramState.canUndo },
		closure: controller.actions['undo'],
		accelerator: shortcut('Z'),
		shortDescription: 'Undo'
	)
	action(
		id: 'redoAction',
		name: 'Redo',
		enabled: bind { model.diagramState.canRedo },
		closure: controller.actions['redo'],
		accelerator: shortcut('Z', 1),
		shortDescription: 'Redo'
	)
	action(
		id: 'findAndReplaceAction',
		name: 'Find and Replace...',
		enabled: bind { model.project != null },
		closure: controller.actions['findAndReplace'],
		shortDescription: 'Find and replace lithologies and symbols'
	)
	action(
		id: 'auditProjectAction',
		name: 'Audit Project...',
		enabled: bind { model.project != null },
		closure: controller.actions['auditProject'],
		shortDescription: "Find and report potential problems in the current project"
	)
	action(
		id: 'aboutAction',
		name: 'About',
		closure: controller.actions['about'],
		shortDescription: 'About PSICAT'
	)
	action(
		id: 'updateAction',
		name: 'Check for Updates...',
		closure: controller.actions['versionCheck'],
		shortDescription: 'Check for a more recent version of PSICAT'
	)
	action(
		id: 'documentationAction',
		name: 'User Guide',
		closure: controller.actions['documentation'],
		shortDescription: 'View the user guide'
	)
	action(
		id: 'feedbackAction',
		name: 'Leave Feedback...',
		closure: controller.actions['feedback'],
		shortDescription: 'Report a bug, request an enhancement, etc.'
	)
	action(
		id: 'rotateAction',
		name: 'Horizontal',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['rotate'],
		accelerator: shortcut('R'),
		shortDescription: 'Rotate diagram'
	)
	action(
		id: 'zoomInAction',
		name: 'Zoom In',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['zoomIn'],
		accelerator: shortcut(java.awt.event.KeyEvent.VK_EQUALS),
		shortDescription: 'Zoom in'
	)
	action(
		id: 'zoomOutAction',
		name: 'Zoom Out',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['zoomOut'],
		accelerator: shortcut(java.awt.event.KeyEvent.VK_MINUS),
		shortDescription: 'Zoom out'
	)
	action(
		id: 'zoom0Action',
		name: '0.01',
		closure: controller.actions['zoom0'],
		accelerator: shortcut('1'),
		enabled: bind { model.activeDiagram != null }
	)
	action(
		id: 'zoom1Action',
		name: '0.10',
		closure: controller.actions['zoom1'],
		accelerator: shortcut('2'),
		enabled: bind { model.activeDiagram != null }
	)
	action(
		id: 'zoom2Action',
		name: '1',
		closure: controller.actions['zoom2'],
		accelerator: shortcut('3'),
		enabled: bind { model.activeDiagram != null }
	)
	action(
		id: 'zoom3Action',
		name: '10',
		closure: controller.actions['zoom3'],
		accelerator: shortcut('4'),
		enabled: bind { model.activeDiagram != null }
	)
	action(
		id: 'zoom4Action',
		name: '100 ',
		closure: controller.actions['zoom4'],
		accelerator: shortcut('5'),
		enabled: bind { model.activeDiagram != null }
	)
	action(
		id: 'zoomOtherAction',
		name: 'Other...',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['zoomOther'],
		accelerator: shortcut('0'),
		shortDescription: 'Zoom to a specific per page'
	)
	action(
		id: 'cmUnitsAction',
		name: 'cm',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['cmUnits'],
		shortDescription: 'Set preferred units to centimeters'
	)
	action(
		id: 'mUnitsAction',
		name: 'm',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['mUnits'],
		shortDescription: 'Set preferred units to meters'
	)
	action(
		id: 'inUnitsAction',
		name: 'in',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['inUnits'],
		shortDescription: 'Set preferred units to inches'
	)
	action(
		id: 'ftUnitsAction',
		name: 'ft',
		enabled: bind { model.activeDiagram != null },
		closure: controller.actions['ftUnits'],
		shortDescription: 'Set preferred units to feet'
	)
	action(
		id: 'exportDiagramAction',
		name: 'Diagram...',
		enabled: bind { model.project != null },
		closure: controller.actions['exportDiagram'],
		shortDescription: 'Export diagrams to PDF or other image formats'
	)
	action(
		id: 'exportTabularAction',
		name: 'Tabular Data...',
		enabled: bind { model.project != null },
		closure: controller.actions['exportTabular'],
		shortDescription: 'Export data to Excel'
	)
	action(
		id: 'exportStratColumnAction',
		name: 'Strat Column...',
		enabled: bind { model.project != null },
		closure: controller.actions['exportStratColumn'],
		shortDescription: 'Export strat column as PDF'
	)
	action(
		id: 'importLegacyAction',
		name: 'Legacy PSICAT Data',
		enabled: bind { model.project != null },
		closure: controller.actions['importLegacy'],
		shortDescription: 'Import data from previous PSICAT versions'
	)
	action(
		id: 'importTabularAction',
		name: 'Tabular Data',
		enabled: bind { model.project != null },
		closure: controller.actions['importTabular'],
		shortDescription: 'Import tabular data from Excel'
	)
	action(
		id: 'openDISAction',
		name: 'Open from DIS',
		accelerator: shortcut('D'),
		closure: controller.actions['openDIS'],
		shortDescription: 'Open a section or hole from the DIS'
	)
}