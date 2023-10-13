package org.andrill.coretools.geology.ui.csdf

import java.awt.Color
import java.awt.geom.Rectangle2D
import org.andrill.coretools.geology.models.csdf.LithologyInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler


class LithologyTrack extends AbstractIntervalTrack {
	// Properties:
	//   * track-header:   the text or image to draw in the header
	//   * track-footer:   the text or image to draw in the footer

	def getFilter() { return { it instanceof LithologyInterval } }
	def getHeader() { "Lithology" }
	def getFooter() { "Lithology" }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(LithologyInterval.class, [:]), new ResizePolicy()])
	}

	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		if (m?.scheme) {
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
		} else {
			return new ColorFill(fill)
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