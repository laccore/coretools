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

import groovy.lang.Closure;

import java.awt.Image
import java.awt.Rectangle
import java.io.File;
import java.util.Map;
import java.util.zip.ZipFile

import javax.swing.DefaultComboBoxModel
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
				m.custom = false
    		}
    	}
    }
	
	// for working with MVC groups
	def withMVC(Map params = [:], String type, Closure closure) {
		def result
		try {
			result = closure(buildMVCGroup(params, type, type))
		} catch (e) {
			e.printStackTrace()
			//Dialogs.showErrorDialog('Error', e.message)
		} finally {
			destroyMVCGroup(type)
		}
		return result
	}
	
	def getMVC(String id) {
		[model: app.models[id], view: app.views[id], controller: app.controllers[id]]
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
		setEntry(null)
		updatePreview()
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
				addCustomImages(scheme)
	    	} else {
	    		view.schemeId.text = ""
	    		view.schemeName.text = ""
	    		view.schemeType.selectedItem = null
	    		view.schemeEntries.clearSelection()
	    		model.schemeEntries.clear()
				removeCustomImages()
	    	}
			setSchemeDirty(false)
    	}
    }
	
	// add/remove non-standard images from loaded/closed scheme to/from imageChooser 
	def pathToFile(String path) { 
		return path != null ? path.substring(path.lastIndexOf("/") + 1) : ""
	}
	def addCustomImages(scheme) {
		def stdImageNames = model.standardImages.collect { pathToFile(it.image) }
		def customImages = scheme.entries.findAll() { pathToFile(it.image).endsWith("png") && !(pathToFile(it.image) in stdImageNames) }.unique() { it.image }
		customImages.each {
			model.standardImages << [name:pathToFile(it.image), image:it.image, icon:helper.iconify(it.image), custom:true]
		}
	}
	def removeCustomImages() {
		def culledCustom = model.standardImages.findAll() { !(it.custom) }
		model.standardImages.clear()
		model.standardImages.addAll(culledCustom)
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
			def subversion = app.applicationProperties['app.subversion'] ? " ${app.applicationProperties['app.subversion']}" : ""
			def baseTitle = "Scheme Editor ${app.applicationProperties['app.version']}" + "$subversion"
			def fileName = model.schemeFile?.name
			def newTitle = baseTitle + (fileName ? " - [$fileName]" : " - [New Untitled Scheme]") + (model.schemeDirty ? "*" : "")
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
		try {
			helper.write([id:view.schemeId.text, name:view.schemeName.text, type:view.schemeType.selectedItem,
				entries:model.schemeEntries], model.schemeFile)
		} catch (e) {
			JOptionPane.showMessageDialog(app.appFrames[0], "${model.schemeFile} could not be saved: ${e.message}",
				"Error Saving Scheme", JOptionPane.ERROR_MESSAGE)
			return
		}
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
		if (view.schemeEntries.isEditing()) {
			view.schemeEntries.cellEditor?.stopCellEditing()
		}
		model.schemeEntries.getReadWriteLock().writeLock().lock()

		try {
			def entryName = JOptionPane.showInputDialog(app.appFrames[0], "New Entry Name:")
			if (entryName) {
				def e = [name:entryName, code:SchemeHelper.createUniqueCode(entryName, model.schemeEntries)]
				model.schemeEntries << e
				setEntry(e)
				def row = model.schemeEntries.indexOf(model.entry)
				view.schemeEntries.selectionModel.setSelectionInterval(row, row)
			}
    	} finally {
			model.schemeEntries.getReadWriteLock().writeLock().unlock()
		}
    }
    
    def removeEntry = { evt = null ->
		if (view.schemeEntries.isEditing()) {
			view.schemeEntries.cellEditor?.stopCellEditing()
		}
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
		if (entry) {
			def index = model.schemeEntries.indexOf(entry)
			
			// Handle odd case in which an existing scheme has 2+ entries that are exactly
			// the same (it is no longer possible to create such schemes, as code uniqueness is
			// now enforced). When the second (per current sorting) entry is selected in this case,
			// the matching index is that of the first entry - we try setting the selection to the
			// first, which triggers another setEntry() call and we loop until crashing (out of heap
			// memory because we keep trying to load the image to preview). If we find multiple matches
			// don't update selection! This should prevent the crash.
			def matchingEntries = model.schemeEntries.findAll { it == entry }
			if (matchingEntries.size() > 1) return;
			
			view.schemeEntries.setRowSelectionInterval(index, index)
		}
    }
    
    def updateColor = { evt = null ->
		withMVC('ColorChooser', entries:model.schemeEntries, selectedColor:model.entry?.color) { mvc ->
			def color = mvc.controller.show()
			if (color) {
				model.entryColor = color.toString()
				model.entry.color = color.toString()
				schemeChanged()
				updatePreview()
			}
		}
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
	    	def m = [name:f.name, image:f.toURL().toExternalForm(), icon:helper.iconify(f.toURL().toExternalForm()), custom:true]
	    	model.standardImages << m
	    	doLater {
	    		view.standardImages.setSelectedValue(m, true)
	    	}
	    }
    }
    
	public void valueChanged(ListSelectionEvent e) {
		if (!e.isAdjusting) {
			int row = view.schemeEntries.selectedRow
			if (row == -1 && model.entry) {
				row = model.schemeEntries.indexOf(model.entry)
			}
			if (row != -1) {
				setEntry(model.schemeEntries[row])
				view.schemeEntries.selectionModel.setSelectionInterval(row, row)
				view.schemeEntries.scrollRectToVisible(new Rectangle(view.schemeEntries.getCellRect(row, 0, true)))
			}
		}
	}
	
	public void listChanged(ListEvent listChanges) { schemeChanged() }
	
	public void schemeNameLostFocus(event) {
		// use scheme name to auto-populate scheme ID field if it's empty
		if (view.schemeName.text.length() > 0 && view.schemeId.text.length() == 0) {
			def sid = view.schemeName.text.toLowerCase()
			sid = sid.replaceAll(/[ -]{1,}/, ".") // convert spaces and hyphens to dots
			sid = sid.replaceAll(/[^a-z0-9.]/, "") // remove non-alphanumerics except for dots
			view.schemeId.text = sid
		}
	}
}

private class CustomFileFilter extends FileFilter {
	List extensions = []
	String description
	
	boolean accept(File file) {
		file.isDirectory() || extensions.any { file.name.toLowerCase().endsWith(it) }
	}
}