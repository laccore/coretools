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

import java.lang.StringBuilder
import java.awt.geom.Rectangle2D
import java.text.DecimalFormat
import org.andrill.coretools.graphics.GraphicsContext
/**
 * A track to draw Annotation models and descriptions on other models.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class AnnotationTrack extends GeologyTrack {
	private static final int PADDING = 3
	private static final DecimalFormat DEC = new DecimalFormat("0.00")
	def getHeader() { "Description" }
	def getFooter() { "Description" }
	def getWidth()  { return 128 }
	def getFilter() { return { it.hasProperty('description') && it.description } }

	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		this.bounds = bounds

		// figure out the models on the page
		def clip = clip(bounds, graphics.clip)
		// TODO: fixup models reference
		def onpage = models.findAll{ onpage(it, clip) }
		if (!onpage) { return }
		
		// calculate some string metrics
		def font = font
		def stringBounds = graphics.getStringBounds(font, "MMMMMggggg")
		def letterHeight = stringBounds.height
		def letterWidth = stringBounds.width / 10
		
		// wrap our description to the column width
		def text = [:]
		def rects = [:]
		onpage.eachWithIndex { m, i -> 
			text[m] = wrap(m, letterWidth, bounds.width - 2*PADDING)
			def r = mrect(m, bounds.x + PADDING, bounds.width - 2*PADDING)
			r.setSize((int) r.width, (int) ((text[m].size() + 1) * letterHeight))
			rects[m] = r
			if (i > 0) {
				def prev = rects[onpage[i-1]]
				int overlap = prev.maxY - r.minY
				if (overlap > 0) {
					r.translate(0, overlap)
				}
			}
		}
		
		// layout our rectangles so they don't hang off the end of the page
		int overlap = rects[onpage[-1]].maxY - bounds.maxY
		for (int i = onpage.size() - 1; overlap > 0 && i >= 0; i--) {
			int space
			if (i == 0) {
				space = rects[onpage[i]].minY - bounds.minY
			} else {
				space = rects[onpage[i]].minY - rects[onpage[i-1]].maxY
			}
			if (space > 0) {
				int adjust = Math.min(overlap, space)
				for (j in i..<onpage.size()) {
					rects[onpage[j]].translate(0, -adjust)
				}
				overlap -= adjust
			}
		}
		
		// draw the text
		onpage.each { m ->
			def x = rects[m].x
			def y = rects[m].y
			text[m].each { line ->
				if (line) { graphics.drawString(x, y, font, line) }
				y += letterHeight
			}
		}
	}
	
	private def wrap(m, letterWidth, lineWidth) {
		def lines = []
		// label
		lines << m.toString()
		
		// description
		m.description.readLines().each { line ->
			StringBuilder current = new StringBuilder()
			def w = 0
			line.tokenize().each { word ->
				if (w + word.length() * letterWidth < lineWidth) {
					current.append(word + " ")
					w += (word.length() + 1) * letterWidth
				} else {
					lines << current.toString()
					current = new StringBuilder()
					current.append(word + " ")
					w = (word.length() + 1) * letterWidth
				}
			}
			lines << current.toString()
		}
		return lines
	}
}
