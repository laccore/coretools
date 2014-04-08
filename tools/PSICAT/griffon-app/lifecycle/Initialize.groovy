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
import java.util.prefs.Preferences

import groovy.swing.SwingBuilder
import griffon.util.GriffonPlatformHelper

import org.andrill.coretools.Platform
import org.andrill.coretools.ResourceLoader

import psicat.PSICATController
import psicat.util.Dialogs

// set our look and feel
GriffonPlatformHelper.tweakForNativePlatform(app)
SwingBuilder.lookAndFeel('mac', 'nimbus', 'gtk', ['metal', [boldFonts: false]])

// initialize the coretools platform
Platform.start()

// brg 4/6/2014: Schemes are now loaded per-project
// add resources from the resource/ directory
//def loader = Platform.getService(ResourceLoader.class)
//def res = new File("resources")
//if (res.exists() && res.isDirectory()) {
//	res.eachFile { loader.addResource(it.toURL()) }	
//}

// restore the last directory
def lastDir = new File(Preferences.userNodeForPackage(PSICATController).get('psicat.lastDir', System.getProperty("user.home")))
if (lastDir.exists() && lastDir.isDirectory()) {
	Dialogs.currentDir = lastDir
} else {
	Dialogs.currentDir = new File(System.getProperty("user.home"))
}