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

import org.apache.log4j.Logger
import org.apache.log4j.FileAppender
import org.apache.log4j.SimpleLayout

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import org.andrill.coretools.Platform;
import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.ui.Scale
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeEntry
import org.andrill.coretools.model.scheme.SchemeManager

import psicat.util.*

class ExportStratColumnWizardController {
    def model
    def view
	
	private static Logger logger = Logger.getLogger(ExportStratColumnWizardController.class)
	
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
	def topDepth = 0.0
	def bottomDepth = 15.0
	
	// track used lithologies and occurrences for legend
	def noneSchemeEntry = new SchemeEntry("none", "None", null)
	def usedLiths = new HashSet()
	def usedOccs = new HashSet()

    void mvcGroupInit(Map args) {
    	model.project = args.project
		model.metadataPath = args.metadataPath
    }

	def setScaleFactor(intervalLength) {
		scaleFactor = CONTENT_HEIGHT / intervalLength
	}
	
	def getSchemeEntry(String schemeId, String code) {
		def scheme = Platform.getService(SchemeManager.class)?.getScheme(schemeId)
		scheme == null ? noneSchemeEntry : scheme.getEntry(code)
	}
	
	def depth2pix(depth) { ((depth - topDepth) * scaleFactor).intValue() } 

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
	
	void drawRuler(graphics, top, base) {
		def physHeight = base - top
		def xbase = MARGIN + RULER_WIDTH
		def ybase = CONTENT_Y
		
		// top depth label
		graphics.setFont(new Font("SansSerif", Font.PLAIN, 9))
		def roundedTop = top.setScale(3, BigDecimal.ROUND_HALF_UP) 
		def topDepthString = "${top} meters"
		graphics.drawString(topDepthString, xbase - graphics.fontMetrics.stringWidth(topDepthString), ybase - 5)
		
		// draw ticks
		def maxHeightInt = Math.ceil(base).intValue()
		def topInt = Math.floor(top).intValue()
		def curHeight = topInt + (topInt < top ? 1 : 0)
		def drawDmTicks = model.drawDms && (scaleFactor > 20.72) // resolution of 4144pix/200m...200m+ cores, no dms 
		while (curHeight < maxHeightInt) {
			def bigTick = (curHeight % 5 == 0)
			def labelTick = (curHeight % 10 == 0 || drawDmTicks) // draw label every meter if dms are being drawn
			def width = bigTick ? 30 : 15
			def ytick = ybase + depth2pix(curHeight)
			graphics.drawLine(xbase - width, ytick, xbase, ytick)
			
			if (drawDmTicks) {
				for (int i = 1; i < 10; i++) {
					def dmtick = Math.ceil(ybase + ((curHeight - topInt) + (0.1 * i)) * scaleFactor).intValue()
					if (dmtick > PAGE_HEIGHT - MARGIN)
						break
					def dmTickWidth = 6
					graphics.drawLine(xbase - dmTickWidth, dmtick, xbase, dmtick)
				}
			}
			
			// don't label tick if it's < 1m below topmost depth tick to avoid overlap
			if (labelTick && (curHeight - roundedTop > 1.0)) {
				def label = "$curHeight"
				def labelWidth = graphics.fontMetrics.stringWidth(label) + 20 // + 20 to prevent overlap with small ticks
				// 1.4 is the fudgiest of fudge factors, but the numbers are nicely placed now.
				graphics.drawString(label, xbase - Math.ceil(labelWidth/1.4).intValue(), ytick - 3)
			}
			
			curHeight++
		}
		
		// draw depth of final tick
		def roundedBase = base.setScale(3, BigDecimal.ROUND_HALF_UP)
		def baseYPos = Math.ceil(ybase + physHeight * scaleFactor).intValue()
		graphics.drawLine(xbase - 30, baseYPos, xbase, baseYPos)
		def baseLabelWidth = graphics.fontMetrics.stringWidth("$roundedBase")
		graphics.drawString("$roundedBase", xbase - Math.ceil(baseLabelWidth).intValue(), baseYPos + 8) // draw below final tick
		
		// right border of ruler
		graphics.setStroke(new BasicStroke(1));
		graphics.drawLine(xbase, ybase, xbase, baseYPos)
	}
	
	void drawGrainSizeScale(graphics) {
		def TICK_HEIGHT = 20
		def xmin = MARGIN + RULER_WIDTH
		def xmax = xmin + STRAT_WIDTH
		
		graphics.setStroke(new BasicStroke(1))
		graphics.setFont(new Font("SansSerif", Font.BOLD, 9))
		def labelHeight = graphics.fontMetrics.height
		
		// horizontal line over strat column - also first tick of ruler, thus - 30 to draw a "big tick"
		graphics.drawLine(xmin - 30, CONTENT_Y, xmax, CONTENT_Y)
		
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
	
	int drawOccurrence(graphics, occEntry, x, y, height) {
		def occheight = Math.min(occEntry.image.height as BigDecimal, height).intValue()
		if (occheight < 1)
			occheight = 1
		def occwidth = (occheight < occEntry.image.height) ? (occEntry.image.width * (occheight / occEntry.image.height)).intValue() : occEntry.image.width

		// 6/10/2015 brg: swear I tried this simple approach before but abandoned it for some reason.
		// No problems now, looks slightly better than texpoly method (smoother, and no weird artifacting
		// at edges) and results in a smaller file to boot. Leaving in for now.
		graphics.drawImage(occEntry.image, x, y, occwidth, occheight, null)
		
		return occwidth
	}
	
	// draw lithology pattern for interval
	def drawInterval(graphics, schemeEntry, x, xur, xlr, y, height, outline=false) {
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

		if (outline) {
			graphics.setPaint(Color.BLACK) // section outline
			graphics.draw(lithpoly)
		}
	}
	
	// convenience method for "classic" strat column generation, where sectionData is a map with top, base, and section (section name)
	def drawSectionName(graphics, sectionData, y, offset) {
		drawSectionName(graphics, sectionData.top, sectionData.base, sectionData.section, y, offset)
	}
	
	def drawSectionName(graphics, sectop, secbase, secname, y, offset) { // trimExpName=null
		graphics.setFont(new Font("SansSerif", Font.PLAIN, 2))
		graphics.setPaint(Color.BLACK)
		
		def top = depth2pix(sectop) + y
		def base = depth2pix(secbase) + y

		// experimental - remove (typically redundant) project portion of section names
		// to save space allow larger font in section names		
//		if (trimExpName) {
//			def trimIndex = secname.indexOf(trimExpName)
//			if (trimIndex != -1)
//				secname = secname.substring(trimIndex + trimExpName.length())
//			def underscoreIndex = secname.indexOf("_")
//			if (underscoreIndex != -1)
//				secname = secname.substring(0, underscoreIndex)
//			graphics.setFont(new Font("SansSerif", Font.PLAIN, 4)) // use bigger font, more space!
//		}

		// center name vertically in section range
		def centerShift = (((secbase - sectop) / 2.0) * scaleFactor).intValue() + 1
		graphics.drawString(secname, 5, top + centerShift)

		// start line at end of section name
		def strWidth = graphics.getFontMetrics().stringWidth(secname)
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
			drawInterval(graphics, it, xbase, xbase + lithDim, xbase + lithDim, y, lithDim, true)
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
		initLogging()
	}
	
	def initLogging() {
		if (model.exportLog) {
			def logPath = FileUtils.removeExtension(model.exportPath) + ".log"
			def appender = new FileAppender(new SimpleLayout(), logPath, false)
			logger.addAppender(appender)
			logger.setAdditivity(false)
		}
	}
	
	def shutdownLogging() {
		if (model.exportLog) logger.removeAllAppenders()
	}
	
	void errbox(title, message) {
		Dialogs.showErrorDialog(title, message, view.root)
		resetProgress()
	}
	
	void export() {
		preExport()

		if (model.drawGrainSize && !model.useProjectGrainSize && !model.alternateGrainSizePath) {
			errbox("Export Error", "A grain size file must be selected.")
			return
		}
		
		// determine depth to pixel scaling
		try {
			topDepth = model.startDepth as BigDecimal
			bottomDepth = model.endDepth as BigDecimal
		} catch (Exception e) {
			errbox("Export Error", "Invalid top or bottom depth.")
			return
		}
		if (bottomDepth <= topDepth) {
			errbox("Export Error", "Bottom depth must be greater than top depth.")
			return
		}
		def totalIntervalLength = bottomDepth - topDepth
		setScaleFactor(totalIntervalLength)
		logger.info("Starting strat column export to ${model.exportPath}...")
		logger.info("Content height = ${CONTENT_HEIGHT}pix, works out to $scaleFactor pix/m, or ${1.0/scaleFactor} m/pix")

		// parse alternate grain sizes if necessary...
		def altGSMap = null
		if (model.drawGrainSize && !model.useProjectGrainSize) {
			try {
				def altGSData = GeoUtils.parseAlternateGrainSizeFile(model.alternateGrainSizePath)
				model.grainSizeScale = new Scale(altGSData['scale'])
				altGSMap = altGSData['gs']
			} catch (e) {
				errbox("Export Error", "Couldn't parse grain size file: ${e.getMessage()}")
				return
			}
		} else { // ...or use project's grain size
			model.grainSizeScale = app.controllers['PSICAT'].grainSize
		}
		
		updateProgress(10, "Preparing data...")
		
		try {
			model.sortedMetadata = model.stratColumnMetadata.getDrawData(model.project, logger)
		} catch (e) {
			errbox("Export Error", "Error processing metadata: ${e.getMessage()}")
			return
		}
		
		// create PDF
		Document document = new Document(new Rectangle(PAGE_WIDTH, PAGE_HEIGHT))
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(model.exportPath))
		document.open();
		PdfContentByte cb = writer.getDirectContent()
		Graphics2D g2 = cb.createGraphics(PAGE_WIDTH, PAGE_HEIGHT)
		
		if (model.drawGrainSize && model.drawGrainSizeLabels)
			drawGrainSizeScale(g2)
			
		drawRuler(g2, topDepth, bottomDepth)
		
		// fudgy 0.3 gives decent line visibility without obscuring narrow intervals when zoomed way out
		g2.setStroke(new BasicStroke(0.3F))
		
		def offsetSectionName = false
		model.sortedMetadata.eachWithIndex { sitdata, sitIndex ->
			if (sitdata.top < topDepth || sitdata.base > bottomDepth) {
				logger.info("Skipping ${sitdata.section} [${sitdata.top} - ${sitdata.base} outside of depth range [$topDepth - $bottomDepth]")
			} else {
				logger.info("--- Interval $sitIndex ---")
				updateProgress(10 + (sitIndex / model.sortedMetadata.size() * 90).intValue(), "Exporting data...")
				
				def nextTop = sitIndex + 1 < model.sortedMetadata.size() ? model.sortedMetadata[sitIndex + 1].top : null 
				
				// now grab intervals and draw them - curint is simply the Model that is an Interval
				sitdata.siIntervals.eachWithIndex { sectionDrawData, sectionIndex ->
					logger.info("- Drawing ${sectionDrawData.sectionName}... -")
					def modelList = sectionDrawData.models
					
					if (model.drawSectionNames) {
						drawSectionName(g2, sectionDrawData.top, sectionDrawData.base, sectionDrawData.sectionName, MARGIN + HEADER_HEIGHT, offsetSectionName)
						offsetSectionName = !offsetSectionName
					}
										
					// modelList now contains Intervals and Occurrences - need to separate the two
					def rawIntervals = modelList.findAll { it.modelType.equals("Interval") }
					def intervals = rawIntervals.sort { a, b -> a.base.compareTo(b.base) } // ensure max base is last in list so minimum gap is filled below
					def occs = modelList.findAll { it.modelType.equals("Occurrence") }

					intervals.eachWithIndex { mod, intervalIndex ->
						def t = sectionDrawData.top + mod.top.value
						def b = sectionDrawData.top + mod.base.value

						logger.info("   Interval $intervalIndex: top = ${mod.top}, base = ${mod.base}, t = $t, b = $b")
						def xbase = MARGIN + RULER_WIDTH
						def ybase = MARGIN + HEADER_HEIGHT
						def y = depth2pix(t) + ybase
						def bot = depth2pix(b) + ybase
						
						// use alternate grain size if necessary
						def pattern = mod.lithology
						def entry = pattern ? getSchemeEntry(pattern.scheme, pattern.code) : noneSchemeEntry
						if (pattern && !entry) {
							logger.warn("no scheme entry found for $pattern, mapping to None lithology")
							entry = noneSchemeEntry
						}
						def gsTop = mod.grainSizeTop ?: gsdef() // TODO: gsdef() check here for missing grain sizes
						def gsBase = mod.grainSizeBase ?: gsdef()
						if (model.drawGrainSize && !model.useProjectGrainSize) {
							def altGS = altGSMap[pattern?.toString()] ?: gsdef()
							logger.info("current lith = [$pattern], [$altGS]")
							gsTop = gsBase = altGS
						}
						
						// In cases where the physical gap between sections resolves to less
						// than one pixel, fill that gap by extending the base of the last
						// interval to match the top of the next section instead of drawing a gap.
						// Made change after seeing regular 1-pixel gaps between sections in a
						// 520m strat column (CPCP).
						def height = bot - y
						if (intervalIndex == intervals.size() - 1) {
							def nextSecTopY = null
							def pixSize = 1.0 / scaleFactor
							if (sectionIndex < sitdata.siIntervals.size() - 1) {
								def nextSec = sitdata.siIntervals[sectionIndex + 1]
								if (nextSec.top - sitdata.base < pixSize) {
									nextSecTopY = depth2pix(nextSec.top) + ybase
									logger.info("nextSec top ${nextSec.top} - curSec base ${sitdata.base} < 1px ($pixSize)")
								}
							} else if (nextTop) {
								logger.info("next top ${nextTop} - curSec base $b < 1px ($pixSize)?")
								if (Math.abs(nextTop - b) < pixSize) {
									nextSecTopY = depth2pix(nextTop) + ybase
									logger.info("next metadata interval top ${nextTop} - curSec base $b < 1px ($pixSize)")
									//height = nextSecTopY - y
								}
							}
							if (nextSecTopY) 
								height = nextSecTopY - y
						}
						logger.info("y = $y, physical height = ${b - t}, pixel height = $height")

						def xur = xbase + (model.drawGrainSize ? gsoff(gsTop) : STRAT_WIDTH)
						def xlr = xbase + (model.drawGrainSize ? gsoff(gsBase) : STRAT_WIDTH)
						drawInterval(g2, entry, xbase, xur, xlr, y, height, model.drawIntervalBorders)
						if (!entry) logger.warn("No lithology entry for $mod")
						usedLiths << entry
						
						if (model.drawSymbols) {
							// find Occurrences in this Interval and draw
							def intervalOccs = occs.findAll { (it.base.value + it.top.value) / 2.0 > mod.top.value && (it.base.value + it.top.value) / 2.0 < mod.base.value }
	
							if (model.aggregateSymbols) {
								def usedEntries = new HashSet()
								def aggregatedOccs = []
								intervalOccs.each { it ->
									if (usedEntries.add(it.scheme.toString())) { aggregatedOccs << it }
								}
								intervalOccs = aggregatedOccs
							}
							
							// draw interval's occurrences starting from interval's rightmost edge (x) and top (y)
							def OCC_SPACE = 2
							def occRowWidth = OCC_SPACE * 2 // pad between end of lithology and start of occurrence
							intervalOccs.each { o ->
								def occEntry = getSchemeEntry(o.scheme?.scheme, o.scheme?.code)
								if (occEntry && occEntry.image) {
									def occwidth = drawOccurrence(g2, occEntry, Math.max(xlr, xur) + occRowWidth, y, height)
									occRowWidth += (occwidth + OCC_SPACE)
									usedOccs << occEntry
								}
							}
						}
					} // intervals.eachWithIndex()
				}
			}
		}

		if (model.drawLegend) drawLegend(g2)

		g2.dispose()
		document.close()

		logger.info("Export complete!\n########################\n\n")
		shutdownLogging()
		updateProgress(100, "Export complete!")
	}
	
    def actions = [
		'chooseMetadata': { evt = null ->
			chooseMetadata()
		},
		'chooseExport': { evt = null ->
			def file = Dialogs.showSaveDialog('Export Strat Column', CustomFileFilter.PDF, '.pdf', app.appFrames[0])
			if (file) {
				model.exportPath = file.absolutePath
			}
		},
		'chooseAlternateGrainSize': { evt = null ->
			def file = Dialogs.showOpenDialog("Choose Grain Size File", CustomFileFilter.CSV, app.appFrames[0])
			if (file) { model.alternateGrainSizePath = file.absolutePath }
		},
		'doExport': { evt = null ->
			doOutside {	export() }
		}
    ]
	
	boolean chooseMetadata() {
		def confirmed = false
		try {
			app.controllers['PSICAT'].withMVC('OpenStratColumnDepths', project:model.project, metadataPath:model.metadataPath) { mvc ->
				def dlg = mvc.view.openSCMD
				dlg.setLocationRelativeTo(app.appFrames[0])
				dlg.setVisible(true)
				if (mvc.model.confirmed) {
					model.metadataPath = mvc.model.metadataPath
					model.stratColumnMetadata = mvc.model.stratColumnMetadata
					model.startDepth = model.stratColumnMetadata.getTop()
					model.endDepth = model.stratColumnMetadata.getBase()
					confirmed = true
				}
			}
		} catch (Exception e) {
			errbox("Metadata Error", "${e.message}")
		}
		return confirmed
	}

    def show() {
    	Dialogs.showCustomOneButtonDialog("Export Strat Column", view.root, app.appFrames[0])
    }
}