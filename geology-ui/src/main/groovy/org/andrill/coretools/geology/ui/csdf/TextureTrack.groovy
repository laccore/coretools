package org.andrill.coretools.geology.ui.csdf

import org.andrill.coretools.geology.models.csdf.TextureInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.scene.TrackParameter
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.*

class TextureTrack extends AbstractFeatureTrack {
	private static final String DEFAULT_TITLE = "Texture"
	private static final PARAMETERS = [
		"draw-repeating" : new TrackParameter("draw-repeating", "Tile symbols", "<html>If enabled, draw symbol repeatedly, filling entire interval.<br/>If disabled, draw single symbol with whiskers at interval boundaries.</html>", TrackParameter.Type.BOOLEAN, "false"),
		"symbol-size" : new TrackParameter("symbol-size", "Symbol size", "Size, in pixels, of texture symbols.", TrackParameter.Type.INTEGER, "32"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer. (Footer available only in exported diagrams.)", TrackParameter.Type.STRING, DEFAULT_TITLE),
	]

	List<TrackParameter> getTrackParameters() {	return PARAMETERS.values() as List<TrackParameter> }
	
	def getFilter() { return { it instanceof TextureInterval } }
	List<Class> getCreatedClasses() { return [TextureInterval] }
	def getHeader() { getParameter("track-header", DEFAULT_TITLE) }
	def getFooter() { getParameter("track-footer", DEFAULT_TITLE) }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(TextureInterval.class, [:]), new ResizePolicy()])
	}

	@Override
	def getSymbolSize() {
		return (getParameter("symbol-size", PARAMETERS["symbol-size"].defaultValue) as Integer)
	}
}