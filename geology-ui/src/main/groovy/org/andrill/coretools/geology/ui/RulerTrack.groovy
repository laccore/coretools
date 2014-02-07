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

import java.awt.Dimension
import java.awt.geom.Dimension2D
import java.awt.geom.Rectangle2D
import java.math.RoundingMode
import java.text.DecimalFormat

import org.andrill.coretools.graphics.GraphicsContext

/**
 * A track to draw a depth/height ruler.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class RulerTrack extends GeologyTrack {
	// Properties:
	//   * track-header:   the text or image to draw in the header
	//   * track-footer:   the text or image to draw in the footer

	def getHeader() { units }
	def getFooter() { units }

	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		this.bounds = bounds
		
		// figure out our clip
		def clip = clip(bounds, graphics.clip, 100 / scale)

		// figure out our hashing
		def lStep = calculateSpacing(40)
		def hStep = calculateSpacing(10)
		def hStart = new BigDecimal((Math.floor(clip.minY / lStep) * lStep)).setScale(2, RoundingMode.HALF_UP)
		def hEnd = new BigDecimal((Math.ceil(clip.maxY / lStep) * lStep)).setScale(2, RoundingMode.HALF_UP)

		// draw our hashes and labels
		int zeroes = Math.max(0, hStep.scale() - 3)
		DecimalFormat dec = new DecimalFormat((lStep < 1) ? "0.${ '0' * zeroes}0#" : "0")
		for (def i = hStart; i <= hEnd; i += hStep) {
			def pt = pts(i, bounds)
			if (i / lStep == (int) (i / lStep) && pt > bounds.minY && pt < bounds.maxY) {
				// labels
				graphics.drawStringCenter(bounds.minX, pt, bounds.width, 1, font, dec.format(i))
				graphics.drawLine(bounds.x, pt, bounds.x + 10, pt)
				graphics.drawLine(bounds.maxX - 10, pt, bounds.maxX, pt)
			} else {
				// hash marks
				graphics.drawLine(bounds.x, pt, bounds.x + 5, pt)
				graphics.drawLine(bounds.maxX - 5, pt, bounds.maxX, pt)
			}
		}
	}

	private def calculateSpacing(pts) {
		def scale = getScale() as BigDecimal
		def multiplier = Math.pow(10, Math.floor(Math.log10(pts / scale))) as BigDecimal
		def hash
		if (1 * scale * multiplier > pts) {
			hash = 1 * multiplier
		} else if (2 * scale * multiplier > pts) {
			hash = 2 * multiplier
		} else if (5 * scale * multiplier > pts) {
			hash = 5 * multiplier
		} else {
			hash = 10 * multiplier
		}
		return hash
	}
}