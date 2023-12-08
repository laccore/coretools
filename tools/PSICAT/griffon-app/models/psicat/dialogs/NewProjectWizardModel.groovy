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

import groovy.beans.Bindable

import psicat.util.Dialogs

@Bindable class NewProjectWizardModel {
	String name
	String filePath
	boolean originTop = true
	String units = 'cm'
	boolean importSections = false
	boolean useCustomSchemes = false
	Map lithologySchemes = [:]
	Map symbolSchemes = [:]
	List defaultSchemePaths = []

	File getFile() {
		if (!filePath) return null
		filePath.contains(File.separator) ? new File(filePath) : new File(Dialogs.currentSaveDir, filePath)
	}
}