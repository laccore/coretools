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

import java.awt.geom.Rectangle2D

import org.andrill.coretools.dis.models.SectionUnit
import org.andrill.coretools.geology.models.SchemeRef;
import org.andrill.coretools.geology.ui.GeologyTrack 
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.scheme.SchemeManager

import com.google.inject.Inject

class ComponentTrack extends GeologyTrack {
	@Inject 
	SchemeManager schemes
	def getFilter()  { return { it instanceof SectionUnit } }
	def getHeader()  { "Component" }
	def getFooter()  { "Component" }
	def getWidth()   { return 72 }
	def components(m) {
		String prop = getParameter('component-property', 'component')
		def components = []
		def keys =  m.properties.findAll { it.key.startsWith(prop) }.collect { it.key }.sort()
		keys.each { p ->
			def value = m.getProperty(p)
			if (value) {
				components << ((value instanceof SchemeRef) ? value.code : value.toString())
			}
		}
		components
	}
	
	void renderModel(Model m, GraphicsContext graphics, Rectangle2D bounds) {
		def r = getModelBounds(m)
		
		// draw our text
		def components = components(m)
		if (components) {
			def words = components.join(' and ').split(' ')
			int y = r.y
			int height = 0
			int width = 6
			StringBuilder line = new StringBuilder()
			words.each { word ->
				def wb = graphics.getStringBounds(font, word + " ")
				height = wb.height
				if (width + wb.width < bounds.width) {
					line.append(word + " ")
					width += wb.width
				} else {
					String text = line.toString().trim()
					if (text) {
						def lb = graphics.getStringBounds(font, text)
						if (lb.width > bounds.width) {
							int cc = bounds.width / (lb.width / text.length())
							text = text[0..(cc - 3)] + "..."
						}
						if (y + height <= r.maxY) graphics.drawString(r.x + 3, y, font, text)
						y += height
						width = 6 + wb.width
					}
					line = new StringBuilder(word + " ")
				}
			}
			if (line.toString()) {
				String text = line.toString().trim()
				if (text) {
					def lb = graphics.getStringBounds(font, text)
					if (lb.width > bounds.width) {
						int cc = bounds.width / (lb.width / text.length())
						text = text[0..(cc - 3)] + "..."
					}
					if (y + height <= r.maxY) graphics.drawString(r.x + 3, y, font, text)
				}
				y += height
			}
		}
		
		// draw our contacts
		//graphics.drawLine(r.minX, r.minY, r.maxX, r.minY)
		graphics.drawLine(r.minX, r.maxY, r.maxX, r.maxY)
	}
}