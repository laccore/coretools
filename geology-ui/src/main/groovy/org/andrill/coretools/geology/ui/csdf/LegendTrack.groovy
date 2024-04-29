
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
	//   * texture-scaling: double; scaling of textured images in scheme entries, higher shows more detail
	//   * columns:       integer; number of columns in which to draw legend entries

	def getHeader() { "Legend" }
	def getFooter() { "Legend" }
	def getWidth()  { return 32 }
	def getFilter() { return { true } }

	private Font labelFont = null
	private int SYMBOL_SIZE = 32
	final private int INTER_ENTRY_SPACING = 4 // vertical space between entries
	final private int MARGIN = 2 // space between edge of bounds and start/end of entry
	final private int PADDING = 4 // space between entry symbol and label
	private double TEXTURE_SCALING = 1.0
	private int COLS = 1 // number of columns in which to draw legend entries

    @Override
	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		validate()
		
		this.SYMBOL_SIZE = Integer.parseInt(getParameter("symbol-size", "32"))
		if (hasParameter("font-size")) {
			this.labelFont = font.deriveFont((float)Integer.parseInt(getParameter("font-size", "11")))
		} else {
			this.labelFont = font
		}

		this.TEXTURE_SCALING = Double.parseDouble(getParameter("texture-scaling", "1.0"))
		this.COLS = Integer.parseInt(getParameter("columns", "1"))

		this.bounds = bounds
		def clip = clip(bounds, graphics.clip)

		def entries = gatherEntries()
		renderEntries(graphics, entries)
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

	private void renderEntries(GraphicsContext graphics, List<SchemeEntry> entries) {
		final int availableWidth = ((this.bounds.width - (MARGIN*2)) / COLS).intValue()
		int col = 0
		int x = bounds.x + MARGIN
		int y = bounds.y + MARGIN
		entries.each {
			renderEntry(graphics, new Point2D.Double(x, y), availableWidth, it)
			col = (col + 1) % COLS
			if (col == 0) {
				x = bounds.x + MARGIN
				y += SYMBOL_SIZE + INTER_ENTRY_SPACING
			} else {
				x += availableWidth
			}
		}		
	}

	private void renderEntry(GraphicsContext graphics, Point2D pt, int availableWidth, SchemeEntry entry) {
		if (entry.image) {
			def rect = new Rectangle2D.Double(pt.x, pt.y, SYMBOL_SIZE, SYMBOL_SIZE)
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

		final int labelWidth = availableWidth - (PADDING*2 + SYMBOL_SIZE)
		def lines = wrap(graphics, entry.name, this.labelFont, labelWidth)
		final int textHeight = graphics.getStringBounds(this.labelFont, "W").getHeight()
		lines.eachWithIndex { line, index ->
			graphics.drawString(new Point2D.Double(pt.x + MARGIN + SYMBOL_SIZE + PADDING, pt.y + (index * textHeight)), this.labelFont, line)
		}
	}

	private Fill getFill(SchemeEntry entry) {
		Color color = entry.color
		URL image = entry.imageURL
		if (image && color) {
			return new MultiFill(new ColorFill(color), new TextureFill(image, TEXTURE_SCALING))
		} else if (image) {
			return new TextureFill(image, TEXTURE_SCALING)
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
