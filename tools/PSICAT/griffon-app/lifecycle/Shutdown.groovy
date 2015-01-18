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

import psicat.PSICATController
import psicat.util.Dialogs

// save our current directory
def prefs = Preferences.userNodeForPackage(PSICATController)
prefs.put('psicat.lastDir', Dialogs.currentDir.absolutePath)

// save PSICAT window dimensions
def mainViewDims = app.views.PSICAT.mainView.size
prefs.putDouble('psicat.mainViewWidth', mainViewDims.width)
prefs.putDouble('psicat.mainViewHeight', mainViewDims.height)
def mainViewLoc = app.views.PSICAT.mainView.location
prefs.putDouble('psicat.mainViewX', mainViewLoc.x)
prefs.putDouble('psicat.mainViewY', mainViewLoc.y)

// save open project
def project = app.models.PSICAT.project
if (project) {
	prefs.put('psicat.lastProjectName', project.name)
	prefs.put('psicat.lastProjectType', project.getClass().simpleName)
	prefs.put('psicat.lastProjectPath', project?.path?.toURI()?.toString())
}