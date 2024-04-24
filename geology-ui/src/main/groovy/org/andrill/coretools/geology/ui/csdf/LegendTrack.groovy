
package org.andrill.coretools.geology.ui.csdf

import java.awt.Point
import java.awt.geom.Rectangle2D
import java.awt.geom.Point2D
import java.awt.TexturePaint
import java.awt.Color
import java.awt.Font

import org.andrill.coretools.geology.models.*
import org.andrill.coretools.geology.models.csdf.*
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.scheme.SchemeEntry
import org.andrill.coretools.geology.ui.GeologyTrack

class LegendTrack extends GeologyTrack {
	// Properties:
	//   * track-header:   string; the text or image to draw in the header
	//   * track-footer:   string; the text or image to draw in the footer
	//   * symbol-size:   integer; width/height at which to draw pattern/icon
	//   * font-size:     integer; label font size

	def getHeader() { "Legend" }
	def getFooter() { "Legend" }
	def getWidth()  { return 32 }
	def getFilter() { return { true } }

	private Font labelFont = null
	private int symbolSize = 32
	final private int INTER_ENTRY_SPACING = 4 // vertical space between entries
	final private int MARGIN = 2 // space between edge of bounds and start/end of entry
	final private int PADDING = 4 // space between entry symbol and label

    @Override
	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		validate()
		
		this.symbolSize = Integer.parseInt(getParameter("symbol-size", "32"))
		if (hasParameter("font-size")) {
			this.labelFont = font.deriveFont((float)Integer.parseInt(getParameter("font-size", "11")))
		} else {
			this.labelFont = font
		}

		this.bounds = bounds
		def clip = clip(bounds, graphics.clip)

		def entries = gatherEntries()
		int y = 2
		entries.each {
			renderEntry(graphics, new Point2D.Double(bounds.x + MARGIN, y), it)
			y += symbolSize + INTER_ENTRY_SPACING
		}
	}

	private List<SchemeEntry> gatherEntries() {
		def entryMap = [:]
		def types = new java.util.HashSet<String>()
		getModels().each { model ->
			['scheme', 'lithology'].each { propName ->
				if (model.hasProperty(propName)) {
					final schemeProp = model."$propName"
					if (schemeProp && !entryMap.containsKey(schemeProp.toString())) {
						def entry = getSchemeEntry(schemeProp.scheme, schemeProp.code)
						types.add(entry.scheme.type)
						entryMap[schemeProp.toString()] = entry
					}
				}			
			}
		}
		def entries = []
		types.each { curType ->
			entries.addAll(entryMap.values().findAll { it.scheme.type.equals(curType) }.sort { it.name })
		}

		return entries
	}

	void renderEntry(GraphicsContext graphics, Point2D pt, SchemeEntry entry) {
		if (entry.image) {
			def rect = new Rectangle2D.Double(pt.x, pt.y, symbolSize, symbolSize)
			if (['features', 'symbol'].contains(entry.scheme.type)) { // untiled image
				graphics.drawImage(rect, entry.imageURL)
			} else { // tile image
				graphics.setFill(getFill(entry))
				graphics.fillRectangle(rect)
				graphics.setLineColor(Color.BLACK)
				graphics.drawRectangle(rect)
			}
		} else { // no image, draw empty rectangle
			graphics.setLineColor(Color.BLACK)
			graphics.drawRectangle(rect)
		}

		final int labelWidth = this.bounds.width - (PADDING*2 + symbolSize)
		def lines = wrap(graphics, entry.name, this.labelFont, labelWidth)
		final int textHeight = graphics.getStringBounds(this.labelFont, "W").getHeight()
		lines.eachWithIndex { line, index ->
			graphics.drawString(new Point2D.Double(pt.x + MARGIN + symbolSize + PADDING, pt.y + (index * textHeight)), this.labelFont, line)
		}
	}

	private Fill getFill(SchemeEntry entry) {
		Color color = entry.color
		URL image = entry.imageURL
		if (image && color) {
			return new MultiFill(new ColorFill(color), new TextureFill(image))
		} else if (image) {
			return new TextureFill(image)
		} else if (color) {
			return new ColorFill(color)
		}
	}

	private def wrap(GraphicsContext graphics, String str, Font font, int lineWidth) {
		def lines = []
		str.readLines().eachWithIndex { line, index ->
			def w = 0

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
