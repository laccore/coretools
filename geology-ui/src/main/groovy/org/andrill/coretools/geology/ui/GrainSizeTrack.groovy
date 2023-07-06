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

import java.awt.Color
import java.awt.geom.Rectangle2D

import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.geology.models.Interval
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.model.Model;

/**
 * Draws a grain size profile.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class GrainSizeTrack extends GeologyTrack {
	// Properties:
	//   * scale:   the scale code or name

	def getFilter() { return { it instanceof Interval } }
	def getHeader() { "Grain Size" }
	def getFooter() { "Grain Size" }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new ResizePolicy()])
	}

	Scale getGrainSize() {
		String code = container.project.configuration.grainSizeScale ?: Scale.DEFAULT
		return new Scale(code)
	}

	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		this.bounds = bounds
		
		// figure out our clip
		def clip = clip(bounds, graphics.clip)
		def prev = null
		models.findAll{ onpage(it, clip) }.each { m ->
			if (m?.grainSizeTop != null && m?.grainSizeBase != null) {
				def tgs = gs(m.grainSizeTop, m.top.to(units).value)
				def bgs = gs(m.grainSizeBase, m.base.to(units).value)
				
				if (prev) { graphics.drawLine(prev, tgs) }
				if (m == selection) {
					graphics.lineColor = Color.yellow
					graphics.lineThickness = 2
				}
				graphics.drawLine(tgs, bgs)
				graphics.lineThickness = 1
				graphics.lineColor = Color.black
				prev = bgs
			} else {
				prev = null
			}
		}
	}

	def gs(value, y) { pt(grainSize.toScreen(value) * bounds.width + bounds.x, pts(y, bounds)) }

	protected String getModelLabel(interval, pt) {
		return grainSize.getScreenLabel((pt.x - bounds.x) / bounds.width)
	}

	protected String getTrackLabel(pt) {
		return grainSize.getScreenLabel((pt.x - bounds.x) / bounds.width)
	}
}