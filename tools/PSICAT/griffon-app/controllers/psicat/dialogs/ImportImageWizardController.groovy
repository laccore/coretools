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
package psicat.dialogs;

import java.util.Map

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.SortedList
import ca.odell.glazedlists.gui.WritableTableFormat
import ca.odell.glazedlists.swing.EventTableModel

import org.andrill.coretools.geology.models.Image
import org.andrill.coretools.geology.models.Section;
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.graphics.util.ImageInfo

import psicat.util.Dialogs
import psicat.util.CustomFileFilter

class ImportImageWizardController {
	def model
    def view
    def builder

    void mvcGroupInit(Map args) {}

    def actions = [
	    browse: { evt = null ->
	    	def file = Dialogs.showOpenDirectoryDialog("Image Directory", null, app.windowManager.windows[0])
	    	if (file) { model.filePath = file.absolutePath }
    	}
    ]

    def show() {
    	if (Dialogs.showCustomDialog("Import Images", view.root, app.windowManager.windows[0])) {
			// find our images
			def images = findImages()
			if (images.size() == 0) {
				throw new RuntimeException('No images found')
			}

			// pop up table for the user to confirm
			def columns = ["File", "Name", "Top", "Base", "Group"]
			view.table.model = new EventTableModel(images, [
                    getColumnCount:	{ columns.size() },  
                    getColumnName:	{ index -> columns[index] },  
                    getColumnValue: { object, index -> object."${columns[index].toLowerCase()}" },
                    isEditable:		{ object, index -> index != 0 },
                    setColumnValue: { object, value, index -> object."${columns[index].toLowerCase()}" = value; return object }
                ] as WritableTableFormat)
			if (Dialogs.showCustomDialog("Imported Images", view.tablePanel, app.windowManager.windows[0])) {
				return model.createSections ? createSections(images) : createImages(images, view.section.selectedItem)
			}
		}
    }
	
	private def createImages(images, section) {
		def container = model.project.openContainer(section)
		images.each { addImage(it, container) }
		model.project.saveContainer(container)
		return "Imported ${images.size()} images into section '${section}'"
	}
	
	private def createSections(images) {
		images.each { image ->
			def container = model.project.createContainer(image.name)
			// add our image
			addImage(image, container)

			// create a section as well
			def i = container.models[0]
			def s = new Section()
			s.top = i.top
			s.base = i.base
			s.name = image.name
			container.add(s)
			
			model.project.saveContainer(container)
		}
		return "Imported ${images.size()} images as new sections"
	}
	
	private def findImages() {
		if (model.file) {
			double depth = model.parseTop ? -1.0 : model.top as Double
			int dpi = model.parseBase ? -1 : model.dpi as Integer
			
			// find all images
			def images = new SortedList(new BasicEventList(), {a, b -> a?.top <=> b?.top} as Comparator)
			def regex = ~/([0-9]*\.[0-9]+)/
			model.file.eachFileMatch({it[it.lastIndexOf('.')..-1].toLowerCase() in CustomFileFilter.IMAGES.extensions}) { file ->
				file.withInputStream { stream -> 
					ImageInfo ii = new ImageInfo()
					ii.setInput(stream)
					if (ii.check()) {
						// create an image object
						def image = [:]
						image.path = file.toURI().toURL()
						image.file = file
						image.group = model.group
						image.name = file.name.contains('.') ? file.name[0..<file.name.lastIndexOf('.')] : file.name
						
						// figure out top and base
						def match = regex.matcher(file.name)
						if (depth < 0) {
							image.top = (match.find() ? match.group(0) as Double : 0.0)
						} else {
							image.top = depth
						}
						
						if (dpi < 0) {
							if (match.find()) {
								image.base = match.group(0) as Double
							} else {
								def length = new Length(ii.physicalHeightInch, "in").to("m")
								image.base = image.top + length.value
							}
						} else {
							def length = new Length(ii.height / dpi, "in").to("m")
							image.base = image.top + length.value
						}
						if (depth >= 0) { depth = image.base }
						images << image
					}
				}
			}
			return images
		} else {
			throw new IllegalStateException('No directory specified')
		}
	}
	
	private File copyImageFile(image, container) {
		def ant = new AntBuilder()
		def projDir = new File(model.project.path.toURI())
		def destDir = new File(projDir, "images")
		if (!destDir.exists())
			destDir.mkdirs()
			def destFile = new File(destDir, image.file.name)
		ant.copy(file:"$image.file.canonicalPath", tofile:"$destFile.canonicalPath")
		
		return destFile
	}

    private void addImage(image, container) {
    	boolean isTopOrigin = (model.project.configuration['origin'] ?: 'top') == 'top'
    	def min = Math.min(image.top as Double, image.base as Double)
    	def max = Math.max(image.top as Double, image.base as Double)
    	
		Image model = new Image()
    	model.path = copyImageFile(image, container).toURI().toURL()
    	model.top =  isTopOrigin ? "$min m" : "$max m" 
    	model.base = isTopOrigin ? "$max m" : "$min m" 
    	model.group = image.group
    	container.add(model)
    }
}