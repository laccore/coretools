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
import org.andrill.coretools.geology.models.Interval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model;
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
/**
 * A track to draw Interval models.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class IntervalTrack extends GeologyTrack {
	// Properties:
	//   * track-header:   the text or image to draw in the header
	//   * track-footer:   the text or image to draw in the footer

	def getFilter() { return { it instanceof Interval } }
	def getHeader() { "Intervals" }
	def getFooter() { "Intervals" }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(Interval.class, [:]), new ResizePolicy()])
	}

	Scale getGrainSize() {
		String code = container?.project?.configuration?.grainSizeScale ?: Scale.DEFAULT
		return new Scale(code)
	}

	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def outline = getOutline(m)
		if (m?.lithology) {
			graphics.setFill(getFill(m))
			graphics.fillPolygon(outline)
		}
		graphics.drawPolygon(outline)
	}

	void renderSelected(Model model, GraphicsContext graphics, Rectangle2D bounds) {
		graphics.pushState()
		graphics.lineThickness = 1
		graphics.lineColor = Color.yellow

		// render our outline
		graphics.drawPolygon(getOutline(model))
		
		// render any handles
		def r = getModelBounds(model)
		model.constraints.each { k,v ->
			def handle = v?.handle
			switch (handle) {
				case 'north': drawHandle(r.centerX, r.minY, graphics); break
				case 'south': drawHandle(r.centerX, r.maxY, graphics); break
				case 'east' : drawHandle(r.minX, r.centerY, graphics); break
				case 'west' : drawHandle(r.maxX, r.centerY, graphics); break
			}
		}
		graphics.popState()
	}

	def getOutline(m) {
		def outline = []
		outline << pt(bounds.minX, pts(m.top.to(units).value, bounds))
        outline << gs(m?.grainSizeTop ?: 0, m.top.to(units).value)
        outline << gs(m?.grainSizeBase ?: 0, m.base.to(units).value)
        outline << pt(bounds.minX, pts(m.base.to(units).value, bounds))
        return outline
	}

	def gs(value, y) { pt(grainSize.toScreen(value) * bounds.width + bounds.x, pts(y, bounds)) }
	
	private Fill getFill(m) {
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