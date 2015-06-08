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
import java.io.File;
import java.util.zip.ZipFile
import javax.swing.ImageIcon
import javax.swing.JColorChooser
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.filechooser.FileFilter

/**
 * The SchemeEditor controller.
 */
class SchemeEditorController implements ListSelectionListener {
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
	    if (fc.showOpenDialog(app.windowManager.windows[0]) == JFileChooser.APPROVE_OPTION) {
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
	    		   JOptionPane.showMessageDialog(app.windowManager.windows[0], "Not a valid Scheme Pack file", 
	    				   "Invalid Scheme Pack", JOptionPane.ERROR_MESSAGE)
	    	   }
	       }
	    }
    }
    
    /**
     * Called when the scheme id, name, type, and entries are changed.
     */
    def schemeChanged = { evt = null -> 
		model.schemeValid = (view.schemeId.text && view.schemeName.text && view.schemeType.selectedItem)
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
    	}
    }
    
	/**
	 * Update model.schemeFile reference and mainView's title bar
	 */
	def updateSchemeFile(file) {
		model.schemeFile = file
		def baseTitle = "Scheme Editor ${app.applicationProperties['app.version']}"
		def fileName = file
		view.mainView.title = baseTitle + (fileName ? " - [$fileName]" : "") 
	}
	
    /**
     * Save the scheme.
     */
    def save = { evt = null ->
	    if (model.schemeFile == null) {
	    	saveAs(evt)
	    } else {
		    helper.write([id:view.schemeId.text, name:view.schemeName.text, type:view.schemeType.selectedItem, 
		                        entries:model.schemeEntries], model.schemeFile)
		    JOptionPane.showMessageDialog(app.windowManager.windows[0], "${view.schemeName.text} saved!", 
	    				   "Scheme Saved", JOptionPane.INFORMATION_MESSAGE)
	    }
    }
    
    /**
     * Save the scheme, prompting the user for a filename.
     */
    def saveAs = { evt = null ->
	    def fc = new JFileChooser(currentSaveDir)
	    fc.fileSelectionMode = JFileChooser.FILES_ONLY
	    fc.addChoosableFileFilter(new CustomFileFilter(extensions:['.jar'], description:'Scheme Packs (*.jar)'))
	    if (fc.showDialog(app.windowManager.windows[0], "Save") == JFileChooser.APPROVE_OPTION) {
			currentSaveDir = fc.currentDirectory
			updateSchemeFile(fc.selectedFile)
			helper.write([id:view.schemeId.text, name:view.schemeName.text, type:view.schemeType.selectedItem,
				entries:model.schemeEntries], model.schemeFile)
			JOptionPane.showMessageDialog(app.windowManager.windows[0], "${view.schemeName.text} saved!", 
				"Scheme Saved", JOptionPane.INFORMATION_MESSAGE)
	    }
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
    
	// If empty, populate Code field based on Name. Don't force user to devise and type a code!
	def fillCode = { evt = null ->
   		if (view.entryCode.text.length() == 0) {
			def code = view.entryName.text.toLowerCase()
			code = code.replace(" ", ",")
			view.entryCode.text = code
   		}
	}
	
    def entryChanged = { evt = null ->
		if (!model.ignoreEvents) {
			model.entryDirty = true
    		model.entryValid = (view.entryName.text && view.entryCode.text)
		}
	}
    
    def addEntry = { evt = null ->
    	def e = [name:'New Entry']
    	model.schemeEntries << e
    	setEntry(e)
		view.entryName.requestFocus()
		view.entryName.selectAll() // Windows: must explicitly select text
    }
    
    def saveEntry = { evt = null ->
    	model.ignoreEvents = true
    	model.entry.name = view.entryName.text
    	model.entry.code = view.entryCode.text
    	model.entry.group = view.entryGroup.text
    	model.entry.color = model.entryColor
    	model.entry.image = model.entryImage
		
		// brg 6/10/2014: SortedList doesn't resort when elements already present in the list are
		// updated. Must explicitly get old entry and reset it to force resort.
		def idx = view.schemeEntries.selectedIndex
		def newEntry = model.schemeEntries.get(idx)
		model.schemeEntries.set(idx, newEntry)
		view.schemeEntries.selectedIndex = model.schemeEntries.indexOf(newEntry)
		view.schemeEntries.ensureIndexIsVisible(view.schemeEntries.selectedIndex)		
    	view.schemeEntries.repaint()
		
    	model.entryDirty = false
    	model.entryValid = (view.entryName.text && view.entryCode.text)
    	model.ignoreEvents = false;
    }
	
	def saveAndAddEntry = { evt = null ->
		saveEntry()
		addEntry()
	}
    
    def revertEntry = { evt = null ->
    	setEntry(model?.entry)
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
	    	view.entryName.text = entry?.name
	    	view.entryCode.text = entry?.code
	    	view.entryGroup.text = entry?.group
	    	model.entryColor = entry?.color
	    	model.entryImage = entry?.image
	    	if (view.schemeEntries.selectedValue != entry) {
	    		view.schemeEntries.setSelectedValue(entry, true)
	    	}
	    } else {
	    	model.entry = null
	    	view.entryName.text = ""
	    	view.entryCode.text = ""
	    	view.entryGroup.text = ""
	    	model.entryColor = null
	    	model.entryImage = null
	    }
    	updatePreview()
    	model.entryDirty = false
        model.entryValid = (view.entryName.text && view.entryCode.text)
        model.ignoreEvents = false
    }
    
    def updateColor = { evt = null ->
    	def color = JColorChooser.showDialog(app.windowManager.windows[0], "Choose Color", view.preview.color)
    	if (color) {
    		model.entryColor = "${color.red},${color.green},${color.blue}"
    	}
    	entryChanged()
    	updatePreview()
    }
    
    def updateImage = { evt = null ->
    	final int option = JOptionPane.showConfirmDialog(app.windowManager.windows[0], [ view.imageChooser ].toArray(), 
    			"Choose Image", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
		if (option == JOptionPane.OK_OPTION) {
			model.entryImage = view.standardImages.selectedValue?.image
		}
    	view.imageFilter.text = ""
    	entryChanged()
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
	    if ( fc.showDialog(app.windowManager.windows[0], "Open" ) == JFileChooser.APPROVE_OPTION ) {
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
    	setEntry(view.schemeEntries.selectedValue)
	}
}

class CustomFileFilter extends FileFilter {
	List extensions = []
	String description
	
	boolean accept(File file) {
		file.isDirectory() || extensions.any { file.name.toLowerCase().endsWith(it) }
	}
}