/*
 * Copyright (c) Brian Grivna, 2014
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

// helper routines for project-local images and schemes   
 
package psicat.util

import org.andrill.coretools.ui.widget.swing.SwingWidgetSet
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeManager
import org.andrill.coretools.misc.scheme.XMLSchemeFactory
import org.andrill.coretools.Platform
import org.andrill.coretools.ResourceLoader

class ProjectLocal {
	static void copy(src, dest) {
		def ant = new AntBuilder()
		ant.copy(file:"$src.canonicalPath", tofile:"$dest.canonicalPath")
	}
	
	static File mkdir(baseUrl, dirName) {
		def dir = new File(new File(baseUrl.toURI()), dirName)
		if (!dir.exists())
			dir.mkdirs()
		return dir
	}
	
	static File copyImageFile(imageFile, projUrl) {
		def destDir = mkdir(projUrl, "images")
		def destFile = new File(destDir, imageFile.name)
		copy(imageFile, destFile)
		
		return destFile
	}
	
	// embrace the ugliness...
	static Scheme addScheme(schemeFile) {
		def loader = Platform.getService(ResourceLoader.class)
		// todo: copy scheme to project
		loader.addResource(schemeFile.toURI().toURL())
		
		// so gross. find matching filename...only way to identify Scheme we just loaded
		def fact = Platform.getService(XMLSchemeFactory.class)
		def scheme = null
		fact.getSchemes().find {
			if (it.getInput().name.equals(schemeFile.name)) {
				scheme = it
				return true
			}
		}
		return scheme
	}
	
	static void loadSchemes(schemeFiles) {
		def loader = Platform.getService(ResourceLoader.class)
		schemeFiles.each { loader.addResource(it.toURI().toURL()) }
		updateSchemeManager(schemeFiles)
	}
	
	static void unloadAllSchemes() {
		println "unloading all schemes:"
		def sm = Platform.getService(SchemeManager.class)
		sm.schemes.each {
			println it.name
			sm.unregisterScheme(it)
		}
	}
	
	static void unloadScheme(scheme) {
		println "unloading single scheme ${scheme.name}"
		def sm = Platform.getService(SchemeManager.class)
		sm.unregisterScheme(scheme)
		scheme.getInput().delete() // delete project-local file
		def widgetSet = Platform.getService(SwingWidgetSet.class)
		widgetSet.updateSchemes(sm)
	}
	
	static File copySchemeToProject(schemeFile, projUrl, prefix = null) {
		def schemeDir = mkdir(projUrl, "schemes")
		def filename = prefix ? prefix + "_" + schemeFile.name : schemeFile.name
		def projSchemeFile = new File(schemeDir, filename)
		copy(schemeFile, projSchemeFile)
		return projSchemeFile
	}
	
	static void copyDefaultSchemes(project) {
		def res = new File("resources")
		if (res.exists() && res.isDirectory()) {
			def prefix = project.name.replace(" ", "") 
			def schemeFiles = []
			res.eachFile {
				schemeFiles.add(copySchemeToProject(it, project.path, prefix))
			}
			loadSchemes(schemeFiles)
		}
	}
	
	static void updateSchemeManager(schemeFiles) {
		def widgetSet = Platform.getService(SwingWidgetSet.class)
		def sm = Platform.getService(SchemeManager.class)
		def fact = Platform.getService(XMLSchemeFactory.class)
		fact.getSchemes().each { 
			if (it.getInput() in schemeFiles)
				sm.registerScheme(it)
		}
		widgetSet.updateSchemes(sm)
	}
}