/*
 * Copyright (c) CSD Facility 2015.
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

actions {
	action(id: 'browseAction', name:'Choose Section Metadata File...', closure: controller.actions.browse)
}

panel(id:'root', layout: new MigLayout('fill, wrap 2'), border: etchedBorder()) {
	label("Section metadata file requirements:", constraints:'wrap, span 2')
	label("  - CSV (comma-separated values) format", constraints:'wrap, span 2')
	label("  - Three columns, in order: section name, top depth (m), bottom depth (m).", constraints: 'wrap, span 2')
	label("  - No column header/label row", constraints:'wrap, span 2, gapbottom 10px')
	button(text:"Choose Metadata File...", action: browseAction)
	label(id:'metadataFileLabel', text: bind { model.metadataFile?.name ?: "[no metadata file selected]"})
}