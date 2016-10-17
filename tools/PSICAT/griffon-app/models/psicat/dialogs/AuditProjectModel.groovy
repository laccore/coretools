/*
 * Copyright (c) Brian Grivna, 2015.
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

	// brgtodo: overlapping intervals, non-abutting adjacent intervals, negative depths(?)
	
	@Bindable def auditResults = new DefaultListModel()
	@Bindable def undescribedSecs = true
	@Bindable def noIntervalSecs = true
	@Bindable def emptyUndescribedInts = true
	@Bindable def emptyUndescribedSyms = true
	@Bindable def zeroLengthInts = true 
	@Bindable def invertedInts = true
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