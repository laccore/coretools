package org.andrill.coretools.geology.ui

import java.awt.Color
import java.awt.geom.Rectangle2D
import org.andrill.coretools.geology.models.BeddingInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler


class BeddingTrack extends AbstractIntervalTrack {
	// Properties:
	//   * track-header:   the text or image to draw in the header
	//   * track-footer:   the text or image to draw in the footer

	def getFilter() { return { it instanceof BeddingInterval } }
	def getHeader() { "Bedding" }
	def getFooter() { "Bedding" }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(BeddingInterval.class, [:]), new ResizePolicy()])
	}
}