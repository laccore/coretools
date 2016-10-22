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
import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.TextFilterator
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.gui.WritableTableFormat
import ca.odell.glazedlists.swing.EventListModel
import ca.odell.glazedlists.swing.DefaultEventSelectionModel
import ca.odell.glazedlists.swing.DefaultEventTableModel
import ca.odell.glazedlists.swing.TextComponentMatcherEditor

import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage

import javax.swing.ListSelectionModel
import javax.swing.ListCellRenderer
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.event.DocumentListener

import java.util.prefs.Preferences

import groovy.swing.SwingBuilder
import net.miginfocom.swing.MigLayout

// build our actions
build(SchemeEditorActions)

// restore previous dimensions of main window
def prefs = Preferences.userNodeForPackage(SchemeEditorController)
def mainWidth = prefs.getDouble('schemeEditor.mainViewWidth', 300.0) as Integer
def mainHeight = prefs.getDouble('schemeEditor.mainViewHeight', 600.0) as Integer
def xpos = prefs.getDouble('schemeEditor.mainViewX', 50.0) as Integer
def ypos = prefs.getDouble('schemeEditor.mainViewY', 50.0) as Integer

// force PreviewPanel borders to redraw correctly by pretending panel is non-opaque,
// otherwise artifacts appear in borders around preview image on Win
def preview = new PreviewPanel()
preview.setOpaque(false)

// build our main application
application(title: "Scheme Editor ${app.applicationProperties['app.version']}",
			id:'mainView', size:[mainWidth, mainHeight], location:[xpos,ypos], layout: new MigLayout('fill', '', '[][grow][]')) {
	menuBar() {
		menu(text: 'File', mnemonic: 'F') {
			menuItem(newAction)
			menuItem(openAction)
			separator()
			menuItem(closeAction)
			separator()
			menuItem(saveAction)
			menuItem(saveAsAction)
			separator()
			menuItem(exportOnePageCatalogAction)
			menuItem(exportPaginatedCatalogAction)
			if (!isMacOSX) {
				separator()
				menuItem(exitAction)
			}
		}
	}
	
	panel(layout:new MigLayout("fill, wrap 2, insets 5", "[][grow]", ""), constraints:'growx, wrap', border:titledBorder("Scheme Properties")) {
		label('Name:')
		textField(id:'schemeName', constraints:'growx', action: updateSchemeAction)
		label('ID:')
		textField(id:'schemeId', constraints:'growx', action: updateSchemeAction)
		label('Type:')
		comboBox(id:'schemeType', items: ['lithology', 'symbol'], editable: false, constraints: 'growx', action: updateSchemeAction)
	}
	panel(layout:new MigLayout("fill, wrap, insets 5", '', '[grow][]'), constraints:'grow, wrap', border:titledBorder("Scheme Entries")) {
	    scrollPane(constraints:'grow') {
	    	table(id:"schemeEntries", model: new DefaultEventTableModel(model.schemeEntries, new SchemeEntryTableFormat(model.schemeEntries)),
				selectionModel: new DefaultEventSelectionModel(model.schemeEntries), selectionMode:ListSelectionModel.SINGLE_SELECTION)
	    }
		hbox {
			button(action: addEntryAction)
			button(action: removeEntryAction)
		}
	}

	panel(layout:new MigLayout('fill, wrap, insets 5', '', '[grow][]'), border:titledBorder("Entry Appearance")) {
		widget(preview, id:'preview', constraints:'grow, h 200px')
		hbox {
			button(text: 'Set Image', action:updateImageAction)
			button(text: 'Set Color', action:updateColorAction)
			//comboBox(id:'colorCombo', items: [], renderer:new ColorComboRenderer(), editable:false)
		}
	}
}
			
schemeName.addFocusListener({ controller.schemeNameLostFocus() } as FocusListener)
schemeEntries.selectionModel.addListSelectionListener(controller)
schemeEntries.addKeyListener(new SchemeEntryTableKeyListener())
schemeId.document.addDocumentListener({ controller.schemeChanged() } as DocumentListener)
schemeName.document.addDocumentListener({ controller.schemeChanged() } as DocumentListener)
model.schemeEntries.addListEventListener(controller)

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

// consume up and down arrow keys and adjust selected row - allowing JTable
// to handle these messages results in a table change event, which screws
// up dirty state tracking - interestingly, changing selection by clicking
// with the mouse does not cause a table change event.
class SchemeEntryTableKeyListener extends KeyAdapter {
	public void keyPressed(KeyEvent event) {
		//System.out.println("Key Pressed!");
		JTable theTable = (JTable) event.getSource();
		int curRow = theTable.getSelectedRow();
		if (event.getKeyCode() == KeyEvent.VK_DOWN) {
			if (curRow < theTable.getRowCount() - 1) {
				theTable.setRowSelectionInterval(curRow + 1, curRow + 1);
				theTable.scrollRectToVisible(new Rectangle(theTable.getCellRect(curRow + 1, 0, true)))
			}
			event.consume();
		} else if (event.getKeyCode() == KeyEvent.VK_UP) {
			if (curRow > 0) {
				theTable.setRowSelectionInterval(curRow - 1, curRow - 1);
				theTable.scrollRectToVisible(new Rectangle(theTable.getCellRect(curRow - 1, 0, true)))
			}
			event.consume();
		}
	}
}

class SchemeEntryTableFormat implements WritableTableFormat {
	def colnames = ['name', 'code']
	def entries = []
	public SchemeEntryTableFormat(entries) { this.entries = entries }
	public int getColumnCount() { return 2; }
	public Object getColumnValue(Object baseObject, int col) {
		def propName = colnames[col]
		return baseObject."$propName"
	}
	public String getColumnName(int col) { return colnames[col].substring(0,1).toUpperCase() + colnames[col].substring(1) }
	
	public boolean isEditable(Object obj, int col) { return col == 0 }
	public Object setColumnValue(Object obj, Object newValue, int column) {
		if (column == 0) {
			obj.name = newValue
			if (!obj.code) {
				obj.code = createUniqueCode(obj.name)
				def objElt = this.entries.find { obj }
				if (objElt) {
					def objIndex = this.entries.indexOf(objElt)
					this.entries.set(objIndex, objElt) // set object to itself to update EventList
				}
			}
			return obj
		}
		return null
	}
	
	private String createUniqueCode(name) {
		def result = null
		def baseCode = SchemeHelper.codeFromName(name)
		int suffixNum = 1
		def suffix = ""
		while (true) {
			def code = baseCode + suffix
			if (this.entries.find { it.code?.equals(code) } != null) {
				suffix = "$suffixNum"
				suffixNum++
			} else {
				result = code	
				break
			}
		}
		return result
	}
}

// Choose Image list cell renderer
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
