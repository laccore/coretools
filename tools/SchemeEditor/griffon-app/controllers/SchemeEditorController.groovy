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

import java.awt.Image
import java.awt.Rectangle
import java.io.File;
import java.util.zip.ZipFile
import javax.swing.ImageIcon
import javax.swing.JColorChooser
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.filechooser.FileFilter

import ca.odell.glazedlists.event.ListEvent
import ca.odell.glazedlists.event.ListEventListener

/**
 * The SchemeEditor controller.
 */
class SchemeEditorController implements ListSelectionListener, ListEventListener {
    def model
    def view
    
    private SchemeHelper helper
    static File currentOpenDir = new File(System.getProperty("user.home"))
	static File currentSaveDir = new File(System.getProperty("user.home"))

    void mvcGroupInit(Map args) {
    	helper = new SchemeHelper()
    	
		// load our standard images
    	["rsrc:/org/psicat/resources/lithologies/scheme.xml", "rsrc:/org/psicat/resources/symbols/scheme.xml"].each { path ->
    		def url = helper.resolve(path)
    		if (url) {
    			url.withInputStream { stream ->
    				model.standardImages.addAll(helper.read(stream).entries)
    			}
    		}
    	}
    	
    	// create our icons
    	doOutside {
    		model.standardImages.each() { m ->
    			m.icon = helper.iconify(m?.image)
    		}
    	}
    }
	
    /**
     * Exit the application
     */
    def exit = { evt = null ->
		// todo: this only works on Windows, Mac has its own exit routine which we can't veto in
		// this ancient version of Griffon - need to get to 0.9.2!
		if (model.schemeDirty) {
			def choice = JOptionPane.showOptionDialog(app.appFrames[0], "Do you want to save changes to ${model.schemeFile.name}?",
				"Save Before Closing?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, ["Save", "Don't Save", "Cancel"] as String[], "Save")
			if (choice == JOptionPane.YES_OPTION) {
				save(null)
			} else if (choice == JOptionPane.CANCEL_OPTION) {
				return
			}
		}
		app.shutdown()
    }

    /**
     * Create a new scheme file
     */
    def newScheme = { evt = null -> 
    	updateSchemeFile(null)
    	setScheme(null)
    }
	
	/**
	 * Open an existing scheme file.
	 */
	def open = { evt = null ->
		def fc = new JFileChooser(currentOpenDir)
		fc.fileSelectionMode = JFileChooser.FILES_ONLY
		fc.addChoosableFileFilter(new CustomFileFilter(extensions: ['.jar', '.zip'], description: 'Scheme Packs (*.jar)'))
		if (fc.showOpenDialog(app.appFrames[0]) == JFileChooser.APPROVE_OPTION) {
			currentOpenDir = fc.currentDirectory
			updateSchemeFile(fc.selectedFile)

			// parse our file
			doOutside {
				def jar = new ZipFile(model.schemeFile)
				def schemeEntry = jar.entries().find() { it.name.endsWith("scheme.xml") }
				if (schemeEntry) {
					helper.add(fc.selectedFile.toURL())
					def stream = jar.getInputStream(schemeEntry)
					def scheme = helper.read(stream)
					stream.close()
					setScheme(scheme)
				} else {
					JOptionPane.showMessageDialog(app.appFrames[0], "Not a valid Scheme Pack file",
							"Invalid Scheme Pack", JOptionPane.ERROR_MESSAGE)
				}
			}
		}
	}
	
	// close current scheme
	def close = { evt = null ->
		if (model.schemeDirty) {
			def choice = JOptionPane.showOptionDialog(app.appFrames[0], "Do you want to save changes to ${model.schemeFile.name}?",
				"Save Before Closing?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, ["Save", "Don't Save", "Cancel"] as String[], "Save")
			if (choice == JOptionPane.YES_OPTION) {
				save(null)
			} else if (choice == JOptionPane.CANCEL_OPTION) {
				return
			}
		}
		newScheme(null)
	}
    
    /**
     * Called when the scheme id, name, type, and entries are changed.
     */
    def schemeChanged = { evt = null -> 
		model.schemeValid = (view.schemeId.text && view.schemeName.text && view.schemeType.selectedItem && model.schemeEntries.size() > 0)
		setSchemeDirty(true)
    }
    
    /**
     * Set the selected scheme
     */
    def setScheme(scheme) {
    	doLater {
	    	if (scheme) {
	    		view.schemeId.text = scheme.id
	    		view.schemeName.text = scheme.name
	    		view.schemeType.selectedItem = scheme.type
	    		view.schemeEntries.clearSelection()
	    		model.schemeEntries.clear()
	    		model.schemeEntries.addAll(scheme.entries)
	    	} else {
	    		view.schemeId.text = ""
	    		view.schemeName.text = ""
	    		view.schemeType.selectedItem = null
	    		view.schemeEntries.clearSelection()
	    		model.schemeEntries.clear()
	    	}
			setSchemeDirty(false)
    	}
    }
    
	/**
	 * Update model.schemeFile reference and mainView's title bar
	 */
	def updateSchemeFile(file) {
		model.schemeFile = file
		updateTitle()
	}

	/**
	 * Update main window title with current file and dirty state	
	 */
	def updateTitle() {
		doLater {
			def baseTitle = "Scheme Editor ${app.applicationProperties['app.version']}"
			def fileName = model.schemeFile?.name
			def newTitle = baseTitle + (fileName ? " - [$fileName]" : "- [New Untitled Scheme]") + (model.schemeDirty ? "*" : "")
			view.mainView.title = newTitle
		}
	}
	
    /**
     * Save the scheme.
     */
    def save = { evt = null ->
	    if (model.schemeFile == null) {
	    	saveAs(evt)
	    } else {
			saveScheme()
	    }
    }
    
    /**
     * Save the scheme, prompting the user for a filename.
     */
    def saveAs = { evt = null ->
	    def fc = new JFileChooser(currentSaveDir)
	    fc.fileSelectionMode = JFileChooser.FILES_ONLY
	    fc.addChoosableFileFilter(new CustomFileFilter(extensions:['.jar'], description:'Scheme Packs (*.jar)'))
	    if (fc.showDialog(app.appFrames[0], "Save") == JFileChooser.APPROVE_OPTION) {
			currentSaveDir = fc.currentDirectory
			updateSchemeFile(fc.selectedFile)
			saveScheme()
	    }
    }

	/**
	 * Write scheme to file and pop success message
	 */
	def saveScheme() {
		helper.write([id:view.schemeId.text, name:view.schemeName.text, type:view.schemeType.selectedItem,
			entries:model.schemeEntries], model.schemeFile)
		JOptionPane.showMessageDialog(app.appFrames[0], "${view.schemeName.text} saved!",
			"Scheme Saved", JOptionPane.INFORMATION_MESSAGE)
		setSchemeDirty(false)
	}

	def setSchemeDirty(dirty) {
		model.schemeDirty = dirty
		updateTitle()
	}
	
	def exportPaginatedCatalog = { evt = null -> exportCatalog(true) }
	def exportOnePageCatalog = { evt = null -> exportCatalog(false) }
	
	/**
	 * Export the current scheme's entries as a PDF "catalog" 
	 */
	def exportCatalog(paginate) {
		def fc = new JFileChooser(currentSaveDir)
		fc.fileSelectionMode = JFileChooser.FILES_ONLY
		fc.selectedFile = new File("${view.schemeName.text}")
		fc.addChoosableFileFilter(new CustomFileFilter(extensions:['.pdf'], description:'PDF Files (*.pdf)'))
		if (fc.showDialog(app.appFrames[0], "Save Catalog File" ) == JFileChooser.APPROVE_OPTION) {
			currentSaveDir = fc.currentDirectory
			def destFile = (fc.selectedFile.name.lastIndexOf('.') == -1) ? new File(fc.selectedFile.absolutePath + ".pdf") : fc.selectedFile
			def isLithology = (view.schemeType.selectedItem == "lithology")
			helper.exportCatalog(paginate, destFile, model.schemeEntries, isLithology, view.schemeName.text, view.schemeId.text)
		}
	}
	
    def addEntry = { evt = null ->
    	def e = [name:'New Entry']
    	model.schemeEntries << e
    	setEntry(e)
		def row = model.schemeEntries.indexOf(model.entry)
		view.schemeEntries.selectionModel.setSelectionInterval(row, row)
    }
    
    def removeEntry = { evt = null ->
    	if (model?.entry) {
			def index = model.schemeEntries.indexOf(model?.entry)
			model.schemeEntries.remove(index)

			// select next entry
			def selectEntry = null
			if (model.schemeEntries.size() > 0) {
				def newIndex = (index < model.schemeEntries.size()) ? index : index - 1
				selectEntry = model.schemeEntries.get(newIndex)
			}
    		setEntry(selectEntry)
    	}
    }
    
    def setEntry(entry) {
    	model.ignoreEvents = true
    	if (entry) {
	    	model.entry = entry
	    	model.entryColor = entry?.color
	    	model.entryImage = entry?.image
	    } else {
	    	model.entry = null
	    	model.entryColor = null
	    	model.entryImage = null
	    }
    	updatePreview()
        model.ignoreEvents = false
    }
    
    def updateColor = { evt = null ->
    	def color = JColorChooser.showDialog(app.appFrames[0], "Choose Color", view.preview.color)
    	if (color) {
    		model.entryColor = "${color.red},${color.green},${color.blue}"
			model.entry.color = "${color.red},${color.green},${color.blue}"
    	}
    	schemeChanged()
		updatePreview()
    }
    
    def updateImage = { evt = null ->
    	final int option = JOptionPane.showConfirmDialog(app.appFrames[0], [ view.imageChooser ].toArray(), 
    			"Choose Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
		if (option == JOptionPane.OK_OPTION) {
			model.entryImage = view.standardImages.selectedValue?.image
			model.entry.image = view.standardImages.selectedValue?.image
		}
    	view.imageFilter.text = ""
		schemeChanged()
    	updatePreview()
    }
    
    def updatePreview = { evt = null ->
    	// handle our color
    	def color = null
    	if (model?.entryColor) { color = helper.parseColor(model.entryColor) }
    	view.preview.color = color
    	
    	// handle our image
    	view.preview.tileImage = (view.schemeType.selectedItem == "lithology")
    	if (model?.entryImage) {
    		doOutside {
    			def image = helper.parseImage(model.entryImage)
    			doLater {
    				view.preview.image = image
    				view.preview.repaint()
    			}
    		}
    	} else {
    		view.preview.image = null
    	}
    	view.preview.repaint()
    }
    
    def customImage = { evt = null ->
	    def fc = new JFileChooser(currentOpenDir)
	    fc.fileSelectionMode = JFileChooser.FILES_ONLY
	    fc.addChoosableFileFilter(new CustomFileFilter(extensions: helper.IMAGE_EXTENSIONS, description: 'Images'))
	    if ( fc.showDialog(app.appFrames[0], "Open" ) == JFileChooser.APPROVE_OPTION ) {
	    	currentOpenDir = fc.currentDirectory
	    	def f = fc.selectedFile
	    	def m = [name:f.name, image:f.toURL().toExternalForm(), icon:helper.iconify(f.toURL().toExternalForm())]
	    	model.standardImages << m
	    	doLater {
	    		view.standardImages.setSelectedValue(m, true)
	    	}
	    }
    }
    
	public void valueChanged(ListSelectionEvent e) {
		if (!e.isAdjusting) {
			int row = view.schemeEntries.selectedRow
			if (row == -1) {
				row = model.schemeEntries.indexOf(model.entry)
			}
			setEntry(model.schemeEntries[row])
			view.schemeEntries.selectionModel.setSelectionInterval(row, row)
			view.schemeEntries.scrollRectToVisible(new Rectangle(view.schemeEntries.getCellRect(row, 0, true)))
		}
	}
	
	public void listChanged(ListEvent listChanges) { schemeChanged() }
}

private class CustomFileFilter extends FileFilter {
	List extensions = []
	String description
	
	boolean accept(File file) {
		file.isDirectory() || extensions.any { file.name.toLowerCase().endsWith(it) }
	}
}