/*
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

import java.io.File;

import groovy.beans.Bindable

import psicat.util.Dialogs

class ExportStratModel {
	String title = "Export Stratigraphic Column"
	def project
	def scene
	
	@Bindable boolean standardFormat = true
	@Bindable String start
	@Bindable String end
	@Bindable String pageSize
	@Bindable boolean renderHeader = true
	@Bindable boolean renderFooter = false
	@Bindable boolean renderColumnBorders = true
	@Bindable boolean renderIntervalOutlines = true
	@Bindable String units = "m"
	@Bindable String filePath = Dialogs.currentSaveDir.absolutePath
	@Bindable String prefix = ''
	@Bindable String diagramColumns
}