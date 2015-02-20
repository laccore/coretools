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
			def section = model.project.openContainer(it.section)
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
		while (curHeight < maxHeightInt) {
			def bigTick = (curHeight % 5 == 0)
			def labelTick = (curHeight % 10 == 0)
			def width = bigTick ? 30 : 15
			def ytick = Math.ceil(ybase + (curHeight * scaleFactor)).intValue()
			graphics.drawLine(xbase - width, ytick, xbase, ytick)
			
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

	void export() {
		if (!model.metadataFile) return

		// create depth-sorted list of section/top/base vals
		def sortedMetadata = GeoUtils.parseMetadataFile(model.metadataFile)
		sortedMetadata = GeoUtils.reconcileSectionIDs(sortedMetadata, model.project)
		def occMap = prepareMetadata(sortedMetadata)
		
		// determine depth to pixel scaling
		def totalIntervalLength = sortedMetadata[-1].base// - sortedMetadata[0].top
		setScaleFactor(totalIntervalLength)
		
		//sortedMetadata.each { println "${it['section']} ${it['top']} ${it['base']}" }
		
		// create PDF
		String filename = new String("/Users/bgrivna/Desktop/stratcolumn.pdf");
		//String filename = new String("C:\\Users\\bgrivna\\Desktop\\stratcolumn.pdf")
		Document document = new Document(new Rectangle(PAGE_WIDTH, PAGE_HEIGHT))
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename))
		document.open();
		PdfContentByte cb = writer.getDirectContent()
		Graphics2D g2 = cb.createGraphics(PAGE_WIDTH, PAGE_HEIGHT)
		
		drawGrainSizeScale(g2)
		drawRuler(g2, totalIntervalLength)
		
		// fudgy 0.3 gives decent line visibility without obscuring narrow intervals when zoomed way out
		g2.setStroke(new BasicStroke(0.3F))
		
		// draw each section's lithologies and grain sizes
		sortedMetadata.each { secdata ->
			def intervals = buildIntervalDrawData(secdata.section, occMap)
			
			// determine total length of intervals - assume they are contiguous (brgtodo: gaps)
			def intTop = intervals[0].top
			//def mdTop = secdata.top
			def intLength = intervals[-1].base - intervals[0].top
			def mdLength = secdata.base - secdata.top
			//println "interval length = $intLength, metadata length = $mdLength"
			
			// if interval length > section length, compress
			def sectionScale = 1.0
			if (intLength > mdLength) {
				sectionScale = mdLength/intLength
				//println "must compress intervals by factor of $sectionScale"
			}
			// if interval length < section length, DO NOT expand to fit - leave as is
			
			def drawSecName = false
			intervals.each { curint ->
				def t = (curint.top - intTop) * sectionScale + secdata.top
				def b = (curint.base - intTop) * sectionScale + secdata.top
				def xbase = MARGIN + RULER_WIDTH
				def ybase = MARGIN + HEADER_HEIGHT
				def pattern = curint.model.lithology
				if (pattern) {
					def entry = getSchemeEntry(pattern.scheme, pattern.code)
					if (entry) {
						def y = new BigDecimal(t * scaleFactor).intValue() + ybase
						def height = new BigDecimal((b - t) * scaleFactor).intValue()
						def xur = xbase + gsoff(curint.gsTop)
						def xlr = xbase + gsoff(curint.gsBase)
						
						def lithpoly = new Polygon()
						lithpoly.addPoint(xbase, y)
						lithpoly.addPoint(xur, y)
						lithpoly.addPoint(xlr, y + height)
						lithpoly.addPoint(xbase, y + height)
						
						//println "drawing poly at depth $t ($y y-coord) of height $height (bottom coord ${y + height})"

						g2.setPaint(entry.color) // background color
						g2.fill(lithpoly)
						
						if (entry.image) { // texture
							def rect = new java.awt.geom.Rectangle2D.Double(0, 0, entry.image.width / 2.0D, entry.image.height / 2.0D)
							g2.setPaint(new TexturePaint(entry.image, rect)) 
							g2.fill(lithpoly)
						}
						
						g2.setPaint(Color.BLACK) // section outline
						g2.draw(lithpoly)
						
						def OCC_SPACE = 2
						def occRowWidth = OCC_SPACE * 2 // pad between end of lithology and start of occurrence
						curint.occs.each { o ->
							def occEntry = getSchemeEntry(o.scheme?.scheme, o.scheme?.code)
							def maxx = Math.max(xur, xlr)
							if (occEntry && occEntry.image) {
								def occpoly = new Polygon()
								def lx = maxx + occRowWidth
								def ty = y
								def occheight = Math.min(occEntry.image.height as BigDecimal, height).intValue()
								def occwidth = (occheight < occEntry.image.height) ? (occEntry.image.width * (occheight / occEntry.image.height)).intValue() : occEntry.image.width
								occpoly.addPoint(lx, ty)
								occpoly.addPoint(lx + occwidth, ty)
								occpoly.addPoint(lx + occwidth, ty + occheight)
								occpoly.addPoint(lx, ty + occheight)
								
								occRowWidth += (occwidth + OCC_SPACE)

								def rect = new java.awt.geom.Rectangle2D.Double(lx, ty, occwidth, occheight)
								
								// draw white rectangle to avoid grain size scale lines showing in transparent regions of occurrence image
								// brg 2/20/2015: shelved
								//g2.setColor(Color.WHITE)
								//g2.fillRect(lx, ty, occwidth, occheight)
								
								g2.setPaint(new TexturePaint(occEntry.image, rect))
								g2.fill(occpoly)
							}
						}

						// draw section name - 2/20/2015 brg: shelving for now
//						if (drawSecName) {
//							g2.setFont(new Font("SansSerif", Font.PLAIN, 9))
//							g2.setPaint(Color.BLACK)
//							g2.drawString(secdata.section, MARGIN + 450, y + MARGIN)
//							drawSecName = false
//						}
					} else { println "No entry found" }
				} else { println "No lithology found" }
			}
		}
		
		g2.dispose();
		document.close();
	}
		
    def actions = [
		'chooseMetadata': { evt = null ->
			app.controllers['PSICAT'].withMVC('ChooseSectionMetadata', project:model.project, metadataFile:model.metadataFile) { mvc ->
				if (mvc.controller.show()) {
					model.metadataFile = mvc.model.metadataFile
					view.root.setPreferredSize(view.root.getPreferredSize())
				}
			}
		}
    ]

    def show() {
    	if (Dialogs.showCustomDialog("Export Strat Column", view.root, app.appFrames[0])) {
			export()
    	}
    }
}