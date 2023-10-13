package org.andrill.coretools.geology.ui.csdf

import java.awt.Color
import java.awt.Cursor
import java.awt.Rectangle
import java.awt.geom.Rectangle2D
import java.math.RoundingMode

import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.geology.ui.event.MovePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.edit.Command
import org.andrill.coretools.model.edit.EditableProperty
import org.andrill.coretools.scene.Scene.Origin
import org.andrill.coretools.scene.event.Feedback
import org.andrill.coretools.scene.event.DefaultFeedback
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.SceneMouseEvent
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.*
import org.andrill.coretools.geology.models.*
import org.andrill.coretools.geology.models.csdf.Feature

class FeatureTrack extends GeologyTrack {
	// Properties:
	//   * filter-group:   only show Features of a specific group
	//   * symbol-size:    the width of the symbol
	//   * draw-repeating: draw the symbols repeating instead of whiskers
	//   * track-header:   the text or image to draw in the header
	//   * track-footer:   the text or image to draw in the footer
	
	def cache = [:]
	def getHeader() { "Features" }
	def getFooter() { "Features" }
	def getWidth()  { return 96 }
	def getFilter() { 
		String filter = getParameter("filter-group", null)
		if (filter) {
			return { it instanceof Feature && it?.group == filter }
		} else {
			return { it instanceof Feature }
		}
	}
	protected SceneEventHandler createHandler() { 
		new DefaultTrackEventHandler(this, [new CreatePolicy(Feature.class, [:], symbolSize), new ResizePolicy(), new MovePolicy()])
	}

	def layout(Model m) {
		// need to re-layout if scalingFactor or image has changed, otherwise use cached values
		if (cache[m] && scene.scalingFactor == cache[m].scalingFactor &&
			((cache[m].image == null) || (cache[m].image && cache[m].image == getSchemeEntry(m?.scheme?.scheme, m?.scheme?.code).imageURL))) { 
			return cache[m].bounds
		}

		// base model bounds
		int ss = symbolSize
		def r = mrect(m, 0, ss)
		if (r.height < ss) { r = rect(r.x, r.y - (ss - r.height)/2, r.width, ss) }

		// adjust for overlap
		def offset = new Length("1 m").value
		def intersecting = index.get(mmin_meters(m) - offset, mmax_meters(m)).findAll(filter) as List
		int index = intersecting.indexOf(m)
		if (index > 0) {
			def overlap = intersecting[0..index - 1].find { r.intersects(layout(it)) }
			while (overlap) {
				r.translate(ss, 0)
				overlap = intersecting[0..index - 1].find { r.intersects(layout(it)) }
			}
		}

		// cache the results
		def entry = getSchemeEntry(m?.scheme?.scheme, m?.scheme?.code)
		cache[m] = new CachedFeature(bounds: r, image: entry == null ? null : entry?.imageURL, scalingFactor: scene.scalingFactor)
		return r
	}
		
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		boolean drawRepeating = Boolean.parseBoolean(getParameter("draw-repeating", "false"))
		int ss = symbolSize
		def r = getModelBounds(m)
		def c = cache[m]

		// see if we got an image
		if (c.image) {
			if (drawRepeating) {
				// calculate the number of whole symbols we can draw in our rectangle
				int num = r.height / ss

				// calculate the centered rectangle
				def cr = (num == 0) ? rect(r.x, r.centerY - (ss/2), ss, ss) : rect(r.x, r.centerY - (num * (ss/2)), ss, num*ss)

				// draw whiskers if our centered rectangle is smaller than our whole rectangle
				if (cr.height < r.height) {
					graphics.drawLine(r.minX, r.y, r.maxX, r.y)
					graphics.drawLine(r.minX, r.maxY, r.maxX, r.maxY)
					graphics.drawLine(r.centerX, r.y, r.centerX, r.maxY)
				}

				// fill our centered rectangle
				graphics.setFill(Color.white)
				graphics.fillRectangle(cr)

				// draw our repeating images
				for (int i = cr.y; i < cr.maxY; i += ss) {
					graphics.drawImage(cr.x, i, ss, ss, c.image)
				}
			} else {
				// draw our whiskers
				if (!drawRepeating && r.height > ss) {
					graphics.drawLine(r.minX, r.y, r.maxX, r.y)
					graphics.drawLine(r.minX, r.maxY, r.maxX, r.maxY)
					graphics.drawLine(r.centerX, r.y, r.centerX, r.maxY)
				}

				// calculate our centered rectangle and draw the image in it
				def cr = rect(r.x, r.centerY - (ss/2), ss, ss)
				graphics.setFill(Color.white)
				graphics.fillRectangle(cr)
				graphics.drawImage(cr, c.image)
			}
		} else {
			graphics.drawLine(r.minX, r.minY, r.maxX, r.maxY)
			graphics.drawLine(r.minX, r.maxY, r.maxX, r.minY)
			graphics.drawRectangle(r)
		}
	}

	def getSymbolSize() {
		return (getParameter("symbol-size", "32") as Integer)
	}
	
	Rectangle2D getModelBounds(Model m) {
		def r = layout(m)
		rect2d(r.x + bounds.x, r.y, r.width, r.height)
	}

	// get min and max of a Model in meters, the unit expected by GeologyModelIndex
	def mmin_meters(Model m) { new Length(mmin(m), units).to('m').value }
	def mmax_meters(Model m) { new Length(mmax(m), units).to('m').value }

	void modelAdded(Model m) {
		//super.modelAdded(m)
		def offset = new Length("1 m").value
		index.get(mmin_meters(m) - offset, mmax_meters(m) + offset).findAll(filter).each { cache.remove(it) }
		invalidate()
	}
	void modelRemoved(Model m) {
		//super.modelRemoved(m)
		def offset = new Length("1 m").value
		index.get(mmin_meters(m) - offset, mmax_meters(m) + offset).findAll(filter).each { cache.remove(it) }
		invalidate()
	}
	void modelUpdated(Model m) {
		//super.modelUpdated(m);
		def offset = new Length("1 m").value
		def r = cache[m]?.bounds
		if (r) {
			// use physM to ensure results in meters, the unit expected by GeologyModelIndex
			def min = physM(r.minY, bounds) - offset
			def max = physM(r.maxY, bounds) + offset
			index.get(min, max).findAll(filter).each { cache.remove(it) }
		}
		invalidate()
	}
}

class CachedFeature {
	Rectangle bounds
	URL image
	def scalingFactor
}