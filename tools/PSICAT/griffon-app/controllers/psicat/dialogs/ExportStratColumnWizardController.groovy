/*
 * Copyright (c) Brian Grivna, 2015.
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
package psicat.dialogs

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import au.com.bytecode.opencsv.CSVReader

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import org.andrill.coretools.Platform;
import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.geology.ui.Scale
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeEntry
import org.andrill.coretools.model.scheme.SchemeManager

import psicat.util.*

class ExportStratColumnWizardController {
    def model
    def view
	
	private static Logger logger = LoggerFactory.getLogger(ExportStratColumnWizardController.class)
	
	def PAGE_HEIGHT = 72 * 60 // 60" 
	def PAGE_WIDTH = 612 // 8.5"
	def MARGIN = 36 // 1/2" margin at 72dpi
	def HEADER_HEIGHT = 108 // space for grain size labels, ruler units, etc.
	def HEADER_Y = MARGIN + HEADER_HEIGHT // = 144 i.e. 2"
	def CONTENT_HEIGHT = PAGE_HEIGHT - HEADER_HEIGHT - (MARGIN * 2) 
	def CONTENT_Y = MARGIN + HEADER_HEIGHT
	def RULER_WIDTH = 50
	def STRAT_WIDTH = 300
	def OCCURRENCE_WIDTH = 60 // extra space for occurrences
	def LEGEND_WIDTH = 130
	def scaleFactor = 1.0
	
	// track used lithologies and occurrences for legend
	def noneSchemeEntry = new SchemeEntry("none", "None", null)
	def usedLiths = new HashSet()
	def usedOccs = new HashSet()

    void mvcGroupInit(Map args) {
    	model.project = args.project
		model.grainSizeScale = args.grainSizeScale
    }

	def setScaleFactor(intervalLength) {
		scaleFactor = CONTENT_HEIGHT / intervalLength
	}
	
	def getSchemeEntry(String schemeId, String code) {
		def scheme = Platform.getService(SchemeManager.class)?.getScheme(schemeId)
		scheme == null ? noneSchemeEntry : scheme.getEntry(code)
	}
	
	def gsoff(grainSize) { (STRAT_WIDTH * model.grainSizeScale.toScreen(grainSize)).intValue()	}
	
	// get default grain size - first value in Scale
	def gsdef() { model.grainSizeScale.values[0] }
	
	// blatantly copying from SchemeHelper.groovy - TODO: unify!
	def wrap(text, fontMetrics, maxWidth) {
		def lines = []
		def width = 0
		def curLine = ""
		text.split(" ").each { word ->
			def wordWidth = fontMetrics.stringWidth(word + " ")
			if (width + wordWidth > maxWidth) {
				lines << curLine
				curLine = word + " "
				width = wordWidth
			} else {
				curLine += (word + " ")
				width += wordWidth
			}
		}
		lines << curLine
	}

	def prepareMetadata(sortedMetadata) {
		def occs = [:]
		sortedMetadata.each {
			def secOccs = []
			def section = null
			try {
				section = model.project.openContainer(it.section)
			} catch (IllegalArgumentException iae) {
				logger.warn("Metadata section ${it.section} could not be found")
				return
			}

			def modelIterator = section.iterator()
			while (modelIterator.hasNext()) {
				GeologyModel mod = modelIterator.next()
				if (mod.modelType.equals("Occurrence")) { secOccs << mod }
			}
			occs[it.section] = secOccs
			model.project.closeContainer(section)
		}
		return occs
	}
	
	// for given section, collect properties and occurrences for each Interval
	def buildIntervalDrawData(sectionName, occMap) {
		def occCount = occMap[sectionName]?.size()
		def intervals = []
		def section = model.project.openContainer(sectionName)
		def modelIterator = section.iterator()
		while (modelIterator.hasNext()) {
			GeologyModel mod = modelIterator.next()
			if (mod.modelType.equals("Interval")) {
				def top = mod.top.to('m').value // ensure we're working in meters
				def base = mod.base.to('m').value
				def gsTop = mod.grainSizeTop ?: gsdef()
				def gsBase = mod.grainSizeBase ?: gsdef()
				
				// gather occurrences whose top depth is within this interval
				def occs = occMap[sectionName]
				def intervalOccs = occs?.findAll { it.top?.to('m').value >= top && it.top?.to('m').value <= base }
				occCount -= intervalOccs.size()
				
				intervals << ['top':top, 'base':base, 'model':mod, 'gsTop':gsTop, 'gsBase':gsBase, 'occs':intervalOccs]
			}
		}
		model.project.closeContainer(section)
		
		if (occCount > 0) logger.warn("${sectionName}: ${occCount} occurrences left over")
		
		return intervals
	}
	
	void drawRuler(graphics, physHeight) {
		def logHeight = physHeight * scaleFactor
		def xbase = MARGIN + RULER_WIDTH
		def ybase = CONTENT_Y
		
		graphics.setFont(new Font("SansSerif", Font.PLAIN, 9))
		graphics.drawString("meters", xbase - graphics.fontMetrics.stringWidth("meters"), ybase - 15)
		
		// vertical line at right edge of ruler area
		graphics.setStroke(new BasicStroke(1));
		graphics.drawLine(xbase, ybase, xbase, ybase + CONTENT_HEIGHT - 1) // minus stroke width to keep out of bottom margin
		
		// draw ticks
		def maxHeightInt = Math.ceil(physHeight).intValue()
		def curHeight = 0
		def drawDmTicks = model.drawDms && (scaleFactor > 20.72) // resolution of 4144pix/200m...200m+ cores, no dms 
		while (curHeight < maxHeightInt) {
			def bigTick = (curHeight % 5 == 0)
			def labelTick = (curHeight % 10 == 0 || drawDmTicks) // draw label every meter if dms are being drawn
			def width = bigTick ? 30 : 15
			def ytick = Math.ceil(ybase + (curHeight * scaleFactor)).intValue()
			graphics.drawLine(xbase - width, ytick, xbase, ytick)
			
			if (drawDmTicks) {
				for (int i = 1; i < 10; i++) {
					def dmtick = Math.ceil(ybase + (curHeight + (0.1 * i)) * scaleFactor).intValue()
					if (dmtick > PAGE_HEIGHT - MARGIN)
						break
					def dmTickWidth = 6
					graphics.drawLine(xbase - dmTickWidth, dmtick, xbase, dmtick)
				}
			}
			
			if (labelTick) {
				def label = "$curHeight"
				def labelWidth = graphics.fontMetrics.stringWidth(label) + 20 // + 20 to prevent overlap with small ticks
				graphics.drawString(label, xbase - labelWidth, ytick - 3)
			}
			
			curHeight++
		}
	}
	
	void drawGrainSizeScale(graphics) {
		def TICK_HEIGHT = 20
		def xmin = MARGIN + RULER_WIDTH
		def xmax = xmin + STRAT_WIDTH
		
		graphics.setStroke(new BasicStroke(1))
		graphics.setFont(new Font("SansSerif", Font.BOLD, 9))
		def labelHeight = graphics.fontMetrics.height
		
		// horizontal line over strat column
		graphics.drawLine(xmin, CONTENT_Y, xmax, CONTENT_Y)
		
		// vertical grain size separator ticks
		def labelCount = model.grainSizeScale.labels.size()
		def offsetMax = (xmin + (model.grainSizeScale.offset * STRAT_WIDTH)).intValue()
		def wid = ((xmax - offsetMax) / labelCount)
		for (int i = 0; i <= labelCount; i++) {
			def x = (offsetMax + (i * wid)).intValue()
			// always draw last tick at xmax or we may come up short due to BigDecimal -> int conversion
			if (i == labelCount)
				x = xmax
			graphics.drawLine(x, CONTENT_Y, x, CONTENT_Y - TICK_HEIGHT)
			
			// draw grain size scale lines over entire interval - brg 2/20/2015: shelving for now
//			if (false) {
//				def oldStroke = graphics.stroke
//				float[] dash = [ 5.0F ]
//				graphics.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0F, dash, 3.0F))
//				graphics.drawLine(x, CONTENT_Y, x, PAGE_HEIGHT - MARGIN)
//				graphics.stroke = oldStroke
//			}
			
			if (i < labelCount) {
				// draw labels vertically
				def oldTrans = graphics.transform
				def label = model.grainSizeScale.labels[i]
				graphics.translate(x + (wid/2).intValue() + (labelHeight/2).intValue(), CONTENT_Y - 3) // fudgy -3 so labels aren't touching ticks
				graphics.rotate(-1.57079633) // pi/2 radians, i.e. 90 degrees CCW
				graphics.drawString(label, 0, 0)
				graphics.transform = oldTrans
			}
		}
	}
	
	// draw interval's occurrences starting from interval's rightmost edge (x) and top (y)
	def drawOccurrences(graphics, intervalDrawData, intervalHeight, x, y) {
		def OCC_SPACE = 2
		def occRowWidth = OCC_SPACE * 2 // pad between end of lithology and start of occurrence
		intervalDrawData.occs.each { o ->
			def occEntry = getSchemeEntry(o.scheme?.scheme, o.scheme?.code)
			if (occEntry && occEntry.image) {
				def occwidth = drawOccurrence(graphics, occEntry, x + occRowWidth, y, intervalHeight)
				occRowWidth += (occwidth + OCC_SPACE)
				usedOccs << occEntry
			}
		}
	}
	
	int drawOccurrence(graphics, occEntry, x, y, height) {
		def occheight = Math.min(occEntry.image.height as BigDecimal, height).intValue()
		if (occheight < 1)
			occheight = 1
		def occwidth = (occheight < occEntry.image.height) ? (occEntry.image.width * (occheight / occEntry.image.height)).intValue() : occEntry.image.width

		def occpoly = new Polygon()
		occpoly.addPoint(x, y)
		occpoly.addPoint(x + occwidth, y)
		occpoly.addPoint(x + occwidth, y + occheight)
		occpoly.addPoint(x, y + occheight)
		
		def rect = new java.awt.geom.Rectangle2D.Double(x, y, occwidth, occheight)
		
		// draw white rectangle to avoid grain size scale lines showing in transparent regions of occurrence image
		// brg 2/20/2015: shelved
		//g2.setColor(Color.WHITE)
		//g2.fillRect(lx, ty, occwidth, occheight)
		
		graphics.setPaint(new TexturePaint(occEntry.image, rect))
		graphics.fill(occpoly)
		
		return occwidth
	}
	
	// draw lithology pattern for interval
	def drawInterval(graphics, schemeEntry, x, xur, xlr, y, height) {
		def lithpoly = new Polygon()
		lithpoly.addPoint(x, y)
		lithpoly.addPoint(xur, y)
		lithpoly.addPoint(xlr, y + height)
		lithpoly.addPoint(x, y + height)

		graphics.setPaint(schemeEntry?.color ?: Color.WHITE) // background color
		graphics.fill(lithpoly)
		
		if (schemeEntry?.image) { // texture
			def rect = new java.awt.geom.Rectangle2D.Double(0, 0, schemeEntry.image.width / 4.0D, schemeEntry.image.height / 4.0D)
			def texPaint = new TexturePaint(schemeEntry.image, rect)
			graphics.setPaint(texPaint)
			graphics.fill(lithpoly)
		}
		
		graphics.setPaint(Color.BLACK) // section outline
		graphics.draw(lithpoly)
	}
	
	def drawSectionName(graphics, sectionData, y, offset) {
		graphics.setFont(new Font("SansSerif", Font.PLAIN, 2))
		graphics.setPaint(Color.BLACK)
		
		def top = (sectionData.top * scaleFactor).intValue() + y
		def base = (sectionData.base * scaleFactor).intValue() + y

		// center name vertically in section range
		def centerShift = (((sectionData.base - sectionData.top) / 2.0) * scaleFactor).intValue() + 1
		graphics.drawString(sectionData.section, 5, top + centerShift)

		// start line at end of section name
		def strWidth = graphics.getFontMetrics().stringWidth(sectionData.section)
		def startx = 5 + strWidth + 1
		def endx = MARGIN + 8 + (offset ? 3 : 0)
		graphics.drawLine(startx, top, endx, top)
		drawArrow(graphics, endx - 2, top, false) // down (indicating top of section)
		graphics.drawLine(startx, base, endx, base)
		drawArrow(graphics, endx - 2, base, true) // up (base of section)
		graphics.drawLine(endx - 1, top + 1, endx - 1, base - 1)
	}
	
	def drawArrow(graphics, x, y, up) {
		def dy = up ? -1 : 1
		graphics.drawLine(x, y, x + 1, y + dy)
		graphics.drawLine(x + 1, y + dy, x + 2, y)
	}
	
	def drawLegendText(graphics, font, text, x, y, dim) {
		def fm = graphics.getFontMetrics()
		def lines = wrap(text, fm, LEGEND_WIDTH - (dim + 5))
		def lineHeight = font.createGlyphVector(fm.getFontRenderContext(), text).getVisualBounds().getHeight().intValue()
		def ystart = y + ((dim - lineHeight * (lines.size() - 1)) / 2).intValue()

		lines.each {
			graphics.setPaint(Color.BLACK)
			graphics.drawString(it, x + dim + 5, ystart)
			ystart += lineHeight
		}		
	}
	
	def drawLegend(graphics) {
		def xbase = MARGIN + RULER_WIDTH + STRAT_WIDTH + OCCURRENCE_WIDTH
		def ybase = CONTENT_Y
		
		// title, centered
		graphics.setFont(new Font("SansSerif", Font.PLAIN, 14))
		graphics.setPaint(Color.BLACK)
		def title = "Legend"
		def titleWidth = graphics.getFontMetrics().stringWidth(title)
		graphics.drawString(title, xbase + ((LEGEND_WIDTH - titleWidth) / 2).intValue(), ybase + 10)
		
		def legendFont = new Font("SansSerif", Font.PLAIN, 9)
		graphics.setFont(legendFont)
		def y = ybase + 20
		def lithDim = 30
		def liths = usedLiths.toArray().sort { it.name }
		liths.each {
			drawInterval(graphics, it, xbase, xbase + lithDim, xbase + lithDim, y, lithDim)
			drawLegendText(graphics, legendFont, it.name, xbase, y, lithDim)
			y += lithDim + 5
		}
		
		def occs = usedOccs.toArray().sort { it.name }
		def occDim = 32
		occs.each {
			drawOccurrence(graphics, it, xbase, y, occDim)
			drawLegendText(graphics, legendFont, it.name, xbase, y, occDim)
			y += occDim + 5
		}
	}
	
	def updateProgress(value, string) { 
		view.progress.value = value
		view.progress.string = string
	}
	
	def resetProgress() { updateProgress(0, '') }

	def preExport() {
		usedLiths.clear()
		usedOccs.clear()
	}
	
	void export() {
		preExport()
		
		updateProgress(10, "Preparing data...")
		
		// create depth-sorted list of section/top/base vals
		def sortedMetadata = null
		try {
			sortedMetadata = GeoUtils.parseMetadataFile(model.metadataPath, model.project)
		} catch (e) {
			Dialogs.showErrorDialog("Export Error", "Couldn't parse metadata file: does it meet all requirements?")
			resetProgress()
			return
		}
		def occMap = prepareMetadata(sortedMetadata)
		
		// determine depth to pixel scaling
		def totalIntervalLength = sortedMetadata[-1].base// - sortedMetadata[0].top
		setScaleFactor(totalIntervalLength)
		logger.info("Content height = ${CONTENT_HEIGHT}, works out to $scaleFactor pix/m, or ${1.0/scaleFactor} m/pix")
		
		//sortedMetadata.each { println "${it['section']} ${it['top']} ${it['base']}" }
		
		// create PDF
		Document document = new Document(new Rectangle(PAGE_WIDTH, PAGE_HEIGHT))
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(model.exportPath))
		document.open();
		PdfContentByte cb = writer.getDirectContent()
		Graphics2D g2 = cb.createGraphics(PAGE_WIDTH, PAGE_HEIGHT)
		
		drawGrainSizeScale(g2)
		drawRuler(g2, totalIntervalLength)
		
		// fudgy 0.3 gives decent line visibility without obscuring narrow intervals when zoomed way out
		g2.setStroke(new BasicStroke(0.3F))
		
		// draw each section's lithologies, occurrences and grain sizes
		sortedMetadata.eachWithIndex { secdata, sectionIndex ->
			logger.info("--- ${secdata.section} ---")
			updateProgress(10 + (sectionIndex / sortedMetadata.size() * 90).intValue(), "Writing ${secdata.section}")
			
			def intervals = buildIntervalDrawData(secdata.section, occMap)
			if (intervals.size() > 0) {
				// determine total length of intervals - assume they are contiguous
				def intTop = intervals[0].top
				def intLength = intervals[-1].base - intervals[0].top
				logger.info("interval top = ${intervals[0].top}, base = ${intervals[-1].base}, intervalLength = $intLength")
				def mdLength = secdata.base - secdata.top
				logger.info("metadata top = ${secdata.top}, base = ${secdata.base} len: $mdLength")
				
				// if interval length > section length, compress
				def sectionScale = 1.0
				if (intLength > mdLength) {
					sectionScale = mdLength/intLength
					logger.info("must compress by factor of $sectionScale")
				} else {
					logger.info("intLength <= mdLength: diff = ${mdLength - intLength}")
				}
				// if interval length < section length, DO NOT expand to fit - leave as is
				
				if (model.drawSectionNames) {
					def offset = (sectionIndex % 2 == 1) // stagger adjacent section lines
					drawSectionName(g2, secdata, MARGIN + HEADER_HEIGHT, offset)
				}

				intervals.eachWithIndex { curint, intervalIndex ->
					def t = (curint.top - intTop) * sectionScale + secdata.top
					def b = (curint.base - intTop) * sectionScale + secdata.top
					logger.info("Interval $intervalIndex: top = ${curint.top}, base = ${curint.base}, t = $t, b = $b")
					def xbase = MARGIN + RULER_WIDTH
					def ybase = MARGIN + HEADER_HEIGHT
					def y = new BigDecimal(t * scaleFactor).intValue() + ybase
					def bot = new BigDecimal(b * scaleFactor).intValue() + ybase
					def height = bot - y
					def xur = xbase + gsoff(curint.gsTop)
					def xlr = xbase + gsoff(curint.gsBase)

					// In cases where the physical gap between sections resolves to less
					// than one pixel, fill that gap by extending the base of the last
					// interval to match the top of the next section instead of drawing a gap.
					// Made change after seeing regular 1-pixel gaps between sections in a
					// 520m strat column (CPCP).
					if (intervalIndex == intervals.size() - 1 && sectionIndex < sortedMetadata.size() - 1) {
						def nextSec = sortedMetadata[sectionIndex + 1]
						def pixSize = 1.0 / scaleFactor
						if (nextSec.top - secdata.base < pixSize) {
							def nextSecTopY = (nextSec.top * scaleFactor).intValue() + ybase
							logger.info("nextSec top ${nextSec.top} - curSec base ${secdata.base} < 1px ($pixSize)")
							height = nextSecTopY - y
						}
					}
					logger.info("y = $y, height = $height")
					
					def pattern = curint.model.lithology
					def entry = pattern ? getSchemeEntry(pattern.scheme, pattern.code) : noneSchemeEntry
					
					drawInterval(g2, entry, xbase, xur, xlr, y, height)
					drawOccurrences(g2, curint, height, Math.max(xur, xlr), y)
					
					usedLiths << entry
				}
			} else { logger.warn("Couldn't create intervals for section ${secdata.section}") }
		} // sortedMetadata.eachWithIndex

		if (model.drawLegend) drawLegend(g2)

		g2.dispose()
		document.close()

		updateProgress(100, "Export complete!")
	}

    def actions = [
		'chooseMetadata': { evt = null ->
			def file = Dialogs.showOpenDialog('Choose Section Metadata', CustomFileFilter.CSV, app.appFrames[0])
			if (file) {
				model.metadataPath = file.absolutePath
			}
		},
		'chooseExport': { evt = null ->
			def file = Dialogs.showSaveDialog('Export Strat Column', CustomFileFilter.PDF, '.pdf', app.appFrames[0])
			if (file) {
				model.exportPath = file.absolutePath
			}
		},
		'doExport': { evt = null ->
			doOutside {	export() }
		}
    ]

    def show() {
    	Dialogs.showCustomOneButtonDialog("Export Strat Column", view.root, app.appFrames[0])
    }
}