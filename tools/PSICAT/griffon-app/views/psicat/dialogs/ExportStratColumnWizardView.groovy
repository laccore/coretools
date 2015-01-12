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
	action(id: 'browseAction', name:'Choose Metadata File...', closure: controller.actions.browse)
}

panel(id:'root', layout: new MigLayout('fill, wrap 2'), border: etchedBorder()) {
	label('Outputs a strat column in PDF format comprising sections in user-provided metadata file.', constraints: 'span 2')
	label("Metadata file must be CSV with three columns: section name, top depth (m), bottom depth (m).", constraints: 'span 2, gapbottom 10px')
	button(text:"Choose Metadata File...", action: browseAction)
	label(id:'metadataFileLabel', text:"[no metadata file selected]")
}
