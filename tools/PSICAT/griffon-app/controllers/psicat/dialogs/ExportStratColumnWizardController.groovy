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
package psicat.dialogs

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
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeEntry
import org.andrill.coretools.model.scheme.SchemeManager

import psicat.util.*

class ExportStratColumnWizardController {
    def model
    def view
	
	def PAGE_HEIGHT = 72 * 60 // 48"
	def PAGE_WIDTH = 620
	def MARGIN = 36 // 1/2" margin at 72dpi 
	def scaleFactor = 1.0

    void mvcGroupInit(Map args) {
    	model.project = args.project
    }

	def setScaleFactor(top, base) {
		def intervalLength = base - top
		scaleFactor = (PAGE_HEIGHT - MARGIN*2) / intervalLength
		//println "updated scaleFactor: $scaleFactor"
	}
	
	def getSchemeEntry(String schemeId, String code) {
		def scheme = Platform.getService(SchemeManager.class)?.getScheme(schemeId)
		scheme == null ? null : scheme.getEntry(code)
	}
	
	void updateGrainSizeRange(BigDecimal grainSize) {
		if (grainSize < model.gsMin) model.gsMin = grainSize
		if (grainSize > model.gsMax) model.gsMax = grainSize
	}
	
	def gsoff(grainSize) {
		def gs = (grainSize / (model.gsMax - model.gsMin) * 100) as Integer
		//println "offset = $gs" 
		return gs
	}
	
	def gsdef() { (model.gsMax - model.gsMin) / 2.0 }
	
	// brgtodo: do other preliminaries? e.g. collect used schemerefs for legend creation
	def prepareMetadata(sortedMetadata) {
		def occs = [:]
		sortedMetadata.each {
			def secOccs = []
			def section = model.project.openContainer(it.section)
			def modelIterator = section.iterator()
			while (modelIterator.hasNext()) {
				GeologyModel mod = modelIterator.next()
				if (mod.modelType.equals("Interval")) {
					//def gsTop = mod.grainSizeTop?.value
					//def gsBase = mod.grainSizeBase?.value
					if (mod.grainSizeTop) updateGrainSizeRange(mod.grainSizeTop)
					if (mod.grainSizeBase) updateGrainSizeRange(mod.grainSizeBase)
				} else if (mod.modelType.equals("Occurrence")) {
					secOccs << mod
				}
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
				def gsTop = mod.grainSizeTop ?: gsdef() // ignore unit from grain size and assume mm
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

	void export() {
		if (!model.metadataFile) return

		// create depth-sorted list of section/top/base vals
		def sortedMetadata = GeoUtils.parseMetadataFile(model.metadataFile)
		sortedMetadata = GeoUtils.reconcileSectionIDs(sortedMetadata, model.project)
		def occMap = prepareMetadata(sortedMetadata)
		
		// determine depth to pixel scaling
		setScaleFactor(sortedMetadata[0].top, sortedMetadata[-1].base)
		
		//sortedMetadata.each { println "${it['section']} ${it['top']} ${it['base']}" }
		
		// create PDF
		String filename = new String("/Users/bgrivna/Desktop/stratcolumn.pdf");
		Document document = new Document(new Rectangle(PAGE_WIDTH, PAGE_HEIGHT + 300));
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
		document.open();        
		PdfContentByte cb = writer.getDirectContent();
		cb.setLineWidth(0.1F)
		Graphics2D g2 = cb.createGraphics(PAGE_WIDTH, PAGE_HEIGHT + 300);
		g2.setFont(new Font("SansSerif", Font.PLAIN, 9))
		
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
				def pattern = curint.model.lithology
				if (pattern) {
					def entry = getSchemeEntry(pattern.scheme, pattern.code)
					if (entry) {
						def y = new BigDecimal(t * scaleFactor).intValue()
						def height = new BigDecimal((b - t) * scaleFactor).intValue()
						def xur = MARGIN + 300 + gsoff(curint.gsTop)
						def xlr = MARGIN + 300 + gsoff(curint.gsBase)
						
						def lithpoly = new Polygon()
						lithpoly.addPoint(MARGIN, y + MARGIN)
						lithpoly.addPoint(xur, y + MARGIN)
						lithpoly.addPoint(xlr, y + MARGIN + height)
						lithpoly.addPoint(MARGIN, y + MARGIN + height)

						g2.setPaint(entry.color) // background color
						g2.fill(lithpoly)
						
						if (entry.image) { // texture
							def rect = new java.awt.geom.Rectangle2D.Double(0, 0, entry.image.width / 2.0D, entry.image.height / 2.0D)
							g2.setPaint(new TexturePaint(entry.image, rect)) 
							g2.fill(lithpoly)
						}
						
						g2.setPaint(Color.BLACK) // section outline
						g2.draw(lithpoly)
						
						def OCC_SPACE = 5
						def occRowWidth = OCC_SPACE * 2 // pad between end of lithology and start of occurrence
						curint.occs.each { o ->
							def occEntry = getSchemeEntry(o.scheme?.scheme, o.scheme?.code)
							def maxx = Math.max(xur, xlr)
							if (occEntry && occEntry.image) {
								def occpoly = new Polygon()
								def lx = maxx + occRowWidth
								def ty = y + MARGIN
								def occheight = Math.min(occEntry.image.height as BigDecimal, height).intValue()
								def occwidth = (occheight < occEntry.image.height) ? occheight : occEntry.image.width
								occpoly.addPoint(lx, ty)
								occpoly.addPoint(lx + occwidth, ty)
								occpoly.addPoint(lx + occwidth, ty + occheight)
								occpoly.addPoint(lx, ty + occheight)
								
								occRowWidth += (occwidth + OCC_SPACE)

								def rect = new java.awt.geom.Rectangle2D.Double(lx, ty, occwidth, occheight)
								g2.setPaint(new TexturePaint(occEntry.image, rect))
								g2.fill(occpoly)
							}
						}
						
						if (drawSecName) {
							g2.setPaint(Color.BLACK)
							g2.drawString(secdata.section, MARGIN + 450, y + MARGIN)
							drawSecName = false
						}
						
					} else { println "No entry found" }
				} else { println "No lithology found" }
				
				// draw symbols (depth adjusted by sectionScale) whose top depth is within interval (original range) 
				
			}
		}
		
		// draw legend
		
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