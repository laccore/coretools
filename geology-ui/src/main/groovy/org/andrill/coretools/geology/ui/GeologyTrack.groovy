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

import java.math.RoundingMode
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Point
import java.awt.Rectangle
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.concurrent.atomic.AtomicBoolean

import org.andrill.coretools.Platform;
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.models.util.GeologyModelIndex
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.model.scheme.SchemeManager
import org.andrill.coretools.scene.LabelProvider
import org.andrill.coretools.scene.Scene
import org.andrill.coretools.scene.Scene.Origin
import org.andrill.coretools.scene.Scene.ScenePart
import org.andrill.coretools.scene.Track
import org.andrill.coretools.scene.event.SceneEventHandler

import com.google.common.collect.ImmutableMap;

/**
 * An abstract base class for geology-related tracks.
 *
 * @author Josh Reed (jareed@andrill.org)
 */
abstract class GeologyTrack implements ModelContainer.Listener, Track, LabelProvider {
	protected static def INDEX = [:]
	Scene scene
	protected ModelContainer container
	protected SceneEventHandler handler
	private AtomicBoolean valid = new AtomicBoolean(false)
	Rectangle2D bounds = new Rectangle(0,0,0,0)
	
	def getIndex() { INDEX[container] }
	def getModels() { INDEX[container].getAllModels((getFilter() as GeologyModelIndex.Filter)) as List }
	def getHeader() { "Header" }
	def getFooter() { "Footer" }
	def getFilter() { return { false } }
	def getWidth()  { return 72 }
	protected Map<String, String> parameters = [:]
	protected SceneEventHandler createHandler() { null }
	
	void validate() {
		if (!valid.getAndSet(true)) {
			layout()
		}
	}
	void invalidate() {
		valid.getAndSet(false) 
	}
	
	void layout() { }
	
	Object getAdapter(Class clazz) {
		if (clazz == SceneEventHandler.class) {
			if (handler == null) {
				handler = createHandler()
			}
			return handler
		} else if (clazz == LabelProvider.class) {
			return this
		}
		return null
	}
	
	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		validate()
		
		this.bounds = bounds
		def clip = clip(bounds, graphics.clip)
		def selected = selection
		def sel
		
		index.get(new Length(clip.minY, units).to('m').value, new Length(clip.maxY, units).to('m').value).findAll(filter).each { m ->
			renderModel(m, graphics, clip)
			if (m == selected) { sel = m }
		}
		if (sel) { renderSelected(sel, graphics, clip) }
	}
	
	void renderSelected(Model model, GraphicsContext graphics, Rectangle2D bounds) {
		graphics.pushState()
		graphics.lineThickness = 2
		graphics.lineColor = Color.red
		
		// render our outline
		def r = getModelBounds(model)
		def dx = (r.minX == bounds.minX) ? 1 : 0
		def dw = (r.maxX == bounds.maxX) ? dx + 1 : dx
		graphics.drawRectangle(rect2d(r.x + dx, r.y, r.width - dw, r.height))
		
		// render any handles
		if (scene.commandStack.isEditable()) {
			model.constraints.each { k,v ->
				def handle = v?.handle
				switch (handle) {
					case 'north': drawHandle(r.centerX, r.minY, graphics); break
					case 'south': drawHandle(r.centerX, r.maxY, graphics); break
					case 'east' : drawHandle(r.minX, r.centerY, graphics); break
					case 'west' : drawHandle(r.maxX, r.centerY, graphics); break
				}
			}
		}
		graphics.popState()
	}
	
	void drawHandle(x, y, GraphicsContext graphics) {
		graphics.lineThickness = 1
		graphics.lineColor = Color.black
		graphics.drawLine(x - 7, y, x + 7, y)
		graphics.setFill(Color.black)
		graphics.fillRectangle(x - 2, y - 2, 5, 5)
		graphics.setFill(Color.yellow)
		graphics.fillRectangle(x - 1, y - 1, 3, 3)
	}
	
	void renderModel(Model model, GraphicsContext graphics, Rectangle2D bounds) {
		// overridden by subclasses
	}
	
	void setModels(ModelContainer container) {
		// unset and set our listeners
		if (this.container) { 
			this.container.removeListener(this)
			INDEX[this.container].disconnect()
		}
		this.container = container
		if (this.container) {
			this.container.addListener(this)
			if (!INDEX.containsKey(this.container)) {
				def index = new GeologyModelIndex()
				index.connect(this.container)
				INDEX[this.container] = index
			}
		}
		invalidate()
	}
	
	Rectangle2D getContentSize() {
		if (container.models.size() == 0) {
			return rect2d(bounds.x, 0, width, -1)
		} else {
			def min = index.get(index.minIndex).inject(Double.MAX_VALUE) { min, m -> Math.min(min, mmin(m)) }
			def max = index.get(index.maxIndex).inject(Double.MIN_VALUE) { max, m -> Math.max(max, mmax(m)) }
			return rect2d(0, scale(min), width, scale(max - min))
		}	
	}
	
	void renderFooter(GraphicsContext graphics, Rectangle2D bounds) {
		renderTextOrImage(graphics, bounds, getParameter("track-footer", footer))
	}
	
	void renderHeader(GraphicsContext graphics, Rectangle2D bounds) {
		renderTextOrImage(graphics, bounds, getParameter("track-header", header))
	}
	
	Object findAt(Point2D screen, ScenePart part) {
		if (part == ScenePart.HEADER || part == ScenePart.FOOTER) {
			return this
		} else {
			return index.get(physM(screen.y, bounds)).findAll(filter).find { getModelBounds(it).contains(screen.x, screen.y) } ?: this
		}
	}
	
	Rectangle2D getModelBounds(Model m) { mrect(m) }
	
	protected String getHint(name, defaultValue) { scene.renderHints[name] ?: defaultValue }
	
	void renderTextOrImage(GraphicsContext graphics, Rectangle2D bounds, String value) {
		if (value && value.contains(":/")) {
			def image = svc(ResourceLoader.class).getResource(value)
			if (image) {
				graphics.drawImageCenter(bounds, image)
			} else {
				graphics.drawStringCenter(bounds, font, value)
			}
		} else if (value) {
			graphics.drawStringCenter(bounds, font, value)
		}
	}
	
	// geometry properties
	def dim(w, h)			{ new Dimension((int) w, (int) h) }
	def pt(x, y)			{ new Point((int) x, (int) y) }
	def pt2d(x, y)			{ new Point2D.Double(x, y) }
	def rect(x, y, w, h)	{ new Rectangle((int) x, (int) y, (int) w, (int) h) }
	def rect2d(x, y, w, h)	{ new Rectangle2D.Double(x, y, w, h) }
	def mrect(m, x = bounds.x, w = bounds.width)	{ 
		def t = pts(m.top.to(units).value, bounds)
		def b = pts(m.base.to(units).value, bounds)
		rect(x, t, w, b - t)
	}
	
	// common properties and conversions
	def getFont()   	{ new Font("SanSerif", Font.PLAIN, 12) }
	def getUnits()		{ getHint("preferred-units", "m") }
	def getScale() 		{ scene.scalingFactor }
	def scale(val) 		{ val * scene.scalingFactor }
	def mmax(m)			{ (scene.origin == Origin.TOP) ? m.base.to(units).value : m.top.to(units).value }
	def mmin(m)			{ (scene.origin == Origin.TOP) ? m.top.to(units).value : m.base.to(units).value }
	def pts(phys, bounds) { 
		if (scene.origin == Origin.TOP) {
			return phys * scale
		} else {
			return bounds.maxY - (phys * scale) + bounds.minY
		}
	}
	def phys(pts, bounds) { 
		if (scene.origin == Origin.TOP) {
			return pts / scale
		} else {
			return ((bounds.maxY - pts) / scale) + (bounds.minY / scale)
		}
	}
	def physM(pts, bounds) {
		def val = phys(pts, bounds)
		new Length(val, units).to('m').value
	}
	def onpage(m, b)	{
		b.intersects(b.x, mmin(m), b.width, Math.max(mmax(m) - mmin(m), 1 / scale))
	}
	def clip(bounds, clip, jitter = 0) {
		def c = clip ? bounds.createIntersection(clip) : bounds
		def ct = phys(c.minY, bounds)
		def cb = phys(c.maxY, bounds)
		rect2d(c.x, Math.min(ct, cb) - jitter, c.width, Math.abs(cb - ct) + 2*jitter)
	}
	
	// services
	def svc(clazz) { Platform.getService(clazz) }
	def getSelection() {
		return (scene.selection && !scene.selection.isEmpty()) ? scene.selection.firstObject : null
	}
	
	// container listener
	void modelAdded(Model m) {
		if (filter(m)) { invalidate() }
	}
	void modelRemoved(Model m) {
		if (index.allModels.contains(m)) { invalidate() }
	}
	void modelUpdated(Model m) {
		if (filter(m)) { invalidate() }
	}
	
	// label provider
	String getLabel(Point2D screen, ScenePart part) {
		if (part == ScenePart.HEADER) {
			return header
		} else if (part == ScenePart.FOOTER) {
			return footer
		} else {
			def obj = findAt(screen, part)
			def label = (obj == this) ? getTrackLabel(screen) : getModelLabel(obj, screen)
			def phys = new BigDecimal(phys(screen.y, bounds)).setScale(2, RoundingMode.HALF_UP)
			if (label) {
				return "$phys $units: ${label}"
			} else {
				return "$phys $units"	
			}
		}
	}
	
	protected String getModelLabel(model, pt) {
		return model.toString()
	}
	protected String getTrackLabel(pt) {
		return null
	}
	
	// scheme-related
	def getSchemeEntry(String id, String code) {
		def scheme = svc(SchemeManager.class).getScheme(id)
		scheme == null ? null : scheme.getEntry(code)
	}
	
	String getParameter(String name, String defaultValue) {
	    parameters[name] ?: defaultValue
	}
	
	ImmutableMap<String, String> getParameters() {
	    return ImmutableMap.copyOf(parameters)
	}
	
	void setParameter(String name, String value) {
	    if (value) {
	    	parameters[name] = value
		} else {
			parameters.remove(name)
		}
	}
}