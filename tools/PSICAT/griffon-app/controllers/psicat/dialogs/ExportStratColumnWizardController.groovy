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
	
	def PAGE_HEIGHT = 72 * 60 // 60" 
	def PAGE_WIDTH = 612 // 8.5"
	def MARGIN = 36 // 1/2" margin at 72dpi
	def HEADER_HEIGHT = 108 // space for grain size labels, ruler units, etc.
	def HEADER_Y = MARGIN + HEADER_HEIGHT // = 144 i.e. 2"
	def CONTENT_HEIGHT = PAGE_HEIGHT - HEADER_HEIGHT - (MARGIN * 2) 
	def CONTENT_Y = MARGIN + HEADER_HEIGHT
	def RULER_WIDTH = 50
	def STRAT_WIDTH = 400
	def OCCURENCE_WIDTH = 90 // extra space for occurrences
	def scaleFactor = 1.0

    void mvcGroupInit(Map args) {
    	model.project = args.project
		model.grainSizeScale = args.grainSizeScale
    }

	def setScaleFactor(intervalLength) {
		scaleFactor = CONTENT_HEIGHT / intervalLength
		//println "updated scaleFactor: $scaleFactor"
	}
	
	def getSchemeEntry(String schemeId, String code) {
		def scheme = Platform.getService(SchemeManager.class)?.getScheme(schemeId)
		scheme == null ? null : scheme.getEntry(code)
	}
	
	def gsoff(grainSize) { (STRAT_WIDTH * model.grainSizeScale.toScreen(grainSize)).intValue()	}
	
	// get default grain size - first value in Scale
	def gsdef() { model.grainSizeScale.values[0] }

	def prepareMetadata(sortedMetadata) {
		def occs = [:]
		sortedMetadata.each {
			def secOccs = []
			def section = null
			try {
				section = model.project.openContainer(it.section)
			} catch (IllegalArgumentException iae) {
				println "Metadata section ${it.section} could not be found"
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
				//println "top = $gsTop, base = $gsBase"
				
				// gather occurrences whose top depth is within this interval
				def occs = occMap[sectionName]
				def intervalOccs = occs?.findAll { it.top?.to('m').value >= top && it.top?.to('m').value <= base }
				occCount -= intervalOccs.size()
				
				intervals << ['top':top, 'base':base, 'model':mod, 'gsTop':gsTop, 'gsBase':gsBase, 'occs':intervalOccs]
//				println "Interval top = $top, base = $base, gsTop = $gsTop, gsBase = $gsBase"
			}
		}
		model.project.closeContainer(section)
		
		if (occCount > 0) println ("${sectionName}: ${occCount} occurrences left over")
		
		return intervals
	}
	
	void drawRuler(graphics, physHeight) {
		//println "scaleFactor = $scaleFactor, dmTicks = $dmTicks" 
		def logHeight = physHeight * scaleFactor
		//println "physHeight = $physHeight, page height = $PAGE_HEIGHT, content y = $CONTENT_Y, logical height of content = $logHeight"
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
				def labelWidth = graphics.fontMetrics.stringWidth(label) + 20
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
				def occpoly = new Polygon()
				def lx = x + occRowWidth
				//def ty = y
				def occheight = Math.min(occEntry.image.height as BigDecimal, intervalHeight).intValue()
				if (occheight < 1)
					occheight = 1
				def occwidth = (occheight < occEntry.image.height) ? (occEntry.image.width * (occheight / occEntry.image.height)).intValue() : occEntry.image.width
				//println "   interval height = $intervalHeight, symbol height = $occheight, width = $occwidth"
				occpoly.addPoint(lx, y)
				occpoly.addPoint(lx + occwidth, y)
				occpoly.addPoint(lx + occwidth, y + occheight)
				occpoly.addPoint(lx, y + occheight)
				
				occRowWidth += (occwidth + OCC_SPACE)

				def rect = new java.awt.geom.Rectangle2D.Double(lx, y, occwidth, occheight)
				
				// draw white rectangle to avoid grain size scale lines showing in transparent regions of occurrence image
				// brg 2/20/2015: shelved
				//g2.setColor(Color.WHITE)
				//g2.fillRect(lx, ty, occwidth, occheight)
				
				graphics.setPaint(new TexturePaint(occEntry.image, rect))
				graphics.fill(occpoly)
			}
		}
	}
	
	// draw lithology pattern for interval
	def drawInterval(graphics, schemeEntry, x, xur, xlr, y, height) {
		def lithpoly = new Polygon()
		lithpoly.addPoint(x, y)
		lithpoly.addPoint(xur, y)
		lithpoly.addPoint(xlr, y + height)
		lithpoly.addPoint(x, y + height)

		graphics.setPaint(schemeEntry.color) // background color
		graphics.fill(lithpoly)
		
		if (schemeEntry.image) { // texture
			def rect = new java.awt.geom.Rectangle2D.Double(0, 0, schemeEntry.image.width / 2.0D, schemeEntry.image.height / 2.0D)
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
		def endx = MARGIN + 10 + (offset ? 3 : 0)
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
	
	def updateProgress(value, string) { 
		view.progress.value = value
		view.progress.string = string
	}
	
	def resetProgress() { updateProgress(0, '') }

	void export() {
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
		println "Content height = ${CONTENT_HEIGHT}, works out to $scaleFactor pix/m"
		
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
		
		def texCache = [:]
		
		// draw each section's lithologies and grain sizes
		sortedMetadata.eachWithIndex { secdata, sectionIndex ->
			updateProgress(10 + (sectionIndex / sortedMetadata.size() * 90).intValue(), "Writing ${secdata.section}")
			
			def intervals = buildIntervalDrawData(secdata.section, occMap)
			if (intervals.size() > 0) {
				
				// determine total length of intervals - assume they are contiguous (brgtodo: gaps)
				def intTop = intervals[0].top
				def intLength = intervals[-1].base - intervals[0].top
				def mdLength = secdata.base - secdata.top
				
				// if interval length > section length, compress
				def sectionScale = 1.0
				if (intLength > mdLength) {
					sectionScale = mdLength/intLength
					//println "must compress intervals by factor of $sectionScale"
				}
				// if interval length < section length, DO NOT expand to fit - leave as is
				
				def drawSecName = model.drawSectionNames
				intervals.eachWithIndex { curint, intervalIndex ->
					def t = (curint.top - intTop) * sectionScale + secdata.top
					def b = (curint.base - intTop) * sectionScale + secdata.top
					def xbase = MARGIN + RULER_WIDTH
					def ybase = MARGIN + HEADER_HEIGHT
					def pattern = curint.model.lithology
					if (pattern) {
						def entry = getSchemeEntry(pattern.scheme, pattern.code)
						def code = pattern.scheme + ':' + pattern.code
						if (entry) {
							def y = new BigDecimal(t * scaleFactor).intValue() + ybase
							def height = new BigDecimal((b - t) * scaleFactor).intValue()
							def xur = xbase + gsoff(curint.gsTop)
							def xlr = xbase + gsoff(curint.gsBase)

							// In cases where the physical gap between sections resolves to less
							// than one pixel, fill that gap by extending the base of the last
							// interval to match the top of the next section. (Rather than drawing
							// an exaggerated gap due to rounding up). Made change after seeing regular
							// 1-pixel gaps between sections in a 520m strat column (CPCP).
							if (intervalIndex == intervals.size() - 1 && sectionIndex < sortedMetadata.size() - 1) {
								def nextSec = sortedMetadata[sectionIndex + 1]
								def pixSize = 1.0 / scaleFactor
								if (nextSec.top - secdata.base < pixSize)
									height += 1
							}
							
							drawInterval(g2, entry, xbase, xur, xlr, y, height)// + fudgy)
							drawOccurrences(g2, curint, height, Math.max(xur, xlr), y)
	
							if (drawSecName) {
								def offset = (sectionIndex % 2 == 1)
								drawSectionName(g2, secdata, ybase, offset)
								drawSecName = false
							}
						} else { println "No entry found" }
					} else { println "No lithology found" }
				}
			} else { println "Couldn't create intervals for section ${secdata.section}" }
		}
		
		g2.dispose();
		document.close();
		
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