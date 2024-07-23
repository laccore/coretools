package org.andrill.coretools.geology.ui.csdf

import java.awt.Color
import java.awt.geom.Rectangle2D

import org.andrill.coretools.geology.models.csdf.BeddingInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.scene.TrackParameter
import org.andrill.coretools.geology.ui.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*

class BeddingTrack extends AbstractIntervalTrack {
	private static final String DEFAULT_TITLE = "Bedding"
	private static final PARAMETERS = [
		"draw-repeating" : new TrackParameter("draw-repeating", "Tile symbols", "<html>If enabled, draw symbol repeatedly, filling entire interval.<br/>If disabled, draw single symbol with whiskers at interval boundaries.</html>", TrackParameter.Type.BOOLEAN, "false"),
		"draw-outline" : new TrackParameter("draw-outline", "Outline intervals", "<html>Draw a border around intervals. Applies only when Tile symbols is <b>enabled</b>.</html>", TrackParameter.Type.BOOLEAN, "true"),
		"symbol-size" : new TrackParameter("symbol-size", "Symbol size", "<html>Size, in pixels, of bedding symbols. Applies only when Tile symbols is <b>disabled</b>.</html>", TrackParameter.Type.INTEGER, "32"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer. (Footer available only in exported diagrams.)", TrackParameter.Type.STRING, DEFAULT_TITLE),
	]

	List<TrackParameter> getTrackParameters() { return PARAMETERS.values() as List<TrackParameter> }

	def getFilter() { return { it instanceof BeddingInterval } }
	List<Class> getCreatedClasses() { return [BeddingInterval] }
	def getHeader() { getParameter("track-header", DEFAULT_TITLE) }
	def getFooter() { getParameter("track-footer", DEFAULT_TITLE) }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(BeddingInterval.class, [:]), new ResizePolicy()])
	}

	@Override
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		if (Boolean.parseBoolean(getParameter("draw-repeating", PARAMETERS["draw-repeating"].defaultValue))) {
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
		return (getParameter("symbol-size", PARAMETERS["symbol-size"].defaultValue) as Integer)
	}
}