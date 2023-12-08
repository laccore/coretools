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
/**
 * Actions for the scheme editor.
 */
actions {
	// Menu Items
	action( id: 'newAction',
		name: 'New',
		mnemonic: 'N',
		enabled: bind { model.schemeFile != null },
		accelerator: shortcut('N'),
		closure: controller.newScheme,
		shortDescription: 'New scheme'
	)
	action( id: 'openAction',
		name: 'Open...',
		closure: controller.open,
		mnemonic: 'O',
		accelerator: shortcut('O'),
		shortDescription: 'Open a scheme'
	)
	action( id: 'closeAction',
		name: 'Close',
		closure: controller.close,
		mnemonic: 'W',
		enabled: bind { model.schemeFile != null },
		accelerator: shortcut('W'),
		shortDescription: 'Close current scheme'
	)
	action( id: 'exitAction',
		name: 'Quit',
		closure: controller.exit,
		mnemonic: 'Q',
		accelerator: shortcut('Q'),
	)
	action( id: 'saveAction',
		name: 'Save',
		enabled: bind { model.schemeValid },
		closure: controller.save,
		mnemonic: 'S',
		accelerator: shortcut('S'),
		shortDescription: 'Save scheme'
	)
	action( id: 'saveAsAction',
		name: 'Save As...',
		enabled: bind { model.schemeValid },
		closure: controller.saveAs
	)
	action(
		id: 'exportOnePageCatalogAction',
		name: 'Export Catalog...',
		enabled: bind { model.schemeValid },
		closure: controller.exportOnePageCatalog
	)
	action(
		id: 'exportPaginatedCatalogAction',
		name: 'Export Paginated Catalog...',
		enabled: bind { model.schemeValid },
		closure: controller.exportPaginatedCatalog
	)
	action( id: 'updateSchemeAction',
		closure: controller.schemeChanged
	)
	action( id: 'updateSchemeTypeAction',
		closure: controller.schemeTypeChanged
	)
	
	// entry actions
	action( id:'addEntryAction',
		name:'Add Entry',
		closure: controller.addEntry
	)
	action( id:'removeEntryAction',
		name:'Remove Entry',
		enabled: bind { model.entry != null },
		closure: controller.removeEntry
	)
	action( id:'updateColorAction',
		name: 'Set Color',
		enabled: bind { model.entry != null },
		closure: controller.updateColor
	)
	action( id:'updateImageAction',
		name: 'Set Image',
		enabled: bind { model.entry != null },
		closure: controller.updateImage
	)
	action( id:'updatePreviewAction',
		name:'Update Preview',
		enabled: bind { model.entry != null },
		closure: controller.updatePreview
	)
	action( id:'customImageAction', 
		name: 'Custom Image', 
		closure: controller.customImage
	)
}
