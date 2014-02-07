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

import org.andrill.coretools.model.Project
import org.andrill.coretools.model.edit.CommandStack
import org.andrill.coretools.scene.Scene
import org.andrill.coretools.scene.Selection

import groovy.beans.Bindable

class DiagramModel {
	String id
	Project project
	@Bindable String name = 'untitled'
	@Bindable Scene scene = null
	@Bindable boolean dirty = false
	CommandStack commandStack
	def diagramState = null
	@Bindable boolean vertical = false
	@Bindable String units = "m"
}