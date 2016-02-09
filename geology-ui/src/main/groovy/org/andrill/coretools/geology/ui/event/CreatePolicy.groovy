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
import java.awt.Rectangle

import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.edit.Command;
import org.andrill.coretools.model.edit.CreateCommand;
import org.andrill.coretools.scene.Scene.Origin
import org.andrill.coretools.scene.event.Feedback
import org.andrill.coretools.scene.event.SceneEvent
import org.andrill.coretools.scene.event.DefaultFeedback
import org.andrill.coretools.scene.event.EventPolicy.Type

/**
 * An event policy for creating geology-related models.
 *
 * @author Josh Reed (jareed@andrill.org)
 */
class CreatePolicy extends GeologyPolicy {
	Class clazz
	Map template
	boolean linked = false
	int width

	CreatePolicy(clazz, template = [:], width = -1) {
		this.clazz = clazz
		this.template = template
		linked = (clazz?.constraints?.top?.linkTo || clazz?.constraints?.base?.linkTo)
		this.width = width
	}
		
	Type getType() { Type.CREATE }
		
	Feedback getFeedback(SceneEvent e, Object target) {
		int val1, val2
		if (linked) {
			def models = track.models
			val1 = pts(models.size() > 0 ? track.mmax(models.last()) : track.scene.contentSize.minY / track.scene.scalingFactor)
			val2 = e.y
		} else if (e.dragY == -1) {
			val1 = val2 = e.y
		} else {
			val1 = e.dragY
			val2 = e.y
		}
		int w = (width == -1 ? track.bounds.width : width)
		int h = (val1 == val2 ? width : Math.abs(val1 - val2))
		def r = new Rectangle((int) track.bounds.x, Math.min(val1, val2), w, h)
		new DefaultFeedback(Feedback.CREATE_TYPE, null, Cursor.CROSSHAIR_CURSOR, null, new RectangleFeedback(r))
	}

	Command getCommand(SceneEvent e, Object target) {
		// create new instance
		def obj = clazz.newInstance()
		
		// populate from template and event
		template.each { k,v ->
	 		if (obj.properties.containsKey(k)) {
	 			obj."$k" = v
	 		}
		}
		
		// figure out the top and base
		def val1, val2
		if (linked) {
			val1 = track.models.size() > 0 ? track.mmax(track.models.last()) : track.scene.contentSize.minY / track.scene.scalingFactor
			val2 = phys(e.y)
		} else if (e.dragY == -1) {
			val1 = val2 = phys(e.y)
		} else {
			val1 = phys(e.dragY)
			val2 = phys(e.y)
		}

		// validate contiguous linked objects (Intervals, Units), preventing creation
		// of inverted and zero-length objects, or an object overlapping existing objects.
		def originTop = track.scene.origin == Origin.TOP
		if (linked) {
			if (val1 >= val2) return null;
			def overlap = false;
			track.models.each {
				def newmin = Math.min(val1, val2)
				def newmax = Math.max(val1, val2)
				def modmin = Math.min(it.top.value, it.base.value)
				def modmax = Math.max(it.top.value, it.base.value)
				// is new top or base inside object, or is object encompassed by new top and base?
				if ((newmin > modmin && newmin < modmax) || (newmax > modmin && newmax < modmax) || (newmax > modmax && newmin < modmin))
					overlap = true;
			}
			if (overlap) return null;
		}
	 
		// set the top/base
		if (originTop) {
			obj.top = "${round(Math.min(val1, val2))} ${track.units}"
			obj.base = "${round(Math.max(val1, val2))} ${track.units}"
		} else {
			obj.base = "${round(Math.min(val1, val2))} ${track.units}"
			obj.top = "${round(Math.max(val1, val2))} ${track.units}"
		}
		
		return new CreateCommand(obj, track.scene.models)
	}
}
