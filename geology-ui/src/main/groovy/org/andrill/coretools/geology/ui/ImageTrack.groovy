/*
 * Copyright (c) Josh Reed, 2009.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andrill.coretools.geology.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.geom.Rectangle2D

import org.andrill.coretools.geology.models.Image
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.util.ImageInfo
import org.andrill.coretools.model.Model
import org.andrill.coretools.scene.TrackParameter

/**
 * A track to draw Image models.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class ImageTrack extends GeologyTrack {
	// Properties:
	//   * filter-group:   string; only show Images of a specific group
	private static final String DEFAULT_TITLE = "Images"
	private static final PARAMETERS = [
		"embed-image" : new TrackParameter("embed-image", "Embed image", "<html>If enabled, draws raw, unscaled image. Otherwise, draws scaled image.<br>Typically used in diagram export to maximize image quality.</html>", TrackParameter.Type.BOOLEAN, "false"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer. (Footer available only in exported diagrams.)", TrackParameter.Type.STRING, DEFAULT_TITLE),
	]
	List<TrackParameter> getTrackParameters() { return PARAMETERS.values() as List<TrackParameter> }

	def getHeader() { DEFAULT_TITLE }
	def getFooter() { DEFAULT_TITLE }
	def getWidth()  {
		// TODO fixup models reference
		if (models.size() == 0) { return 72 }
		double physWidth = physWidth(models[models.size()/2 as int]) 
		return (physWidth == 0.0) ? 72 : scale(physWidth)
	}
	def getFilter() {
		String filter = getParameter("filter-group", null)
		if (filter) {
			return { it instanceof Image && it?.group == filter }
		} else {
			return { it instanceof Image }
		}
	}

	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def param = getParameter("embed-image", PARAMETERS['embed-image'].defaultValue)
		if (param.equals("true")) {
			graphics.embedImage(getModelBounds(m), m.path)
		} else {
			graphics.drawImage(getModelBounds(m), m.path)
		}
	}
	
	private double physWidth(model) {
		def physWidth = 0.0
		try {
			model?.path?.withInputStream { stream ->
				ImageInfo ii = new ImageInfo()
				ii.setInput(stream)
				if (ii.check()) {
					physWidth = ((double) ii.width / ((double) ii.height / (mmax(model) - mmin(model))))
				}
			}
		} catch (e) {
			// ignore
		}
		return physWidth
	}
}
