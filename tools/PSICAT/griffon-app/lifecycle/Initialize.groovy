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

import java.awt.Font
import java.util.prefs.Preferences
import javax.swing.UIManager

import groovy.swing.SwingBuilder

import org.andrill.coretools.Platform
import org.andrill.coretools.ResourceLoader

import psicat.PSICATController
import psicat.util.Dialogs

// set our look and feel
//<<<<<<< HEAD
//SwingBuilder.lookAndFeel('mac', 'nimbus', 'gtk', ['metal', [boldFonts: false]])
//=======
//GriffonPlatformHelper.tweakForNativePlatform(app)
SwingBuilder.lookAndFeel('system')
//>>>>>>> master

// initialize the coretools platform
Platform.start()

import griffon.util.Environment

println "### Current Environment = ${Environment.current} ###"

SplashGriffonAddon.display(app)

def getDirPref(prefKey) {
	def dir = new File(Preferences.userNodeForPackage(PSICATController).get(prefKey, System.getProperty("user.home")))
	return (dir.exists() && dir.isDirectory()) ? dir : new File(System.getProperty("user.home"))
}

// restore the last open and save directories
Dialogs.currentOpenDir = getDirPref('psicat.lastOpenDir')
Dialogs.currentSaveDir = getDirPref('psicat.lastSaveDir')

// on Windows, JTextArea uses fixed-width font, unlike all other controls - force it to match
UIManager.getDefaults().put("TextArea.font", UIManager.getFont("TextField.font"))
