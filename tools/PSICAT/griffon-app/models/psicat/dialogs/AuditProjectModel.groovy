/*
 * Copyright (c) CSD Facility, 2015.
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

import javax.swing.DefaultListModel

import groovy.beans.Bindable

class AuditProjectModel {
	def project = null

	@Bindable def auditResults = new DefaultListModel()
	def modelTypes = []
}

class AuditResult {
	public AuditResult(section, issues) {
		this.section = section
		this.issues = issues
	}
	
	def section
	def issues
	
	public String toString() { return "$section: $issues" }
}