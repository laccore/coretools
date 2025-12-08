
package org.andrill.coretools.geology.ui.csdf

import java.awt.Color
import java.awt.Point
import java.awt.geom.Rectangle2D

import org.andrill.coretools.geology.models.csdf.UnitInterval
import org.andrill.coretools.geology.models.Section
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.Model
import org.andrill.coretools.scene.TrackParameter
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.GeologyTrack

class SectionNameTrack extends GeologyTrack {
	private static final String DEFAULT_TITLE = "Section"
	private static final PARAMETERS = [
		"omit-text" : new TrackParameter("omit-text", "Omit text", "<html>Comma-separated list of text to omit from section names. Results in larger, more concise ID components.<br>Example: for sections GLAD9-PET06-2A-1H-1, GLAD9-PET06-2A-1H-2, GLAD9-PET06-2A-2H-1...,<br>if 'GLAD9-PET06-' is omitted, drawn section names will be 2A-1H-1, 2A-1H-2, 2A-2H-1...", TrackParameter.Type.STRING, ""),
		"draw-vertical" : new TrackParameter("draw-vertical", "Vertical", "Draw section name vertically instead of horizontally", TrackParameter.Type.BOOLEAN, "false"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer. (Footer available only in exported diagrams.)", TrackParameter.Type.STRING, DEFAULT_TITLE),
	]

	List<TrackParameter> getTrackParameters() { return PARAMETERS.values() as List<TrackParameter> }
	
	def getHeader() { getParameter("track-header", DEFAULT_TITLE) }
	def getFooter() { getParameter("track-footer", DEFAULT_TITLE) }
	def getWidth()  { return 96 }
	def getFilter() { return { it instanceof Section }}
	
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def r = getModelBounds(m)
		
		String name = m.name ? omit(m.name): ''
		if (name.length() > 0) {
			def oldClip = graphics.getClip() // clip to model bounds
			graphics.setClip(r.createIntersection(oldClip))
			
			def xmid = (r.getX() + (r.width / 2)).intValue()
			def ymid = (r.getY() + (r.height / 2)).intValue()

            def curFont = font
            def bds = null
            while (true) {
			    bds = graphics.getStringBounds(curFont, name)
				boolean tooLarge = drawVertical() ? (bds.width >= r.height || bds.height >= r.width) : (bds.width >= r.width || bds.height >= r.height)
                if (tooLarge) {
                    int fontSize = curFont.getSize()
                    if (fontSize - 1 < 1) {
                        break // can't get any smaller, give up
                    }
                    curFont = curFont.deriveFont((float)(fontSize - 1))
                } else {
                    break
                }
            }

			if (drawVertical()) {
				def pt = new Point(xmid - (bds.height / 2).intValue(), ymid + (bds.width / 2).intValue())
				graphics.drawStringRotated(pt, curFont, name, -(java.lang.Math.PI / 2.0)) // 90 degrees CCW
			} else {
				def pt = new Point(xmid - (bds.width / 2).intValue(), ymid - (bds.height / 2).intValue())
				graphics.drawString(pt, curFont, name)
			}
			graphics.setClip(oldClip) // restore old clipping region
		}

		def ytop = pts(m.top.to(units).value, bounds)
		def ybase = pts(m.base.to(units).value, bounds)
		graphics.drawRectangle(bounds.minX, ytop, bounds.maxX - bounds.minX, ybase - ytop)
	}

	private boolean drawVertical() {
		return Boolean.parseBoolean(getParameter("draw-vertical", "false"))
	}

	private String omit(String sectionName) {
		def result = sectionName
		if (hasParameter("omit-text")) {
			String textToOmit = getParameter("omit-text", "")
			textToOmit.split(",").each {
				if (it.length() > 0) {
					result = result.replaceAll(it, "")
				}
			}
		}
		return result
	}
}
