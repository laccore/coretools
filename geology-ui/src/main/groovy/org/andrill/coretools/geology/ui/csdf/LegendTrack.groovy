
package org.andrill.coretools.geology.ui.csdf

import java.awt.Point
import java.awt.geom.Rectangle2D
import java.awt.geom.Point2D
import java.awt.TexturePaint
import java.awt.Color
import java.awt.Font
import java.awt.geom.AffineTransform

import org.andrill.coretools.geology.models.*
import org.andrill.coretools.geology.models.csdf.*
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.graphics.fill.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.scheme.SchemeEntry
import org.andrill.coretools.geology.ui.GeologyTrack
import org.andrill.coretools.scene.TrackParameter

import org.andrill.coretools.misc.util.StringUtils

class LegendTrack extends GeologyTrack {
	private static final String DEFAULT_TITLE = "Legend"
	private static final PARAMETERS = [
		"symbol-size" : new TrackParameter("symbol-size", "Symbol size", "Size, in pixels, of each legend entry pattern or icon.", TrackParameter.Type.INTEGER, "32"),
		"font-size" : new TrackParameter("font-size", "Font size", "Font size of each legend entry name.", TrackParameter.Type.INTEGER, "11"),
		"heading-font-size" : new TrackParameter("heading-font-size", "Heading font size", "Font size of entry type headings.", TrackParameter.Type.INTEGER, "13"),
		"texture-scaling" : new TrackParameter("texture-scaling", "Texture scaling", "Scaling of legend entry patterns. Lower values zoom in, higher values zoom out.", TrackParameter.Type.FLOAT, "1"),
		"columns" : new TrackParameter("columns", "Columns", "Number of columns in legend layout.", TrackParameter.Type.INTEGER, "1"),
		"track-header" : new TrackParameter("track-header", "Header text", "Text to display in track header.", TrackParameter.Type.STRING, DEFAULT_TITLE),
		"track-footer" : new TrackParameter("track-footer", "Footer text", "Text to display in track footer. (Footer available only in exported diagrams.)", TrackParameter.Type.STRING, DEFAULT_TITLE),
	]

	List<TrackParameter> getTrackParameters() {	return PARAMETERS.values() as List<TrackParameter> }	

	def getHeader() { getParameter("track-header", DEFAULT_TITLE) }
	def getFooter() { getParameter("track-footer", DEFAULT_TITLE) }
	def getWidth()  { return 128 }
	def getFilter() { return { true } }

	private Font labelFont = null
	private Font headingFont = null
	private int SYMBOL_SIZE = 32
	final private int INTER_ENTRY_SPACING = 4 // vertical space between entries
	final private int INTER_ENTRYTYPE_SPACING = 10 // space after each subheading group
	final private int MARGIN = 2 // space between edge of bounds and start/end of entry
	final private int PADDING = 4 // space between entry symbol and label

    @Override
	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		validate()
		
		this.SYMBOL_SIZE = Integer.parseInt(getParameter("symbol-size", "32"))
		if (hasParameter("font-size")) {
			this.labelFont = font.deriveFont((float)Integer.parseInt(getParameter("font-size", "11")))
		} else {
			this.labelFont = font
		}
		this.headingFont = font.deriveFont(Font.BOLD, (float)Integer.parseInt(getParameter("heading-font-size", "13")))

		this.bounds = bounds
		def clip = clip(bounds, graphics.clip)

		LinkedHashMap<String, List<SchemeEntry>> entries = gatherEntries()
		renderEntries(graphics, entries)
	}

	private LinkedHashMap<String, List<SchemeEntry>> gatherEntries() {
		def entryMap = [:]
		def entryTypes = new java.util.HashSet<String>() // scheme types: lithology, grainsize, bedding, etc.
		getModels().each { model ->
			['scheme', 'lithology'].each { propName ->
				if (model.hasProperty(propName)) {
					final schemeProp = model."$propName"
					if (schemeProp && !entryMap.containsKey(schemeProp.toString())) {
						def entry = getSchemeEntry(schemeProp.scheme, schemeProp.code)
						println "${schemeProp.code}: ${entry.getName()}"
						if (entry && entry.scheme) {
							entryTypes.add(entry.scheme.type)
							entryMap[schemeProp.toString()] = entry
						}
					}
				}
			}
		}

		final LinkedHashMap<String, String> SCHEME_TYPE_TO_PRETTY_NAME = [
			"lithology" : "Lithology",
			"grainsize" : "Grain Size",
			"texture" : "Texture",
			"bedding" : "Bedding",
			"features" : "Features",
			"symbol" : "Symbol",
			'caementa': 'Caementa',
			'mortar': 'Mortar',
			'matrix': 'Matrix',
			'pores': 'Pores',
			'fractures': 'Fractures',
			'discontinuity': 'Discontinuity'
		]

		LinkedHashMap<String, List<SchemeEntry>> entries = [:]
		entryTypes.each { curType ->
			def sortedTypedEntries = entryMap.values().findAll { it.scheme.type.equals(curType) }.sort { it.name } // sort entries by name
			entries.put(SCHEME_TYPE_TO_PRETTY_NAME[curType], sortedTypedEntries)
		}

		return entries
	}

	// Return list of entry type names in the same order as the scene's tracks.
	private getEntryTypeOrder() {
		def typeOrder = []
		this.scene.tracks.each { t ->
			if (t.getCreatedClasses().size() > 0) {
				def typeName = StringUtils.humanizeModelName(t.getCreatedClasses().get(0).getSimpleName())

				// fixup model names that differ from their user-facing names
				if (typeName == "Feature") {
					typeName = "Features"
				} else if (typeName == "Interval") {
					typeName = "Lithology"
				} else if (typeName == "Occurrence") {
					typeName = "Symbol"
				}
				typeOrder.add(typeName)
			}
		}
		return typeOrder
	}

	private void renderEntries(GraphicsContext graphics, LinkedHashMap<String, List<SchemeEntry>> entries) {
		final int COLS = 1 // number of columns in which to draw legend entries
		final int availableWidth = ((this.bounds.width - (MARGIN*2)) / COLS).intValue()
		int col = 0
		int x = bounds.x + MARGIN
		int y = bounds.y + MARGIN

		final entryTypeOrder = getEntryTypeOrder()
		boolean showEntryTypeHeadings = entries.size() > 1
		entryTypeOrder.each { entryTypeHeading ->
			if (entryTypeHeading in entries.keySet()) {
				def entryList = entries.get(entryTypeHeading)
				if (showEntryTypeHeadings) {
					graphics.drawString(x, y, this.headingFont, entryTypeHeading)
					y += graphics.getStringBounds(this.headingFont, "W").getHeight() + INTER_ENTRYTYPE_SPACING
				}

				entryList.each { 
					renderEntry(graphics, new Point2D.Double(x, y), availableWidth, it)
					col = (col + 1) % COLS
					if (col == 0) {
						x = bounds.x + MARGIN
						y += SYMBOL_SIZE + INTER_ENTRY_SPACING
					} else {
						x += availableWidth
					}
				}

				if (showEntryTypeHeadings) { y += INTER_ENTRYTYPE_SPACING }
			}

			if (COLS > 1 && col != 0) {
				col = 0
				x = bounds.x + MARGIN
				y += SYMBOL_SIZE + INTER_ENTRY_SPACING
			}
		}
	}

	private void renderEntry(GraphicsContext graphics, Point2D pt, int availableWidth, SchemeEntry entry) {
		// ensure each filled entry draws from upper-left of its texture instead of 
		// wherever the entry happens to be positioned in the legend
		graphics.pushTransform(AffineTransform.getTranslateInstance(pt.x, pt.y))

		def rect = new Rectangle2D.Double(0, 0, SYMBOL_SIZE, SYMBOL_SIZE)
		if (entry.image) {
			if (['bedding', 'features', 'symbol', 'texture', 'caementa', 'mortar', 'matrix', 'pores', 'fractures', 'discontinuity'].contains(entry.scheme.type)) { // untiled image
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
		final int lineHeight = graphics.getStringBounds(this.labelFont, "W").getHeight()
		final int totalHeight = lines.size() * lineHeight
		final int vCenterOffset = (totalHeight < SYMBOL_SIZE) ? (SYMBOL_SIZE - totalHeight) / 2 : 0
		lines.eachWithIndex { line, index ->
			graphics.drawString(new Point2D.Double(MARGIN + SYMBOL_SIZE + PADDING, (index * lineHeight) + vCenterOffset), this.labelFont, line)
		}

		graphics.popTransform()
	}

	private Fill getFill(SchemeEntry entry) {
		final float textureScaling = Double.parseDouble(getParameter("texture-scaling", "1.0"))
		Color color = entry.color
		URL image = entry.imageURL
		if (image && color) {
			return new MultiFill(new ColorFill(color), new TextureFill(image, textureScaling))
		} else if (image) {
			return new TextureFill(image, textureScaling)
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
