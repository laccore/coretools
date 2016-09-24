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

class IntervalDrawData {
	public sectionName
	public top
	public base
	public models
	
	public IntervalDrawData(sectionName, top, base, models) {
		this.sectionName = sectionName
		this.top = top
		this.base = base
		this.models = models
	}
}

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
	def topDepth = 0.0
	def bottomDepth = 15.0
	
	// track used lithologies and occurrences for legend
	def noneSchemeEntry = new SchemeEntry("none", "None", null)
	def usedLiths = new HashSet()
	def usedOccs = new HashSet()

    void mvcGroupInit(Map args) {
    	model.project = args.project
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
	def drawSectionName(graphics, sectionData, y, offset, trimExpName=null) {
		drawSectionName(graphics, sectionData.top, sectionData.base, sectionData.section, y, offset, trimExpName)
	}
	
	def drawSectionName(graphics, sectop, secbase, secname, y, offset, trimExpName=null) {
		graphics.setFont(new Font("SansSerif", Font.PLAIN, 2))
		graphics.setPaint(Color.BLACK)
		
		def top = depth2pix(sectop) + y
		def base = depth2pix(secbase) + y
		
		if (trimExpName) {
			def trimIndex = secname.indexOf(trimExpName)
			if (trimIndex != -1)
				secname = secname.substring(trimIndex + trimExpName.length())
			def underscoreIndex = secname.indexOf("_")
			if (underscoreIndex != -1)
				secname = secname.substring(0, underscoreIndex)
			graphics.setFont(new Font("SansSerif", Font.PLAIN, 4)) // use bigger font, more space!
		}

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
	}
	
	void errbox(title, message) {
		Dialogs.showErrorDialog(title, message, view.root)
		resetProgress()
	}
	
	// return list of section names, starting with startSec, ending with endSec,
	// and including any sections that fall between the two
	def getSectionsInInterval(startSec, endSec) {
		def startnum, endnum
		try {
			startnum = startSec[-1] as Integer
			endnum = endSec[-1] as Integer
		} catch (e) {
			errbox("Parse Error", "Couldn't get section number")
			return []
		}
		
		if (endnum < startnum) {
			errbox("Data Error", "End section $endnum precedes start section $startnum")
			return []
		}
		
		def sections = []
		def baseName = startSec.substring(0, startSec.lastIndexOf('-') + 1)
		(startnum..endnum).each { sections <<  baseName + it }
		
		// find full project seciton name
		def projSections = []
		sections.each { secname ->
			def projsec = model.project.containers.find { it.startsWith(secname) }
			if (projsec)
				projSections << projsec
			else
				println "Couldn't find matching project section for $secname" 
		}

		return projSections
	}
	
	// mod: Interval model, rmin: range min (in meters), rmax: range max (in meters)
	boolean intervalInRange(mod, rmin, rmax) {
		def top = mod.top.to('m').value
		def bot = mod.base.to('m').value
		return (top > rmin && top < rmax) || (bot > rmin && bot < rmax) || (top < rmin && bot > rmax)
	}
	
	// cull models out of range, trim models that overlap range
	def getTrimmedModels(secname, min, max) {
		println "Trimming $secname, min = $min, max = $max"
		def trimmedModels = []
		def projContainer = model.project.openContainer(secname)
		def cursec = GeoUtils.copyContainer(projContainer)
		GeoUtils.zeroBaseContainer(cursec)
		def modit = cursec.iterator()
		while (modit.hasNext()) {
			GeologyModel mod = modit.next()
			
			// only interested in Intervals and Occurrences, skip others, particularly Images,
			// which exceed curated length of section due to inclusion of color card
			if (!mod.modelType.equals("Interval") && !mod.modelType.equals("Occurrence"))
				continue;

			if (min) {
				def cmp = mod.base.compareTo(min)
				if (cmp == -1 || cmp == 0) {
					println "   $mod out of range or base == $min, culling"
					continue;
				}
				if (mod.top.compareTo(min) == -1 && mod.base.compareTo(min) == 1) {
					print "   $mod top above $min, trimming..."
					mod.top = min.to('m')
					println "$mod"
				}
			}
			if (max) {
				def cmp = mod.top.compareTo(max)
				if (cmp == 1 || cmp == 0) {
					println "   $mod out of range or top == $max, culling"
					continue;
				}
				if (mod.top.compareTo(max) == -1 && mod.base.compareTo(max) == 1) {
					print "   $mod bot below $max, trimming..."
					mod.base = max.to('m')
					println "$mod"
				}
			}
			trimmedModels << mod
		}
		
		println "   pre-zeroBase: trimmedModels = $trimmedModels"
		
		// now that we've trimmed, need to zero base *again* so scaling works properly
		GeoUtils.zeroBase(trimmedModels)
		
		println "   post-zeroBase: trimmedModels = $trimmedModels"
		return trimmedModels
	}
	
	def offsetModels(modelList, offset) {
		modelList.each {
			it.top += offset
			it.base += offset
		}
	}
	
	def scaleModels(modelList, scale) {
		modelList.each {
			it.top *= scale
			it.base *= scale
		}
	}
	
	def compressModels(models, drilledLength) {
		if (models.size() > 0) {
			def maxBase = getMaxBase(models)
			def scalingFactor = 1.0
			if (maxBase.value > 0.0) // avoid divide by zero
				scalingFactor = drilledLength / maxBase.value
			println "Drilled length = $drilledLength, modelBase = $maxBase, scalingFactor = $scalingFactor"
			if (scalingFactor < 1.0) {
				println "   Downscaling models..."
				scaleModels(models, scalingFactor)
				println "   Downscaled: $models"
			} else {
				println "   Scaling factor >= 1.0, leaving models as-is"
			}
		}
	}
	
	def getMinTop(modelList) {
		def min = null
		modelList.each {
			if (!min || it.top.compareTo(min) == -1)
				min = it.top
		}
		return min
	}
	
	def getMaxBase(modelList) {
		def max = null
		modelList.each {
			if (!max || it.base.compareTo(max) == 1)
				max = it.base
		}
		return max
	}

	boolean parseSectionMetadata(secMetadataPath) {
		def metadata = null
		try {
			metadata = GeoUtils.parseMetadataFile(secMetadataPath, model.project)
		} catch (e) {
			errbox("Export Error", "Couldn't parse metadata file: ${e.getMessage()}")
			return false
		}
		
		if (metadata.size() == 0) {
			errbox("Export Error", "Couldn't find any project sections that match metadata sections.")
			return false
		}
		
		def intervalsToDraw = []
		metadata.each {
			// gather and zero base models for each section
			def models = getTrimmedModels(it.section, null, null) // no min/max
			
			// compress models to fit drilled interval if necessary
			def drilledLength = it.base - it.top
			compressModels(models, drilledLength)
			
			def intervalModels = [new IntervalDrawData(it.section, it.top, it.base, models)]
			intervalsToDraw.add(['top':it.top, 'base':it.base, 'siIntervals':intervalModels])
		}
		
		model.sortedMetadata = intervalsToDraw.sort { it.top }
		model.startDepth = intervalsToDraw[0].top
		model.endDepth = intervalsToDraw[-1].base
				
		return true
	}
		
	boolean parseSIT(sitPath) {
		def sitdata = null
		try {
			sitdata = GeoUtils.parseSITFile(sitPath, model.project, "MEXI-CHA16")//PLJ-JUN15")
		} catch (e) {
			errbox("Export Error", "Couldn't parse SIT file: ${e.getMessage()}")
			return false
		}
		if (sitdata.size() == 0) {
			errbox("Export Error", "Couldn't find any rows in SIT file.")
			return false
		}
		
		// for each SIT row
		def sitMetadata = [] // list of maps of stuff
		sitdata.eachWithIndex { sd, sitIndex ->
			println "\n--- Interval $sitIndex ---"
			def intervalModels = []
			
			// get names of sections
			def sections = getSectionsInInterval(sd.startSec, sd.endSec)
			def maxBase = null
			sections.eachWithIndex { secname, index ->
				def min = null, max = null
				if (secname.startsWith(sd.startSec))
					min = new Length(sd.startSecDepth, 'cm')
				if (secname.startsWith(sd.endSec))
					max = new Length(sd.endSecDepth, 'cm')
				def models = getTrimmedModels(secname, min, max)
				
				// start models from maximum base of previous section's models
				if (maxBase) {
					println "maxBase = $maxBase, offsetting"
					offsetModels(models, maxBase)
				}
				println "offset models = $models"
				
				// find max base of models - start next section from that depth
				if (models.size() > 0) {
					def minTop = getMinTop(models)
					maxBase = getMaxBase(models)
					intervalModels.add(new IntervalDrawData(secname, sd.startMcd + minTop.value, sd.startMcd + maxBase.value, models))
				}
			}
			
			// compress models to fit drilled interval if necessary
			def drilledLength = sd.endMcd - sd.startMcd
			def scalingFactor = drilledLength / maxBase.value
			println "Drilled length = $drilledLength, modelBase = $maxBase, scalingFactor = $scalingFactor"
			if (scalingFactor < 1.0) {
				println "   Downscaling models..."
				intervalModels.each { idd ->
					scaleModels(idd.models, scalingFactor)
					println "   Downscaled: $idd.models"
				}
			} else {
				println "   Scaling factor >= 1.0, leaving models as-is"
			}
			def spliceIntervalMap = ['top':sd.startMcd, 'base':sd.endMcd, 'siIntervals':intervalModels]
			sitMetadata.add(spliceIntervalMap)
		}
		
		model.sortedMetadata = sitMetadata.sort { it.top }
		model.startDepth = sitMetadata[0].top
		model.endDepth = sitMetadata[-1].base
		
		return true
	}
	
	void export() {
		preExport()

		if (model.drawGrainSize && !model.useProjectGrainSize && !model.alternateGrainSizePath) {
			errbox("Export Error", "A default grain size file must be selected.")
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
		logger.info("Content height = ${CONTENT_HEIGHT}, works out to $scaleFactor pix/m, or ${1.0/scaleFactor} m/pix")

		// parse alternate grain sizes if necessary...
		def altGSMap = null
		if (model.drawGrainSize && !model.useProjectGrainSize) {
			try {
				def altGSData = GeoUtils.parseAlternateGrainSizeFile(model.alternateGrainSizePath)
				model.grainSizeScale = new Scale(altGSData['scale'])
				altGSMap = altGSData['gs']
			} catch (e) {
				errbox("Export Error", "Couldn't parse default grain size file: ${e.getMessage()}")
				return
			}
		} else { // ...or use project's grain size
			model.grainSizeScale = app.controllers['PSICAT'].grainSize
		}
		
		updateProgress(10, "Preparing data...")
		
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
				
				def nextTop = sitIndex + 1 < model.sortedMetadata.size() ? model.sortedMetadata[sitIndex + 1].top : null 
				
				// now grab intervals and draw them - curint is simply the Model that is an Interval
				sitdata.siIntervals.eachWithIndex { intervalDrawData, sectionIndex ->
					logger.info("- ${intervalDrawData.sectionName} -")
					//updateProgress(10 + (sectionIndex / model.sortedMetadata.size() * 90).intValue(), "Writing ${secname}")
					def modelList = intervalDrawData.models
					
					if (model.drawSectionNames) {
						drawSectionName(g2, intervalDrawData.top, intervalDrawData.base, intervalDrawData.sectionName, MARGIN + HEADER_HEIGHT, offsetSectionName, "MEXI-CHA16-")
						offsetSectionName = !offsetSectionName
					}
										
					// modelList now contains Intervals and Occurrences - need to separate the two
					def rawIntervals = modelList.findAll { it.modelType.equals("Interval") }
					def intervals = rawIntervals.sort { a, b -> a.base.compareTo(b.base) } // ensure max base is last in list so minimum gap is filled below
					def occs = modelList.findAll { it.modelType.equals("Occurrence") }

					intervals.eachWithIndex { mod, intervalIndex ->
						def t = sitdata.top + mod.top.value
						def b = sitdata.top + mod.base.value

						logger.info("   Interval $intervalIndex: top = ${mod.top}, base = ${mod.base}, t = $t, b = $b")
						def xbase = MARGIN + RULER_WIDTH
						def ybase = MARGIN + HEADER_HEIGHT
						def y = depth2pix(t) + ybase
						def bot = depth2pix(b) + ybase
						
						// use alternate grain size if necessary
						def pattern = mod.lithology
						def entry = pattern ? getSchemeEntry(pattern.scheme, pattern.code) : noneSchemeEntry
						if (pattern && !entry) {
							logger.info("no scheme entry found for $pattern, mapping to None lithology")
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
								logger.info("\n==> next top ${nextTop} - curSec base $b < 1px ($pixSize)???\n==>")
								if (Math.abs(nextTop - b) < pixSize) {
									nextSecTopY = depth2pix(nextTop) + ybase
									logger.info("\n====>\n====> next metadata interval top ${nextTop} - curSec base $b < 1px ($pixSize)\n====>\n")
									//height = nextSecTopY - y
								}
							}
							if (nextSecTopY) 
								height = nextSecTopY - y
						}
						logger.info("y = $y, height = $height")

						def xur = xbase + (model.drawGrainSize ? gsoff(gsTop) : STRAT_WIDTH)
						def xlr = xbase + (model.drawGrainSize ? gsoff(gsBase) : STRAT_WIDTH)
						drawInterval(g2, entry, xbase, xur, xlr, y, height)
						if (!entry) println "### ERROR No lithology entry for $mod"
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

		updateProgress(100, "Export complete!")
	}
	
    def actions = [
		'chooseMetadata': { evt = null ->
			def file = Dialogs.showOpenDialog('Choose Section Metadata', CustomFileFilter.CSV, app.appFrames[0])
			//if (file && parseMetadata(file.absolutePath)) { // immediately verify and parse file (if valid)
			doOutside {
				if (file) {
					try {
						def mdType = GeoUtils.identifyMetadataFile(file.absolutePath)
						if (mdType == GeoUtils.SectionMetadataFile)
							parseSectionMetadata(file.absolutePath)
						else if (mdType == GeoUtils.SpliceIntervalFile)
							parseSIT(file.absolutePath)
						else {
							errbox("Unrecognized Metadata", "Couldn't identify metadata file")
							return
						}
						model.metadataPath = file.absolutePath
					} catch (Exception e) {
						errbox("Parsing error", "Error: ${e.message}")
					}
				}
			}
		},
		'chooseExport': { evt = null ->
			def file = Dialogs.showSaveDialog('Export Strat Column', CustomFileFilter.PDF, '.pdf', app.appFrames[0])
			if (file) {
				model.exportPath = file.absolutePath
			}
		},
		'chooseAlternateGrainSize': { evt = null ->
			def file = Dialogs.showOpenDialog("Choose Default Grain Size File", CustomFileFilter.CSV, app.appFrames[0])
			if (file) { model.alternateGrainSizePath = file.absolutePath }
		},
		'doExport': { evt = null ->
			doOutside {	export() }
		}
    ]

    def show() {
    	Dialogs.showCustomOneButtonDialog("Export Strat Column", view.root, app.appFrames[0])
    }
}