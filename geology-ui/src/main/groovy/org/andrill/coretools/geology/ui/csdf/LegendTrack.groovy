
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
import org.andrill.coretools.scene.TrackParameter

class LegendTrack extends GeologyTrack {
	private static final String DEFAULT_TITLE = "Legend"
	private static final PARAMETERS = [
		"symbol-size" : new TrackParameter("symbol-size", "Symbol size", "Size, in pixels, of each legend entry pattern or icon.", TrackParameter.Type.INTEGER, "32"),
		"font-size" : new TrackParameter("font-size", "Font size", "Font size of each legend entry name.", TrackParameter.Type.INTEGER, "11"),
		"texture-scaling" : new TrackParameter("texture-scaling", "Texture scaling", "Scaling of legend entry patterns. Lower values zoom in, higher values zoom out.", TrackParameter.Type.FLOAT, "1"),
		"columns" : new TrackParameter("columns", "Columns", "Number of columns in legend layout.", TrackParameter.Type.INTEGER, "1"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer. (Footer available only in exported diagrams.)", TrackParameter.Type.STRING, DEFAULT_TITLE),
	]

	List<TrackParameter> getTrackParameters() {	return PARAMETERS.values() as List<TrackParameter> }	

	def getHeader() { getParameter("track-header", DEFAULT_TITLE) }
	def getFooter() { getParameter("track-footer", DEFAULT_TITLE) }
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
						if (entry && entry.scheme) {
							types.add(entry.scheme.type)
							entryMap[schemeProp.toString()] = entry
						}
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
		def rect = new Rectangle2D.Double(pt.x, pt.y, SYMBOL_SIZE, SYMBOL_SIZE)
		if (entry.image) {
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
