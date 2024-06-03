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

import org.andrill.coretools.model.DefaultProject
import org.andrill.coretools.model.Project

import java.io.File;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import psicat.util.*

class NewProjectWizardController {
    def model
    def view

    void mvcGroupInit(Map args) {
		// gather default lithology scheme options
		model.lithologySchemes = getDefaultSchemes('lithology')
	}

    def actions = [
	    'browse': { evt = null ->
	    	def file = Dialogs.showSaveDirectoryDialog("Select Root Project Directory", null, app.appFrames[0])
	    	if (file) { 
	    		model.filePath = file.absolutePath
	    		if (!model.name) { model.name = file.name }
	    	}
    	}
    ]

    def show() {
    	if (Dialogs.showCustomDialog("Create New Project", view.root, app.appFrames[0])) {
			def defLith = view.lithologyScheme.getSelectedItem()?.toString()
			if (defLith) {
				model.defaultSchemePaths.add(model.lithologySchemes[defLith])
			}

			// assume a single default file exists for each non-lithology scheme type
			['bedding', 'features', 'grainsize', 'texture'].each { schemeType ->
				def schemes = getDefaultSchemes(schemeType)
				if (schemes.size() > 0) {
					def iterator = schemes.keySet().iterator()
					while (iterator.hasNext()) {
						model.defaultSchemePaths.add(schemes[iterator.next()])
						break
					}
				}
			}
			
    		return createProject()
    	}
    }
	
	private def createProject() {
		if (model.file) {
			Project project = new DefaultProject(model.file)
			project.name = model.name ?: model.file.name
			// 6/3/2024 Outcrops are broken and no one has ever asked for them in PSICAT. Disabling.
			// project.origin = model.originTop ? 'top' : 'base'
			project.origin = model.originTop
			project.units = model.units
			project.fontSize = '11' // default to medium font size - no GUI for now
			project.saveConfiguration()
			return project	
		} else {
			throw new IllegalStateException('A directory is required to create a new project')
		}
	}
	
	private def getDefaultSchemes(type) {
		def schemes = [:]
		def res = new File("resources")
		if (res.exists() && res.isDirectory()) {
			res.listFiles().each { f ->
				if (f.name.endsWith(".jar")) {
					def schemeInfo = getSchemeInfo(f)
					if (schemeInfo && schemeInfo.type == type) {
						schemes[schemeInfo.name] = f.absolutePath
					}
				}
			}
		}
		println "found $schemes schemes of type $type"
		return schemes
	}
	
	// return Map of scheme name, id, and type
	private def getSchemeInfo(File f) {
		def schemeInfo = null
		try {
			JarFile jf = new JarFile(f);
			ZipEntry ze = jf.getEntry("scheme.xml");
			def stream = jf.getInputStream(ze);
			schemeInfo = read(stream, true);
		} catch (Exception e) {
			System.out.println("Couldn't get scheme info for ${f.absolutePath}");
		}
		return schemeInfo
	}

	// Read a scheme from a stream.
	// brg 5/9/2018 shamelessly stolen from coretools/tools/SchemeEditor/src/main/SchemeHelper.groovy
	private def read(stream, skipEntries=false) {
		def scheme = [ id:"", name:"", type:"", entries:[] ]
		
		def xml = new XmlSlurper()
		def root = xml.parse(stream)
		scheme.id = root?.@id?.toString()
		scheme.name = root?.@name?.toString()
		scheme.type = root?.@type?.toString()
		if (skipEntries) return scheme
		root.entry.each { e ->
			def props = [:]
			e.property.each { p ->
				if (p?.@name && p?.@value) {
					props[p.@name.toString()] = p.@value.toString()
				}
			}
			scheme.entries << props
		}
		return scheme
	}
}