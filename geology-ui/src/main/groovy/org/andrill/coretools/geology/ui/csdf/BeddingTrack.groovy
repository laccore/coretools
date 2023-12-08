package org.andrill.coretools.geology.ui.csdf

import org.andrill.coretools.geology.models.csdf.BeddingInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.*

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