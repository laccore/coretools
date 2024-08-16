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

import java.util.Map
import javax.swing.JTextArea

import org.apache.log4j.AppenderSkeleton
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import org.apache.log4j.spi.LoggingEvent

import org.andrill.coretools.Platform
import org.andrill.coretools.geology.io.GeologyExcelReader
import org.andrill.coretools.geology.models.Image

import psicat.util.*

class TextAreaAppender extends AppenderSkeleton {
	private JTextArea area = null
	private Level minLevel = Level.WARN
	public TextAreaAppender(JTextArea area) {
		this.area = area
		layout = new PatternLayout("%m%n")
	}
	public void close() {}
	public boolean requiresLayout() { return true }
	protected void append(LoggingEvent event) {
		if (event.getLevel().isGreaterOrEqual(minLevel)) {
			def str = this.layout.format(event)
			area.append(str)
		}
	}
	public setLevel(level) { this.minLevel = level }
}

class ImportTabularWizardController {
    def model
    def view

	private Logger logger = Logger.getLogger(ImportTabularWizardController.class)
	private appender = null
	
    void mvcGroupInit(Map args) {
		appender = new TextAreaAppender(view.logArea)
		logger.addAppender(appender)
	}

    def actions = [
	    browse: { evt = null ->
	    	def file = Dialogs.showOpenDialog('Select Tabular Data', new CustomFileFilter(extensions:['.xls'], description:'Excel Workbooks (*.xls)'), view.root)
	    	if (file) { model.filePath = file.absolutePath }
    	},
		doImport: { evt = null ->
			doOutside {
				view.importBtn.enabled = false
				importTabular()
				view.importBtn.enabled = true 
			}
		}
    ]

    def show() {
    	Dialogs.showCustomOneButtonDialog("Import Tabular", view.root, app.appFrames[0])
		return ''
    }
	
	private def updateProgress(str, value) {
		if (str) { view.progress.string = str }
		if (value) { view.progress.value = value }
	}
	
	private def importTabular() {
		if (model.file) {
			def startTime = System.currentTimeMillis()
			view.logArea.setText('Import started...\n')
			updateProgress("Reading Tabular Data...", -1)
			def containerMap = model.file.withInputStream { stream -> (Platform.getService(GeologyExcelReader.class)).read(stream, logger) }
			def sectionCount = createSections(containerMap)
			if (model.copyImages) {
				copyImages(containerMap)
			}
			updateProgress("Import complete", 100)
			
			def elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0
			view.logArea.append("Import complete: created $sectionCount sections, elapsed time ${elapsedTime}s")
		} else {
			Dialogs.showErrorDialog("Import Error", "An import file must be selected")
		}
	}
	
	private def createSections(containerMap) {
		def createCount = 0
		def mapSize = containerMap.size()
		containerMap.eachWithIndex { sectionID, modelList, index ->
			updateProgress("Creating sections ($index/$mapSize)", (index/mapSize * 100).intValue())
			if (!model.project.containers.contains(sectionID)) {
				def container = null
				try {
					// brg 9/9/2015: if create (and save below) not performed in EDT, it appears the project's model
					// and view (section list) get out of sync, causing all manner of problems
					edt { container = model.project.createContainer(sectionID) }
				} catch (Exception e) {
					logger.error("ERROR: Container $sectionID already exists, skipping")
				}
				
				if (container) {
					container.addAll(modelList)
					edt { model.project.saveContainer(container) } // see brg 9/9/2015
					createCount++
					logger.warn("Saved new section $sectionID")
				}
			} else {
				logger.warn("Container $sectionID already exists, skipping")
			}
		}
		return createCount
	}
	
	private def copyImages(containerMap) {
		def imageModels = []
		containerMap.each { sectionID, modelList ->
			imageModels.addAll(modelList.findAll { it instanceof Image })
		}
		def imageCount = imageModels.size()
		logger.warn("found $imageCount Images to copy")
		
		imageModels.eachWithIndex { image, index ->
			updateProgress("Copying images ($index/$imageCount)", (index/imageCount * 100).intValue())
			def imageFile = null
			try {
				imageFile = new File(image.path.toURI())
			} catch (Exception e) {
				logger.warn("Couldn't get File for ${image.path}: $e")
			}
			if (imageFile && imageFile.exists()) {
				ProjectLocal.copyImageFile(imageFile, model.project.path)
				logger.warn("Copying ${image.path} to project")
			} else {
				logger.warn("Image file ${image.path} does not exist, skipping")
			}
		}
	}
}