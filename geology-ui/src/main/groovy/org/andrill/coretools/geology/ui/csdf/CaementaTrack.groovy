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
import org.andrill.coretools.scene.TrackParameter
import org.andrill.coretools.scene.event.*
import org.andrill.coretools.geology.ui.*
import org.andrill.coretools.geology.models.*
import org.andrill.coretools.geology.models.csdf.Caementa

class CaementaTrack extends AbstractFeatureTrack {
	private static final String DEFAULT_TITLE = "Caementa"
	private static final PARAMETERS = [
		"draw-repeating" : new TrackParameter("draw-repeating", "Tile symbols", "<html>If enabled, draw symbol repeatedly, filling entire interval.<br/>If disabled, draw single symbol with whiskers at interval boundaries.</html>", TrackParameter.Type.BOOLEAN, "false"),
		"symbol-size" : new TrackParameter("symbol-size", "Symbol size", "Size, in pixels, of feature symbols.", TrackParameter.Type.INTEGER, "32"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer. (Footer available only in exported diagrams.)", TrackParameter.Type.STRING, DEFAULT_TITLE),
	]

	List<TrackParameter> getTrackParameters() {	return PARAMETERS.values() as List<TrackParameter> }

	def getHeader() { getParameter("track-header", DEFAULT_TITLE) }
	def getFooter() { getParameter("track-footer", DEFAULT_TITLE) }
	def getWidth()  { return 128 }
	def getFilter() { return { it instanceof Caementa } }
	List<Class> getCreatedClasses() { return [Caementa] }
	protected SceneEventHandler createHandler() { 
		new DefaultTrackEventHandler(this, [new CreatePolicy(Caementa.class, [:], getSymbolSize()), new ResizePolicy(), new MovePolicy()])
	}
}