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
package org.andrill.coretools.geology.ui

import java.awt.geom.Rectangle2D

import org.andrill.coretools.geology.models.Unitimport org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.Model;
import org.andrill.coretools.scene.event.SceneEventHandlerimport org.andrill.coretools.scene.event.DefaultTrackEventHandler/**
 * A track to draw Unit models.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class UnitTrack extends GeologyTrack {
	// Properties:
	//   * filter-group:   only show Unit of a specific group
	//   * track-header:   the text or image to draw in the header
	//   * track-footer:   the text or image to draw in the footer
	
	def getHeader() { "Units" }
	def getFooter() { "Units" }
	def getWidth()  { return 32 }
	def getFilter() { 
		String filter = getParameter("filter-group", null)
		if (filter) {
			return { it instanceof Unit && it?.group == filter }
		} else {
			return { it instanceof Unit }
		}
	}
	protected SceneEventHandler createHandler() { 
		new DefaultTrackEventHandler(this, [new CreatePolicy(Unit.class, [:], width), new ResizePolicy()])
	}
	
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def r = getModelBounds(m)
		
		String name = m.name ?: ''
		graphics.drawStringCenter(r, font, m.name ?: '')	
		graphics.drawRectangle(r)
	}
}
