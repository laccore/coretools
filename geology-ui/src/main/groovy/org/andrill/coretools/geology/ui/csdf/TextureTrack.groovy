package org.andrill.coretools.geology.ui.csdf

import org.andrill.coretools.geology.models.csdf.TextureInterval
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.*

class TextureTrack extends AbstractFeatureTrack {
	// Properties:
	//   * track-header:   string; the text or image to draw in the header
	//   * track-footer:   string; the text or image to draw in the footer
	//   * draw-outline:   boolean; draw outline of interval

	def getFilter() { return { it instanceof TextureInterval } }
	def getHeader() { "Texture" }
	def getFooter() { "Texture" }
	def getWidth()  { return 72 }
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(TextureInterval.class, [:]), new ResizePolicy()])
	}
}