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
import static griffon.util.GriffonApplicationUtils.*

import ca.odell.glazedlists.FilterList
import ca.odell.glazedlists.TextFilterator
import ca.odell.glazedlists.swing.EventListModel
import ca.odell.glazedlists.swing.TextComponentMatcherEditor

import java.awt.*
import java.awt.image.BufferedImage

import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.event.DocumentListener

import net.miginfocom.swing.MigLayout

// build our actions
build(SchemeEditorActions)

// force PreviewPanel borders to redraw correctly by pretending panel is non-opaque,
// otherwise artifacts appear in borders around preview image on Win
def preview = new PreviewPanel()
preview.setOpaque(false)

// build our main application
application(title:'Scheme Editor', pack:true, locationByPlatform:true, layout: new MigLayout('fill')) {
	menuBar() {
		menu(text: 'File', mnemonic: 'F') {
			menuItem(newAction)
			menuItem(openAction)
			separator()
			menuItem(saveAction)
			menuItem(saveAsAction)
			if (!isMacOSX) {
				separator()
				menuItem(exitAction)
			}
		}
	}
	
	// master
	label('Id:', constraints: 'sg 1, split, right')
	textField(id:'schemeId', constraints:'growx, wrap', action: updateSchemeAction)
	label('Name:', constraints: 'sg 1, split, right')
	textField(id:'schemeName', constraints:'growx, wrap', action: updateSchemeAction)
	label('Type:', constraints: 'sg 1, split, right')
	comboBox(id:'schemeType', items: ['lithology', 'symbol'], editable: true, constraints: 'growx, wrap', action: updateSchemeAction)
    label('Entries:', constraints: 'sg 1, wrap')
    scrollPane(constraints:'span 2, grow, wrap, h 30%') {
    	list(id:"schemeEntries", cellRenderer: new EntryListRenderer(), model: new EventListModel(model.schemeEntries))
    }
	button(action: addEntryAction, constraints:'split, right')
    button(action: removeEntryAction, constraints: 'right, wrap')
	
	// details
	panel(layout: new MigLayout('fill'), border: titledBorder('Entry Details'), constraints: 'grow') {
	    label('Name:', constraints: 'sg 2, split, right')
	    textField(id:'entryName', constraints: 'span, growx, wrap', editable: true, action: updateEntryAction)
	    label('Code:', constraints: 'sg 2, split, right')
	    textField(id:'entryCode', constraints: 'span, growx, wrap', editable: true, action: updateEntryAction)
	    label('Group:', constraints: 'sg 2, split, right')
	    textField(id:'entryGroup', constraints: 'span, growx, wrap', editable: true, action: updateEntryAction)
	    button(text: 'Set Image', constraints: 'split, growx', action:updateImageAction)
	    button(text: 'Set Color', constraints: 'growx, wrap', action:updateColorAction)
	    widget(preview, id:'preview', constraints:'span 2, grow, h 200px, wrap', border: titledBorder('Preview'))
		button(action: saveEntryAction, constraints: 'split, right')
		button(action: revertEntryAction, constraints: 'right')
	}
}
schemeEntries.addListSelectionListener(controller)
schemeId.document.addDocumentListener({ controller.schemeChanged() } as DocumentListener)
schemeName.document.addDocumentListener({ controller.schemeChanged() } as DocumentListener)
entryName.document.addDocumentListener({ controller.entryChanged() } as DocumentListener)
entryCode.document.addDocumentListener({ controller.entryChanged() } as DocumentListener)
entryGroup.document.addDocumentListener({ controller.entryChanged() } as DocumentListener)


// build our image chooser panel
panel(id:'imageChooser', layout: new MigLayout('fill')) {
	label('Filter:', constraints: 'split')
	imageFilter = textField(id:'imageFilter', constraints:'span,growx, wrap')
	scrollPane(constraints: 'grow, wrap') {
    	list(id:"standardImages", cellRenderer: new EntryListRenderer(), model:new EventListModel(new FilterList(model.standardImages, 
    			new TextComponentMatcherEditor(imageFilter, ([getFilterStrings: { list, obj -> list.add(obj?.name) }] as TextFilterator)))))
    }
	button(constraints:'right', action:customImageAction)
}

// our custom list cell renderer
class EntryListRenderer extends DefaultListCellRenderer {
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
	    def label = super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
	    if (value?.name) { label.text = value.name }
	    if (value?.icon) { label.icon = value.icon } 
	    return label;
	}
}

// our custom preview panel
class PreviewPanel extends JPanel {
	Color color = null
	BufferedImage image = null
	boolean tileImage = true
	
	protected void paintComponent(Graphics g) {
		def insets = getInsets()
		def b = getVisibleRect()
		def r = new Rectangle((int) (b.x + insets.left), 
				(int) (b.y + insets.top), 
				(int) (b.width - insets.left - insets.right), 
				(int) (b.height - insets.top - insets.bottom))
		
		// handle our color
		if (color != null) {
			g.setPaint(color)
			g.fill(r)
		} else {
			g.setPaint(Color.white)
			g.fill(r)
		}
		
		// handle our image
		if (image != null) {
			if (tileImage) {
				g.setPaint(new TexturePaint(image, new Rectangle(0, 0, image.width, image.height)))
				g.fill(r)
			} else {
				g.drawImage(image, (int) (r.centerX - image.width/2), (int) (r.centerY - image.height/2), null)
			}
		}
	}
}