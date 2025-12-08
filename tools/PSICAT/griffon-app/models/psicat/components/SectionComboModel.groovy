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
package psicat.components

import groovy.beans.Bindable

@Bindable class SectionComboModel {
	String labelText = "Section:"
	def project
	def sections = null // if non-null, list of section names to include
	boolean allSections = false
	boolean eachSection = true
	boolean selectSections = true
	String allSectionsText = "All Sections"
	String eachSectionText = "Each Section"
	String selectSectionsText = "Select Sections..."
	def selectedSections = []
}