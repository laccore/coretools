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
import java.lang.Math
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.edit.EditableProperty;
import org.andrill.coretools.scene.Track
import org.andrill.coretools.scene.event.EventPolicy
import org.andrill.coretools.scene.event.Feedback
import org.andrill.coretools.scene.event.DefaultFeedback

/**
 * An abstract base class for geology-related event policies.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
abstract class GeologyPolicy implements EventPolicy {
	Track track
	
	protected def round(value) {
		def min = calculateSpacing(10) / 4
		Math.round(value / min) * min
	}
	
	protected def calculateSpacing(pts) {
		def scale = track.scene.scalingFactor
		def multiplier = Math.pow(10, Math.floor(Math.log10(pts / scale))) as BigDecimal
		def hash
		if (1 * scale * multiplier > pts) {
			hash = 1 * multiplier
		} else if (2 * scale * multiplier > pts) {
			hash = 2 * multiplier
		} else if (5 * scale * multiplier > pts) {
			hash = 5 * multiplier
		} else {
			hash = 10 * multiplier
		}
		return hash
	}
	
	protected EditableProperty getHandleProperty(handle, model) {
		if (!model)	{ return null }
		
		// figure out our handle name
		String handleName
		switch(handle) {
			case Cursor.N_RESIZE_CURSOR: handleName = 'north'; break
			case Cursor.S_RESIZE_CURSOR: handleName = 'south'; break
			case Cursor.E_RESIZE_CURSOR: handleName = 'east'; break
			case Cursor.W_RESIZE_CURSOR: handleName = 'west'; break
		}
		
		// find the property with the specified handle name
		model?.getAdapter(EditableProperty[].class).find { it?.constraints?.handle == handleName }
	}
	
	protected int getHandle(e, model) {
		if (!model) { return 0 }
		
		// get our rectangle and event coordinates
		def r = track.getModelBounds(model)
		int x = e.dragX == -1 ? e.x : e.dragX
		int y = e.dragY == -1 ? e.y : e.dragY
		
		// figure out our handle name
		String handleName
		if (Math.abs(r.minY - y) <= 5) {
			handleName = 'north'
		} else if (Math.abs(r.maxY - y) <= 5) {
			handleName = 'south'
		} else if (Math.abs(r.maxX - x) <= 5) {
			handleName = 'east'
		} else if (Math.abs(r.minX - x) <= 5) {
			handleName = 'west'
		}
		
		// if a handle of handleName exists, return the cursor
		if (handleName && model?.getAdapter(EditableProperty[].class).find { it?.constraints?.handle == handleName }) {
			switch(handleName) {
				case 'north':	return Cursor.N_RESIZE_CURSOR
				case 'south':	return Cursor.S_RESIZE_CURSOR
				case 'east':	return Cursor.E_RESIZE_CURSOR
				case 'west':	return Cursor.W_RESIZE_CURSOR
			}
		} else {
			return 0
		}
	}
	
	protected pts(val)	{ track.pts(val, track.bounds) }
	protected phys(val)	{ track.phys(val, track.bounds) }
}
