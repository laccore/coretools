package org.andrill.coretools.geology.ui.csdf

import java.awt.Color
import java.awt.Point
import java.awt.geom.Rectangle2D
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.models.csdf.*
import org.andrill.coretools.geology.ui.Scale
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.scene.TrackParameter
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler


class LithologyTrack extends AbstractIntervalTrack {
	private static final String DEFAULT_TITLE = "Lithology"
	private static final PARAMETERS = [
		"draw-outline" : new TrackParameter("draw-outline", "Outline intervals", "Draw a border around intervals.", TrackParameter.Type.BOOLEAN, "true"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer.", TrackParameter.Type.STRING, DEFAULT_TITLE),		
	]

	List<TrackParameter> getTrackParameters() { return PARAMETERS.values() as List<TrackParameter> }

	def getFilter() { return { it instanceof LithologyInterval } }
	List<Class> getCreatedClasses() { return [LithologyInterval] }
	def getHeader() { getParameter("track-header", DEFAULT_TITLE) }
	def getFooter() { getParameter("track-footer", DEFAULT_TITLE) }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(LithologyInterval.class, [:]), new ResizePolicy()])
	}

	@Override
	void renderHeader(GraphicsContext graphics, Rectangle2D bounds) {
		if (Boolean.parseBoolean(getParameter("grain-size-header", "false"))) {
			def newBounds = bounds.clone()
			newBounds.height = newBounds.height - Math.abs(128 - 36);
			renderTextOrImage(graphics, newBounds, getParameter("track-header", header))

			grainSize.values.eachWithIndex { gsval, index ->
				// println "GS value ${gsval} toScreen = ${grainSize.toScreen(gsval)}"
				def x = bounds.x + bounds.width * grainSize.toScreen(gsval)
				graphics.drawLine(x, bounds.y + bounds.height, x, bounds.y + 36)
				if (index > 0) {
					def gsstr = grainSize.labels[index-1]
					def gs_x = bounds.x + bounds.width * grainSize.toScreen(grainSize.values[index-1])
					graphics.drawStringRotated(new Point(gs_x.intValue(), (bounds.y + bounds.height).intValue()), font, gsstr, -(java.lang.Math.PI / 2.0))
				}
			}
		} else {
			super.renderHeader(graphics, bounds)
		}
	}

	@Override
	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		validate()
		
		this.bounds = bounds
		def clip = clip(bounds, graphics.clip)
		def selected = selection
		def sel

		final grainSizeIntervals = index.get(new Length(clip.minY, units).to('m').value, new Length(clip.maxY, units).to('m').value).findAll({it instanceof GrainSizeInterval})
		
		index.get(new Length(clip.minY, units).to('m').value, new Length(clip.maxY, units).to('m').value).findAll(filter).each { m ->
			renderModel(m, graphics, clip, grainSizeIntervals)
			if (m == selected) { sel = m }
		}
		if (sel) { renderSelected(sel, graphics, clip, grainSizeIntervals) }
	}

	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds, ArrayList<GrainSizeInterval> grainSizeIntervals) {
		def outline = getOutline(m, grainSizeIntervals)
		if (m?.scheme) {
			graphics.setFill(getFill(m, graphics.fill))
			graphics.fillPolygon(outline)
		}
		if (getDrawOutline()) {
			graphics.drawPolygon(outline)
		}
	}

	void renderSelected(Model model, GraphicsContext graphics, Rectangle2D bounds, ArrayList<GrainSizeInterval> grainSizeIntervals) {
		graphics.pushState()
		graphics.lineThickness = 2
		graphics.lineColor = Color.red

		// render our outline
		graphics.drawPolygon(getOutline(model, grainSizeIntervals))
		
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

	def getOutline(Model m, ArrayList<GrainSizeInterval> grainSizeIntervals) {
		def outline = []

		outline << pt(bounds.minX, pts(m.top.to(units).value, bounds))
		outline << gs(0, m.top.to(units).value)

		// draw subintervals of this LithologyInterval at widths corresponding to
		// GrainSizeIntervals in those subintervals
		final modelRect = mrect(m)
		for (int i = 0; i < grainSizeIntervals.size(); i++) {
			def gsi = grainSizeIntervals[i]
			final gsiRect = mrect(gsi)
			if (modelRect.intersects(gsiRect)) {
				def intersection = modelRect.intersection(gsiRect)
				def width = 0
				if (gsi.scheme) {
					def entry = getSchemeEntry(gsi.scheme.scheme, gsi.scheme.code)
					width = Integer.parseInt(entry.getProperty('width', '0'))
				}
				outline << pt(gswidth(width), intersection.y)
				outline << pt(gswidth(width), intersection.y + intersection.height)

				// Ensure bottom of this LithologyInterval is rendered if it extends below
				// bottom-most GrainSizeInterval
				if (i == grainSizeIntervals.size() - 1 && m.base.to('m').value > gsi.base.to('m').value) {
					// println("Model $m extends beyond last GS $gsi")
					outline << gs(0, gsi.base.to(units).value)
				}
			}
		}

		outline << gs(0, m.base.to(units).value)
		outline << pt(bounds.minX, pts(m.base.to(units).value, bounds))
        return outline
	}

	Scale getGrainSize() {
		String code = container?.project?.configuration?.grainSizeScale ?: Scale.DEFAULT
		return new Scale(code)
	}

	def gs(value, y) { pt(grainSize.toScreen(value) * bounds.width + bounds.x, pts(y, bounds)) }
	def gswidth(value) { grainSize.toScreen(value) * bounds.width + bounds.x }
	
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