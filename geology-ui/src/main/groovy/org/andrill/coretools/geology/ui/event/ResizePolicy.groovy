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

import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.model.edit.Command;
import org.andrill.coretools.model.edit.EditableProperty;
import org.andrill.coretools.scene.event.Feedback
import org.andrill.coretools.scene.event.EventPolicy.Type
import org.andrill.coretools.scene.event.DefaultFeedback
import org.andrill.coretools.scene.event.SceneEvent

/**
 * An event policy for resizing geology-related models.
 *
 * @author Josh Reed (jareed@andrill.org)
 */
class ResizePolicy extends GeologyPolicy {
	 
	Type getType() { Type.RESIZE }
		
	Feedback getFeedback(SceneEvent e, Object target) {
		int handle = getHandle(e, target)
		if (e.clickCount == 0) {
			new DefaultFeedback(Feedback.RESIZE_TYPE, null, handle, null, null)
		} else {
			def r = track.getModelBounds(target)
			switch (handle) {
				case Cursor.N_RESIZE_CURSOR: r.setFrame(r.x, Math.min(e.y, r.maxY), r.width, Math.abs(e.y - r.maxY)); break
				case Cursor.S_RESIZE_CURSOR: r.setFrame(r.x, Math.min(e.y, r.minY), r.width, Math.abs(e.y - r.minY)); break
				case Cursor.E_RESIZE_CURSOR: r.setFrame(Math.min(e.x, r.x), r.y, Math.abs(e.x - r.minX), r.height); break
				case Cursor.W_RESIZE_CURSOR: r.setFrame(Math.max(e.x, r.x), r.y, Math.abs(e.x - r.maxX), r.height); break
				default: return null
			}

			// if interval is invalid (base above top), no feedback rectangle
			final propname = (handle == Cursor.N_RESIZE_CURSOR) ? "top" : "base"
			if (!GeologyModel.validInterval(propname, round(phys(e.y)), target)) {
				return null
			}

			return new DefaultFeedback(Feedback.RESIZE_TYPE, null, handle, null, new RectangleFeedback(r))	
		}
	}

	Command getCommand(SceneEvent e, Object target) {
		int handle = getHandle(e, target)
		def p = getHandleProperty(handle, target)
		if (p) {
			if (handle == Cursor.N_RESIZE_CURSOR || handle == Cursor.S_RESIZE_CURSOR) {
				final depth = "${round(phys(e.y))}"
				if (!GeologyModel.validInterval(p.name, depth, target)) {
					return null
				}
				return p.getCommand("${depth} ${track.units}")
			} else {
				// TODO: need to scale x-values properly
				return p.getCommand("${phys(e.x)}")
			}
		} else {
			return null
		}
	}
}
