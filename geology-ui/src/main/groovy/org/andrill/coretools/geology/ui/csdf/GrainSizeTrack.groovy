package org.andrill.coretools.geology.ui.csdf

import org.andrill.coretools.geology.models.csdf.GrainSizeInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.*

class GrainSizeTrack extends AbstractIntervalTrack {
	// Properties:
	//   * track-header:   string; the text or image to draw in the header
	//   * track-footer:   string; the text or image to draw in the footer
	//   * draw-outline:   boolean; draw outline of interval

	def getFilter() { return { it instanceof GrainSizeInterval } }
	def getHeader() { "Grain Size" }
	def getFooter() { "Grain Size" }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(GrainSizeInterval.class, [:]), new ResizePolicy()])
	}
}