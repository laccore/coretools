/*
import org.andrill.coretools.graphics.GraphicsContext;
import org.andrill.coretools.graphics.GraphicsContext;
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
import org.andrill.coretools.geology.models.Interval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model;
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler

/**
 * A track to draw a lithology strip.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class LithologyTrack extends GeologyTrack {
	// Properties:
	//   * track-header:   the text or image to draw in the header
	//   * track-footer:   the text or image to draw in the footer

	def getFilter() { return { it instanceof Interval } }
	def getHeader() { "Lithology" }
	def getFooter() { "Lithology" }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(Interval.class, [:]), new ResizePolicy()])
	}

	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		if (m?.lithology) {
			def r = getModelBounds(m)
			
			// fill our rectangle with the pattern
			graphics.setFill(getFill(m, graphics.fill))
			graphics.fillRectangle(r)
	
			// draw our contacts
			graphics.drawLine(r.minX, r.minY, r.maxX, r.minY)
			graphics.drawLine(r.minX, r.maxY, r.maxX, r.maxY)
		}
	}
	
	private Fill getFill(m, fill) {
		if (m?.lithology) {
			def entry = getSchemeEntry(m.lithology?.scheme, m.lithology?.code)
			if (!entry) { return null }
			
			Color color = entry.color
			URL image = entry.imageURL
			if (image && color) {
				return new MultiFill(new ColorFill(color), new TextureFill(image))
			} else if (image) {
				return new TextureFill(image)
			} else if (color) {
				return new ColorFill(color)
			}
		} else {
			return new ColorFill(fill)
		}
	}

	protected String getModelLabel(interval, pt) {
		def label = "<br/><b>$interval</b>"
		if (interval?.lithology) {
			def entry = getSchemeEntry(interval?.lithology?.scheme, interval?.lithology?.code)
			if (entry) { label += "\n${entry.name}" }
		}
		if (interval?.description) {
			label += "\n${interval.description}"
		}
		return label
	}
}