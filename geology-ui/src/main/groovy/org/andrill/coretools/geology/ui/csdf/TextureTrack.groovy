package org.andrill.coretools.geology.ui.csdf

import java.awt.Color
import java.awt.geom.Rectangle2D

import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.models.csdf.TextureInterval
import org.andrill.coretools.geology.ui.event.*
import org.andrill.coretools.geology.ui.*
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.scene.TrackParameter
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler

class TextureTrack extends AbstractFeatureTrack {
	private static final String DEFAULT_TITLE = "Texture"
	private static final PARAMETERS = [
		"draw-repeating" : new TrackParameter("draw-repeating", "Tile symbols", "<html>If enabled, draw symbol repeatedly, filling entire interval.<br/>If disabled, draw single symbol with whiskers at interval boundaries.</html>", TrackParameter.Type.BOOLEAN, "false"),
		"draw-outline" : new TrackParameter("draw-outline", "Outline intervals", "<html>Draw a border around intervals. Applies only when Tile symbols is <b>enabled</b>.</html>", TrackParameter.Type.BOOLEAN, "true"),
		"texture-scaling" : new TrackParameter("texture-scaling", "Texture scaling", "<html>Scaling of tiled symbols. Lower values zoom in, higher values zoom out.<br>Applies only when Tile symbols is <b>enabled</b>.</html>", TrackParameter.Type.FLOAT, "1.0"),
		"symbol-size" : new TrackParameter("symbol-size", "Symbol size", "Size, in pixels, of texture symbols.", TrackParameter.Type.INTEGER, "32"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer. (Footer available only in exported diagrams.)", TrackParameter.Type.STRING, DEFAULT_TITLE),
	]

	List<TrackParameter> getTrackParameters() {	return PARAMETERS.values() as List<TrackParameter> }
	def getFilter() { return { it instanceof TextureInterval } }
	List<Class> getCreatedClasses() { return [TextureInterval] }
	def getHeader() { getParameter("track-header", DEFAULT_TITLE) }
	def getFooter() { getParameter("track-footer", DEFAULT_TITLE) }
	def getWidth()  { return 96 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(TextureInterval.class, [:], getSymbolSize()), new MovePolicy(), new ResizePolicy()])
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
		def r = getModelBounds(m)
		if (Boolean.parseBoolean(getParameter("draw-repeating", PARAMETERS["draw-repeating"].defaultValue))) {
			def outline = [pt(r.x, r.y), pt(r.x+r.width, r.y), pt(r.x+r.width, r.y+r.height), pt(r.x, r.y+r.height)]
			if (m?.scheme) {
				graphics.setFill(getFill(m))
				graphics.fillPolygon(outline)
			}
			if (getDrawOutline()) {
				graphics.drawPolygon(outline)
			}	
		} else {
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

	@Override
	def getSymbolSize() {
		return (getParameter("symbol-size", PARAMETERS["symbol-size"].defaultValue) as Integer)
	}

	private boolean getDrawOutline() { return Boolean.parseBoolean(getParameter('draw-outline', 'true')) }
}