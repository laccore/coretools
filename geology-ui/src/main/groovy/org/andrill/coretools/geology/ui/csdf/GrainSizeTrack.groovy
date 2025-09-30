package org.andrill.coretools.geology.ui.csdf

import java.awt.Color
import java.awt.geom.Rectangle2D

import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.models.csdf.GrainSizeInterval
import org.andrill.coretools.geology.ui.*
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.scene.TrackParameter
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler

class GrainSizeTrack extends AbstractIntervalTrack {
	private static final String DEFAULT_TITLE = "Grain Size"
	private static final PARAMETERS = [
		"draw-outline" : new TrackParameter("draw-outline", "Outline intervals", "Draw a border around intervals.", TrackParameter.Type.BOOLEAN, "true"),
		"texture-scaling" : new TrackParameter("texture-scaling", "Texture scaling", "Scaling of patterns. Lower values zoom in, higher values zoom out.", TrackParameter.Type.FLOAT, "1.0"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer. (Footer available only in exported diagrams.)", TrackParameter.Type.STRING, DEFAULT_TITLE),
	]

	List<TrackParameter> getTrackParameters() { return PARAMETERS.values() as List<TrackParameter> }
	def getFilter() { return { it instanceof GrainSizeInterval } }
	List<Class> getCreatedClasses() { return [GrainSizeInterval] }
	def getHeader() { getParameter("track-header", DEFAULT_TITLE) }
	def getFooter() { getParameter("track-footer", DEFAULT_TITLE) }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(GrainSizeInterval.class, [:]), new ResizePolicy()])
	}

	@Override
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

	@Override
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def outline = getOutline(m)
		if (m?.scheme) {
			graphics.setFill(getFill(m))
			graphics.fillPolygon(outline)
		}
		if (getDrawOutline()) {
			graphics.drawPolygon(outline)
		}
	}

	@Override
	private Fill getFill(m) {
		if (m?.scheme) {
			def entry = getSchemeEntry(m.scheme?.scheme, m.scheme?.code)
			if (!entry) { return null }

			final float textureScaling = Double.parseDouble(getParameter("texture-scaling", "1.0"))
			Color color = entry.color
			URL image = entry.imageURL
			if (image && color) {
				return new MultiFill(new ColorFill(color), new TextureFill(image, textureScaling))
			} else if (image) {
				return new TextureFill(image, textureScaling)
			} else if (color) {
				return new ColorFill(color)
			}
		}
	}
}