package org.andrill.coretools.geology.ui.csdf

import org.andrill.coretools.geology.models.csdf.BeddingInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.*
import org.andrill.coretools.model.Model

import java.awt.Color
import java.awt.geom.Rectangle2D
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*

class BeddingTrack extends AbstractIntervalTrack {
	// Properties:
	//   * track-header:   string; the text or image to draw in the header
	//   * track-footer:   string; the text or image to draw in the footer
	//   * draw-outline:   boolean; draw outline of interval, applies only if draw-repeating is false
	//   * draw-repeating: boolean; draw the symbols repeating instead of whiskers
	//   * symbol-size:    integer; pixel width of rendered symbol if draw-repeating is false

	def getFilter() { return { it instanceof BeddingInterval } }
	List<Class> getCreatedClasses() { return [BeddingInterval] }
	def getHeader() { "Bedding" }
	def getFooter() { "Bedding" }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(BeddingInterval.class, [:]), new ResizePolicy()])
	}

	@Override
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		if (Boolean.parseBoolean(getParameter("draw-repeating", "true"))) {
			super.renderModel(m, graphics, bounds)
		} else {
			def r = getModelBounds(m)
			final ss = getSymbolSize()

			// draw whiskered intervals
			if (m?.scheme) {
				graphics.drawLine(r.minX, r.y, r.maxX, r.y)
				graphics.drawLine(r.minX, r.maxY, r.maxX, r.maxY)
				graphics.drawLine(r.centerX, r.y, r.centerX, r.maxY)

				def imageURL = getSchemeEntry(m?.scheme?.scheme, m?.scheme?.code).imageURL

				// calculate our centered rectangle and draw the image in it
				if (ss > r.height) {
					ss = r.height - 4
					if (ss <= 0) { ss = 1 }
				}
				def cr = rect(r.centerX - (ss/2), r.centerY - (ss/2), ss, ss)
				graphics.setFill(Color.white)
				graphics.fillRectangle(cr)
				graphics.drawImage(cr, imageURL)
			} else {
				graphics.drawLine(r.minX, r.minY, r.maxX, r.maxY)
				graphics.drawLine(r.minX, r.maxY, r.maxX, r.minY)
				graphics.drawRectangle(r)
			}
		}
	}

	def getSymbolSize() {
		return (getParameter("symbol-size", "32") as Integer)
	}
}