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
package psicat

import java.util.List

import org.andrill.coretools.model.Project

import ca.odell.glazedlists.BasicEventList

import groovy.beans.Bindable


class PSICATModel {
	@Bindable Project project = null

	List openDiagrams = []
    @Bindable Map activeDiagram = null
    DiagramState diagramState = new DiagramState()
	@Bindable boolean anyDirty = false
    
	@Bindable String status = "Welcome to PSICAT"
}

@Bindable class DiagramState {
	String name = ''
	boolean dirty = false
	boolean canRedo = false
	boolean canUndo = false
	boolean vertical = true
	String units = "m"
	def selection = null
	def commandStack = null
}