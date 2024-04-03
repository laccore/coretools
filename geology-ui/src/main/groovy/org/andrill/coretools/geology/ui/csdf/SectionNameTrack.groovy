
package org.andrill.coretools.geology.ui.csdf

import java.awt.Point
import java.awt.geom.Rectangle2D

import org.andrill.coretools.geology.models.csdf.UnitInterval
import org.andrill.coretools.geology.models.Section;
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.Model;
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.GeologyTrack

class SectionNameTrack extends GeologyTrack {
	// Properties:
	//   * track-header:   string; the text or image to draw in the header
	//   * track-footer:   string; the text or image to draw in the footer
	
	def getHeader() { "Section" }
	def getFooter() { "Section" }
	def getWidth()  { return 32 }
	def getFilter() { 
        return { it instanceof Section }
	}
	
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def r = getModelBounds(m)
		
		String name = m.name ?: ''
		if (name.length() > 0) {
			def oldClip = graphics.getClip() // clip to model bounds
			graphics.setClip(r.createIntersection(oldClip))
			
			def xmid = (r.getX() + (r.width / 2)).intValue()
			def ymid = (r.getY() + (r.height / 2)).intValue()

            def curFont = font
            def bds = null
            while (true) {
			    bds = graphics.getStringBounds(curFont, name)
                if (bds.width >= bounds.width || bds.height >= r.height) {
                    int fontSize = curFont.getSize()
                    if (fontSize < 1) {
                        break // can't get any smaller, give up
                    }
                    curFont = curFont.deriveFont((float)(fontSize - 1))
                } else {
                    break
                }
            }
			def pt = new Point(xmid - (bds.width / 2).intValue(), ymid - (bds.height / 2).intValue())
			graphics.drawString(pt, curFont, name)
			graphics.setClip(oldClip) // restore old clipping region
		}

		def ytop = pts(m.top.to(units).value, bounds)
		def ybase = pts(m.base.to(units).value, bounds)
		graphics.drawRectangle(bounds.minX, ytop, bounds.maxX - bounds.minX, ybase - ytop)
	}
}
