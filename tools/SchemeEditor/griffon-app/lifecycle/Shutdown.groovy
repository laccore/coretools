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

def prefs = Preferences.userNodeForPackage(SchemeEditorController)

// save window size and position
def mainViewDims = app.views.SchemeEditor.mainView.size
prefs.putDouble('schemeEditor.mainViewWidth', mainViewDims.width)
prefs.putDouble('schemeEditor.mainViewHeight', mainViewDims.height)
def mainViewLoc = app.views.SchemeEditor.mainView.location
prefs.putDouble('schemeEditor.mainViewX', mainViewLoc.x)
prefs.putDouble('schemeEditor.mainViewY', mainViewLoc.y)

// save last open and save directory
prefs.put('schemeEditor.lastOpenDir', app.controllers.SchemeEditor.currentOpenDir.absolutePath)
prefs.put('schemeEditor.lastSaveDir', app.controllers.SchemeEditor.currentSaveDir.absolutePath)
