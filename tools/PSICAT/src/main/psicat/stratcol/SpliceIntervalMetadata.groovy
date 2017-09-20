/*
 * Copyright (c) Brian Grivna, 2016.
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

package psicat.stratcol

import org.andrill.coretools.geology.models.Length

import psicat.stratcol.SectionDrawData
import psicat.stratcol.StratColumnMetadata
import psicat.stratcol.StratColumnMetadataTypes as types
import psicat.stratcol.StratColumnMetadataUtils as utils
import psicat.stratcol.SpliceIntervalReader

import psicat.util.GeoUtils

class SpliceIntervalMetadata implements StratColumnMetadata {
	public static final String Site = "Site"
	public static final String Hole = "Hole"
	public static final String Core = "Core"
	public static final String CoreType = "Core Type"
	public static final String TopSection = "Top Section";
	public static final String TopOffset = "Top Offset";
	public static final String TopCCSF = "Top Depth CCSF-A";
	public static final String BottomSection = "Bottom Section";
	public static final String BottomOffset = "Bottom Offset";
	public static final String BottomCCSF = "Bottom Depth CCSF-A";
	
	public static boolean isValid(csvreader) {
		def reader = new SpliceIntervalReader(csvreader)
		return reader.hasColumns([Site, Hole, Core, CoreType, TopSection, TopOffset, TopCCSF, BottomSection, BottomOffset, BottomCCSF])
	}
	
	private metadataPath = null
	private metadata = null
	private sectionMapping = null
	public SpliceIntervalMetadata(metadataPath) {
		this.metadataPath = metadataPath
	}
	public int getType() { return types.SpliceIntervalFile }
	public String getTypeName() { return "Splice Interval" }
	public getDrawData(project) { return createDrawData(project) }
	public getTop() {
		return this.metadata?.collect { it.startMcd }.min()
	}
	public getBase() {
		return this.metadata?.collect { it.endMcd }.max()
	}
	public mapSections(project) {
		parse(project)
		return this.sectionMapping
	}
	
	// project - project to search for sections matching metadata sections
	def parse(project) throws Exception {
		def sectionMapping = []
		def metadata = []
		def reader = new SpliceIntervalReader(utils.openMetadataFile(metadataPath))
		reader.readAll().eachWithIndex { row, rowIndex ->
			def startSecDepth, endSecDepth, startMcd, endMcd
			try {
				startSecDepth = row[TopOffset] as BigDecimal
				endSecDepth = row[BottomOffset] as BigDecimal
				startMcd = row[TopCCSF] as BigDecimal
				endMcd = row[BottomCCSF] as BigDecimal
			} catch (e) {
				throw new Exception("Couldn't convert text to number at row ${rowIndex + 1}: ${e.toString()}", e)
			}
			
			//println "Interval $index:"
			def startSec = makeSectionName(row, TopSection)
			//println "   Start section: $startSec at depth $startSecDepth"
			def endSec = makeSectionName(row, BottomSection)
			//println "   End section: $endSec at depth $endSecDepth"
			
			def projectSections = []
			def sectionNames = getSectionNames(startSec, endSec)
			sectionNames.eachWithIndex { it, sectionIndex ->
				def projSec = utils.findSection(it, project.containers, true)
				sectionMapping << ['metadataSection':it, 'section':projSec]
				
				if (projSec) { projectSections << projSec }
			}
			if (projectSections.size() > 0) {
				metadata << ['startMcd':startMcd, 'endMcd':endMcd, 'startSecDepth':startSecDepth, 'endSecDepth':endSecDepth, 'sections':projectSections]
			}
		}
		
		this.metadata = metadata.sort { it.startMcd }
		this.sectionMapping = sectionMapping
	}
	
	def createDrawData(project) {
		def drawData = []
		this.metadata.eachWithIndex { secMap, index ->
			def secTop = new Length(secMap.startSecDepth, 'cm')
			def secBase = new Length(secMap.endSecDepth, 'cm')
			def sectionModels = [:]
			def totalModelLength = 0
			secMap.sections.eachWithIndex { sec, secIndex ->
				def trimmedModels = GeoUtils.getTrimmedModels(project, sec, secIndex == 0 ? secTop : null, secIndex == secMap.sections.size() - 1 ? secBase : null)
				totalModelLength += GeoUtils.getLength(trimmedModels)
				sectionModels[sec] = trimmedModels
			}
			def intervalLength = secMap.endMcd - secMap.startMcd
			println "totalLength = $totalModelLength vs. interval length of ${secMap.endMcd} - ${secMap.startMcd} = $intervalLength"
			
			if (totalModelLength > intervalLength || totalModelLength == 0) {
				def scalingFactor = intervalLength / totalModelLength
				sectionModels.each { key, modelList -> GeoUtils.scaleModels(modelList, scalingFactor) }
			}
			
			def intervalModels = []
			def top = secMap.startMcd
			sectionModels.each { section, models ->
				def base = top + GeoUtils.getLength(models)
				def sdd = new SectionDrawData(section, top, base, models)
				println "Created $sdd"
				intervalModels << sdd
				top = base
			}
			
			drawData << ['top':secMap.startMcd, 'base':secMap.endMcd, 'siIntervals':intervalModels]
		}
		return drawData.sort { it.top }
	}
	
	private makeSectionName(siRow, secCol, expName=null) {
		def site = siRow[Site]
		def hole = siRow[Hole]
		def core = siRow[Core]
		def tool = siRow[CoreType]
		def sec = siRow[secCol]
		return (expName ? "$expName-" : "") + "$site$hole-$core$tool-$sec"
	}
	
	private parseSection(fullSectionName) {
		return fullSectionName.substring(fullSectionName.lastIndexOf('-') + 1)
	}
	
	// return list of section names, starting with startSec, ending with endSec,
	// and including any sections that fall between the two
	private getSectionNames(startSec, endSec) {
		def startnum, endnum
		try {
			startnum = parseSection(startSec) as Integer
			endnum = parseSection(endSec) as Integer
		} catch (e) {
			//errbox("Parse Error", "Couldn't get section number")
			return []
		}
		
		if (endnum < startnum) {
			//errbox("Data Error", "End section $endnum precedes start section $startnum")
			return []
		}
		
		def sections = []
		def baseName = startSec.substring(0, startSec.lastIndexOf('-') + 1)
		(startnum..endnum).each { sections <<  baseName + it }
		
		return sections
	}	
}
