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
package org.andrill.coretools.dis.ui


import java.awt.Color
import java.awt.geom.Rectangle2D

import org.andrill.coretools.geology.ui.GeologyTrack
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.scene.event.SceneEventHandler
import org.andrill.coretools.scene.event.DefaultTrackEventHandler
import org.andrill.coretools.geology.ui.event.CreatePolicy
import org.andrill.coretools.geology.ui.event.ResizePolicy
import org.andrill.coretools.dis.models.SectionUnit

class LithologyTrack extends GeologyTrack {
	def getFilter()  { return { it instanceof SectionUnit } }
	def getHeader()  { "Lithology" }
	def getFooter()  { "Lithology" }
	def getWidth()   { return 72 }
	def lithology(m) {
		String prop = getParameter('lithology-property', 'lithology')
		return m.getProperty(prop)
	}
	protected SceneEventHandler createHandler() {
		new DefaultTrackEventHandler(this, [new CreatePolicy(SectionUnit.class, [:]), new ResizePolicy()])
	}
	
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def r = getModelBounds(m)
		
		// fill our rectangle with the pattern
		graphics.setFill(getFill(m))
		graphics.fillRectangle(r)
		
		// draw our contacts
		graphics.drawLine(r.minX, r.minY, r.maxX, r.minY)
		graphics.drawLine(r.minX, r.maxY, r.maxX, r.maxY)
	}
	
	private Fill getFill(m) {
		def lithology = lithology(m)
		if (lithology) {
			def entry = getSchemeEntry(lithology?.scheme, lithology?.code)
			if (!entry) { return null }
			
			Color color = entry.color
			URL image = entry.imageURL
			if (image && color) {
				return new MultiFill(new ColorFill(color), new TextureFill(image))
			} else if (image) {
				return new TextureFill(image)
			} else if (color) {
				return new ColorFill(color)
			}
		} else {
			return new ColorFill(Color.white)
		}
	}
	
	protected String getModelLabel(m, pt) {
		def lithology = lithology(m)
		def label = "<br/><b>$m</b>"
		if (lithology) {
			def entry = getSchemeEntry(lithology.scheme, lithology.code)
			if (entry) { label += "\n${entry.name}" }
		}
		if (m?.description) {
			label += "\n${m.description}"
		}
		return label
	}
}