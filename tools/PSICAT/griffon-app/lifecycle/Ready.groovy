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
import javax.swing.JOptionPane
import javax.swing.JCheckBox

import com.brsanthu.googleanalytics.*

import org.andrill.coretools.model.DefaultProject

import psicat.PSICATController


app.controllers.PSICAT.actions.versionCheckSilent()

// load or create unique user ID
def prefs = Preferences.userNodeForPackage(PSICATController)
def uuid = prefs.get('psicat.uuid', null)
if (!uuid) {
	uuid = UUID.randomUUID().toString()
	prefs.put('psicat.uuid', uuid)
}

// track launch
GoogleAnalytics ga = new GoogleAnalytics("UA-64269312-1")
ga.post(new PageViewHit("http://www.laccore.org", "launch: UUID=$uuid"))

// prompt to re-open last project
if (prefs.getBoolean('psicat.promptReopenLastProject', true)) {
	String name = prefs.get('psicat.lastProjectName', null)
	String path = prefs.get('psicat.lastProjectPath', null)
	if (name && path) {
		File projectFile = new File(new URL(path).toURI())
		if (projectFile.exists()) {
			JCheckBox prompt = new JCheckBox('Never prompt to re-open projects');
			if (JOptionPane.showOptionDialog(app.appFrames[0], ["Re-open last project: '$name'\n\n", prompt] as Object[], "Re-open '$name'", 
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null) == JOptionPane.YES_OPTION) {
				app.controllers.PSICAT.openProject(new DefaultProject(projectFile))	
			}
			if (prompt.isSelected()) {
				prefs.putBoolean('psicat.promptReopenLastProject', false)
			}
		}
	}
}