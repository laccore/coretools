
package org.andrill.coretools.geology.ui.csdf

import java.awt.Point
import java.awt.geom.Rectangle2D

import org.andrill.coretools.geology.models.csdf.UnitInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.Model;
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.GeologyTrack

class UnitTrack extends GeologyTrack {
	// Properties:
	//   * filter-group:   string; only show Unit of a specific group
	//   * track-header:   string; the text or image to draw in the header
	//   * track-footer:   string; the text or image to draw in the footer
	
	def getHeader() { "Units" }
	def getFooter() { "Units" }
	def getWidth()  { return 32 }
	def getFilter() { 
		String filter = getParameter("filter-group", null)
		if (filter) {
			return { it instanceof UnitInterval && it?.group == filter }
		} else {
			return { it instanceof UnitInterval }
		}
	}
	List<Class> getCreatedClasses() { return [UnitInterval] }
	protected SceneEventHandler createHandler() { 
		new DefaultTrackEventHandler(this, [new CreatePolicy(UnitInterval.class, [:], width), new ResizePolicy()])
	}
	
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def r = getModelBounds(m)
		
		String name = m.name ?: ''
		if (name.length() > 0) {
			// don't draw name into units above or below, or outside of page bounds
			def oldClip = graphics.getClip()
			graphics.setClip(r.createIntersection(oldClip))
			
			def xmid = (r.getX() + (r.width / 2)).intValue()
			def ymid = (r.getY() + (r.height / 2)).intValue()
			def bds = graphics.getStringBounds(font, name)
			
			def pt = new Point(xmid - (bds.height / 2).intValue(), ymid + (bds.width / 2).intValue())
			graphics.drawStringRotated(pt, font, name, -(java.lang.Math.PI / 2.0)) // 90 degrees CCW
			graphics.setClip(oldClip) // restore old clipping region
		}
		graphics.drawPolygon(getOutline(m))
	}
	
	def getOutline(m) {
		def outline = []
		outline << pt(bounds.minX, pts(m.top.to(units).value, bounds))
		outline << pt(bounds.maxX, pts(m.top.to(units).value, bounds))
		outline << pt(bounds.maxX, pts(m.base.to(units).value, bounds))
		outline << pt(bounds.minX, pts(m.base.to(units).value, bounds))
		return outline
	}
}
