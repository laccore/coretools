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
package org.andrill.coretools.geology.ui.event

import java.awt.Cursor

import org.andrill.coretools.model.edit.Command;
import org.andrill.coretools.model.edit.CompositeCommand;
import org.andrill.coretools.model.edit.EditableProperty;
import org.andrill.coretools.scene.event.*
import org.andrill.coretools.scene.event.EventPolicy.Type
import org.andrill.coretools.geology.models.Length

/**
 * An event policy for moving geology-related models.
 *
 * @author Josh Reed (jareed@andrill.org)
 */
class MovePolicy extends GeologyPolicy {
	 
	Type getType() { Type.MOVE }
		
	Feedback getFeedback(SceneEvent e, Object target) {
		if (e.clickCount == 0) {
			new DefaultFeedback(Feedback.MOVE_TYPE, null, Cursor.MOVE_CURSOR, null, null)
		} else {
			def r = track.getModelBounds(target)
			r.setFrame(r.x + (e.x - e.dragX), r.y + (e.y - e.dragY), r.width, r.height)
			new DefaultFeedback(Feedback.MOVE_TYPE, null, Cursor.MOVE_CURSOR, null, new RectangleFeedback(r))	
		}
	}

	Command getCommand(SceneEvent e, Object target) {
		def dx = new Length(phys(e.x - e.dragX), track.units)
		def dy = new Length(round(phys(e.y)) - round(phys(e.dragY)), track.units)
		
		def commands = []
		buildCommand(Cursor.N_RESIZE_CURSOR, target, dy, commands)
		buildCommand(Cursor.S_RESIZE_CURSOR, target, dy, commands)
		buildCommand(Cursor.E_RESIZE_CURSOR, target, dx, commands)
		buildCommand(Cursor.W_RESIZE_CURSOR, target, dx, commands)
		if (commands) {
			return new CompositeCommand("Move", commands as Command[])
		} else {
			return null
		}
	}
	
	protected void buildCommand(handle, model, delta, commands) {
		def p = getHandleProperty(handle, model)
		if (p) {
			def val = (model."${p.name}" + delta) as String
			def c = p.getCommand(val as String)
			if (c) { commands << c }
		}
	}
} 
