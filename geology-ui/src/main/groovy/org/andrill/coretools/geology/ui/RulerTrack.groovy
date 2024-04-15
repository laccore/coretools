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
	//   * track-header:   string; the text or image to draw in the header
	//   * track-footer:   string; the text or image to draw in the footer
	//   * label-step:     int; frequency of labeled hashes
	//   * hash-step:     int; frequency of unlabeled hashes
	//   * draw-left-hash: boolean; draw left hashes
	//   * draw-right-hash: boolean; draw right hashes
	//   * scale-labels: boolean; grow/shrink labels to fit available space

	def getHeader() { units }
	def getFooter() { units }

	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		this.bounds = bounds
		
		// figure out our clip
		def clip = clip(bounds, graphics.clip, 100 / scale)

		// figure out our hashing
		def lStepParam = Integer.parseInt(getParameter("label-step", "40"))
		def hStepParam = Integer.parseInt(getParameter("hash-step", "10"))
		def drawLeftHash = Boolean.parseBoolean(getParameter("draw-left-hash", "true"))
		def drawRightHash = Boolean.parseBoolean(getParameter("draw-right-hash", "true"))
		def scaleLabels = Boolean.parseBoolean(getParameter("scale-labels", "false"))

		final int labelHashWidth = 10
		final int hashWidth = 5

		def lStep = calculateSpacing(lStepParam)
		def hStep = calculateSpacing(hStepParam)
		def hStart = new BigDecimal((Math.floor(clip.minY / lStep) * lStep)).setScale(2, RoundingMode.HALF_UP)
		def hEnd = new BigDecimal((Math.ceil(clip.maxY / lStep) * lStep)).setScale(2, RoundingMode.HALF_UP)

		def labelFont = font
		if (scaleLabels) {
			final String testLabel = "0000"
			labelFont = calculateLabelSize(graphics, bounds.width - (labelHashWidth + hashWidth + 5), testLabel)
		}

		// draw our hashes and labels
		int zeroes = Math.max(0, hStep.scale() - 3)
		DecimalFormat dec = new DecimalFormat((lStep < 1) ? "0.${ '0' * zeroes}0#" : "0")
		for (def i = hStart; i <= hEnd; i += hStep) {
			def pt = pts(i, bounds)
			if (i / lStep == (int) (i / lStep) && pt > bounds.minY && pt < bounds.maxY) {
				// labels
				graphics.drawStringCenter(bounds.minX, pt, bounds.width, 1, labelFont, dec.format(i))
				if (drawLeftHash) { graphics.drawLine(bounds.x, pt, bounds.x + labelHashWidth, pt) }
				if (drawRightHash) { graphics.drawLine(bounds.maxX - labelHashWidth, pt, bounds.maxX, pt) }
			} else {
				// hash marks
				if (drawLeftHash) { graphics.drawLine(bounds.x, pt, bounds.x + hashWidth, pt) }
				if (drawRightHash) { graphics.drawLine(bounds.maxX - hashWidth, pt, bounds.maxX, pt) }
			}
		}
	}

	def growFontTest = { gc, font, wid, label ->
		gc.getStringBounds(font, label).width < wid
	}

	def shrinkFontTest = { gc, font, wid, label ->
		gc.getStringBounds(font, label).width > wid
	}

	private def calculateLabelSize(GraphicsContext graphics, double availableWidth, String label) {
		if (growFontTest(graphics, font, availableWidth, label)) {
			return scaleFont(graphics, availableWidth, label, 1.0f, growFontTest)
		} else if (shrinkFontTest(graphics, font, availableWidth, label)) {
			return scaleFont(graphics, availableWidth, label, -1.0f, shrinkFontTest)
		}
		return font
	}

	// Adjust default font by incr until it no longer passes input test. A derived font
	// with the last passing font size is returned.
	private scaleFont(GraphicsContext gc, double wid, String label, float incr, Closure test) {
		def curFont = this.font
		float fontSize = font.getSize()
		while ((fontSize + incr > 0.0f) && test(gc, curFont.deriveFont((float)(fontSize + incr)), wid, label)) {
			fontSize += incr
		}
		return curFont.deriveFont(fontSize)
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