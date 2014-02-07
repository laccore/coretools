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

import org.andrill.coretools.model.edit.AbstractCommand;

/**
 * A Groovy implementation of the Command interface to change a property value on an object.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class GCommand extends AbstractCommand {
	def source
	def prop
	def value
	def old
	String label = "Change $prop"
	
	public void executeCommand() {
		old = source."$prop"
		source."$prop" = value
		if (source."$prop" != old) {
			source.updated()
		}
	}
	
	public void undoCommand(){
		source."$prop" = old
		source.updated()
	}
}
