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

import java.io.File

import groovy.beans.Bindable

import psicat.util.Dialogs

@Bindable class ExportStratColumnWizardModel {
	def project = null
	def grainSizeScale = null
	@Bindable def metadataPath = null
	@Bindable def exportPath = null
	@Bindable def alternateGrainSizePath = null
	@Bindable boolean drawLegend = true
	@Bindable boolean drawSectionNames = true
	@Bindable boolean drawSymbols = true
	@Bindable boolean aggregateSymbols = true
	@Bindable boolean drawGrainSize = true
	@Bindable boolean drawGrainSizeLabels = true
	@Bindable boolean useProjectGrainSize = true
	@Bindable boolean drawDms = false
}