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
import java.awt.Color
import java.awt.Font
import java.text.DecimalFormat
import org.andrill.coretools.graphics.GraphicsContext

import org.andrill.coretools.geology.models.Occurrence

/**
 * A track to draw Annotation models and descriptions on other models.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class AnnotationTrack extends GeologyTrack {
	private static final int PADDING = 3
	private static final DecimalFormat DEC = new DecimalFormat("0.00")
	def getAnnotFont() {
		def fontsize = getParameter("font-size", "11")
		new Font("SanSerif", Font.PLAIN, Integer.parseInt(fontsize))
	}
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
		def font = annotFont
		def boldFont = font.deriveFont(Font.BOLD)
		def letterHeight = graphics.getStringBounds(font, "MMMMMggggg").getHeight()

		// wrap our description to the column width
		def text = [:]
		def rects = [:]
		while (true) {
			onpage.eachWithIndex { m, i ->
				text[m] = wrap(m, graphics, font, boldFont, bounds.width - 2*PADDING)
				def r = m instanceof Occurrence ? mrect_midpoint(m, bounds.x + PADDING, bounds.width - 2*PADDING) : mrect(m, bounds.x + PADDING, bounds.width - 2*PADDING)
				r.setSize((int) r.width, (int) ((text[m].size()) * letterHeight))
				rects[m] = r
			}
			// Occurrence annotations are placed at the vertical center of the Occurence's
			// range, meaning we can no longer assume elements of onpage are in depth order.
			// Now that we've gathered all annotation rects, sort list from top to bottom,
			// then adjust for overlaps as needed.
			def rectsToSort = rects.values().asList()
			rectsToSort.sort { it.minY }
			rectsToSort.eachWithIndex { curRect, i ->
				if (i > 0) {
					def prev = rectsToSort[i-1]
					int overlap = prev.maxY - curRect.minY
					if (overlap > 0) {
						curRect.translate(0, overlap)
					}
				}
			}
			// Will all annotations fit in available space? If not, decrement
			// font size and repeat until text fits or font is size 1,
			// at which point we have to give up.
			def totalTextHeight = rects.values().sum { it.height }
			def availableSpace = bounds.height
			if (totalTextHeight > availableSpace && font.size > 1) {
				font = font.deriveFont((float)(font.size - 1))
				boldFont = font.deriveFont(Font.BOLD)
			} else {
				break
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
		
		// draw description text
		onpage.each { m ->
			def x = rects[m].x
			def y = rects[m].y
			
			// draw label
			graphics.drawString(x, y, boldFont, m.toString() + " ")
			def offset = graphics.getStringBounds(boldFont, m.toString() + " ").getWidth()
			
			text[m].eachWithIndex { line, index ->
				if (index != 0) offset = 0 // offset for label on first line
				if (line) { graphics.drawString(x + offset, y, font, line) }
				y += letterHeight
			}
		}
	}
	
	private def wrap(m, graphics, font, boldFont, lineWidth) {
		def lines = []
		m.description.readLines().eachWithIndex { line, index ->
			// leave space for label to be drawn on first line
			def labelWidth = graphics.getStringBounds(boldFont, m.toString() + " ").getWidth()
			def w = (index == 0 ? labelWidth : 0)

			StringBuilder current = new StringBuilder()
			line.tokenize().each { _word ->
				def word = _word + " "
				def wordWidth = graphics.getStringBounds(font, word).getWidth() 
				if (w + wordWidth < lineWidth) {
					current.append(word)
					w += wordWidth
				} else {
					lines << current.toString()
					current = new StringBuilder()
					current.append(word)
					w = wordWidth
				}
			}
			lines << current.toString()
		}
		return lines
	}
}
