package org.andrill.coretools.geology.ui.csdf

import java.awt.Color
import java.awt.geom.Rectangle2D
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.geology.ui.*

abstract class AbstractIntervalTrack extends GeologyTrack {
	// Properties:
	//   * track-header:   the text or image to draw in the header
	//   * track-footer:   the text or image to draw in the footer

	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def outline = getOutline(m)
		if (m?.scheme) {
			graphics.setFill(getFill(m))
			graphics.fillPolygon(outline)
		}
		graphics.drawPolygon(outline)
	}

	void renderSelected(Model model, GraphicsContext graphics, Rectangle2D bounds) {
		graphics.pushState()
		graphics.lineThickness = 2
		graphics.lineColor = Color.red

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
		outline << pt(bounds.maxX, pts(m.top.to(units).value, bounds))
		outline << pt(bounds.maxX, pts(m.base.to(units).value, bounds))
		outline << pt(bounds.minX, pts(m.base.to(units).value, bounds))
        return outline
	}

	private Fill getFill(m) {
		if (m?.scheme) {
			def entry = getSchemeEntry(m.scheme?.scheme, m.scheme?.code)
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
		if (interval?.scheme) {
			def entry = getSchemeEntry(interval?.scheme?.scheme, interval?.scheme?.code)
			if (entry) { label += "\n${entry.name}" }
		}
		if (interval?.description) {
			label += "\n${interval.description}"
		}
		return label
	}
}