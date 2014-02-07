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
package org.andrill.coretools.geology

import org.andrill.coretools.model.Model;
import org.andrill.coretools.geology.models.Length;
import org.andrill.coretools.model.edit.Command;
import org.andrill.coretools.model.edit.CompositeCommand;
import org.andrill.coretools.model.edit.EditableProperty;
/**
 * A Groovy implementation of the EditableProperty interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class GProperty implements EditableProperty {
	def source
	String name
	String widgetType
	Map<String,String> widgetProperties = [:]
	Map<String,String> constraints = [:]
	def validators = []
	Command command
	
	String getValue() {
		source."$name" as String
	}
	
	boolean isValid(String newValue) {
		try { 
			return validators.inject(true) { prev, cur -> prev && cur.call([newValue, source]) } 
		} catch (e) {
			return false
		}
	}
	
	Command getCommand(String newValue) {
		if (constraints?.linkTo && source instanceof Model) {
			def value = source."$name"
			def links = source.container.models.findAll { it.class == source.class && it?."${constraints.linkTo}" == value }
			if (links) {
				def commands = []
				commands << new GCommand(source: source, prop: name, value: newValue)
				links.each { commands << new GCommand(source: it, prop: constraints.linkTo, value: newValue) }
				return new CompositeCommand("Change $name", (commands as Command[]))
			} else {
				return new GCommand(source: source, prop: name, value: newValue)
			}
		} else {
			return new GCommand(source: source, prop: name, value: newValue)
		}
	}
}
