package org.andrill.coretools.geology.ui

import java.awt.Color
import java.awt.geom.Rectangle2D
import org.andrill.coretools.geology.models.CSDFGrainSizeInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model;
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler


class CSDFGrainSizeTrack extends AbstractIntervalTrack {
	// Properties:
	//   * track-header:   the text or image to draw in the header
	//   * track-footer:   the text or image to draw in the footer

	def getFilter() { return { it instanceof CSDFGrainSizeInterval } }
	def getHeader() { "Grain Size" }
	def getFooter() { "Grain Size" }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(CSDFGrainSizeInterval.class, [:]), new ResizePolicy()])
	}
}