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
/*
 * This script is executed inside the EDT, so be sure to
 * call long running code in another thread.
 *
 * You have the following options
 * - SwingBuilder.doOutside { // your code  }
 * - Thread.start { // your code }
 * - SwingXBuilder.withWorker( start: true ) {
 *      onInit { // initialization (optional, runs in current thread) }
 *      work { // your code }
 *      onDone { // finish (runs inside EDT) }
 *   }
 *
 * You have the following options to run code again inside EDT
 * - SwingBuilder.doLater { // your code }
 * - SwingBuilder.edt { // your code }
 * - SwingUtilities.invokeLater { // your code }
 */

import java.util.prefs.Preferences
  
def lastDir = new File(Preferences.userNodeForPackage(SchemeEditorController).get('schemeEditor.lastDir', System.getProperty("user.home")))
if (lastDir.exists() && lastDir.isDirectory()) {
    app.controllers.SchemeEditor.currentDir = lastDir
}