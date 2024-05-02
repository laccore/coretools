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

class FeatureTrack extends AbstractFeatureTrack {
	// Properties:
	//   * symbol-size:    integer; the width of the symbol
	//   * draw-repeating: boolean; draw the symbols repeating instead of whiskers
	//   * track-header:   string; the text or image to draw in the header
	//   * track-footer:   string; the text or image to draw in the footer
	
	def getHeader() { "Features" }
	def getFooter() { "Features" }
	def getWidth()  { return 96 }
	def getFilter() { return { it instanceof Feature } }
	List<Class> getCreatedClasses() { return [Feature] }
	protected SceneEventHandler createHandler() { 
		new DefaultTrackEventHandler(this, [new CreatePolicy(Feature.class, [:], symbolSize), new ResizePolicy(), new MovePolicy()])
	}
}