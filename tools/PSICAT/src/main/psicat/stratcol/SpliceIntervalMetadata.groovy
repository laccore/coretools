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

import org.andrill.coretools.model.DefaultContainer
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.models.Section

import psicat.stratcol.SectionDrawData
import psicat.stratcol.StratColumnMetadata
import psicat.stratcol.StratColumnMetadataTypes as types
import psicat.stratcol.StratColumnMetadataUtils as utils
import psicat.stratcol.SpliceIntervalMetadataParser

import psicat.util.GeoUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SpliceIntervalMetadata implements StratColumnMetadata {
	public static final String Site = "Site"
	public static final String Hole = "Hole"
	public static final String Core = "Core"
	public static final String CoreType = "Core Type"
	public static final String Tool = "Tool"
	public static final String TopSection = "Top Section"
	public static final String TopOffset = "Top Offset"
	public static final String TopCCSF = "Top Depth CCSF-A"
	public static final String BottomSection = "Bottom Section"
	public static final String BottomOffset = "Bottom Offset"
	public static final String BottomCCSF = "Bottom Depth CCSF-A"
	
	public static boolean isValid(mdRows) {
		def parser = new SpliceIntervalMetadataParser(mdRows)
		def alts = [(CoreType): Tool] // accepted alternate names
		return parser.hasColumns([Site, Hole, Core, CoreType, TopSection, TopOffset, TopCCSF, BottomSection, BottomOffset, BottomCCSF], alts)
	}
	
	private metadataPath = null
	private metadata = null
	private sectionMapping = null
	private toolHeaderName = null
	private logger = LoggerFactory.getLogger(SpliceIntervalMetadata.class)

	public SpliceIntervalMetadata(metadataPath) {
		this.metadataPath = metadataPath
	}
	public setLogger(logger) { this.logger = logger }
	public int getType() { return types.SpliceIntervalFile }
	public String getTypeName() { return "Splice Interval" }
	public getDrawData(project) { 
		GeoUtils.setLogger(logger)
		def drawData = createDrawData(project)
		GeoUtils.setLogger(null)
		return drawData
	}
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
		def parser = new SpliceIntervalMetadataParser(utils.openMetadataFile(metadataPath))
		this.toolHeaderName = parser.toolHeaderName
		parser.getRows().eachWithIndex { row, rowIndex ->
			BigDecimal startSecDepth, endSecDepth, startMcd, endMcd
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
				
				if (projSec) { projectSections << ['projSec':projSec, 'sectionNum':parseSection(it)] }
			}
			if (projectSections.size() > 0) {
				metadata << ['startMcd':startMcd, 'endMcd':endMcd, 'startSecDepth':startSecDepth, 'startSec':row[TopSection],
					'endSec':row[BottomSection], 'endSecDepth':endSecDepth, 'sections':projectSections]
			}
		}
		
		this.metadata = metadata.sort { it.startMcd }
		this.sectionMapping = sectionMapping
	}
	
	// TODO: Old strat column logic, purge throughout this file!
	// return list of draw data maps, each of form ['top':top MCD depth, 'base':bottom MCD depth, 
	// 'drawData':list of SectionDrawData to be drawn in that range]
	def createDrawData(project) {
		def drawData = []
		this.metadata.each { secMap ->
			def sectionModels = gatherModels(project, secMap, [])
			
			def intervalModels = []
			def top = secMap.startMcd
			sectionModels.each { section, models ->
				def base = top + GeoUtils.getLength(models)
				def sdd = new SectionDrawData(section, top, base, models)
				logger.info("Created $sdd")
				intervalModels << sdd
				top = base
			}
			
			drawData << ['top':secMap.startMcd, 'base':secMap.endMcd, 'drawData':intervalModels]
		}
		return drawData.sort { it.top }
	}

	def getContainers(project, includeModels=[]) {
		def containers = [:]
		this.metadata.each { secMap ->
			def sectionModels = gatherModels(project, secMap, includeModels)

			// offset all models by secMap.startMcd and throw them in a container
			def top = secMap.startMcd
			sectionModels.each { sectionName, modelList ->
				GeoUtils.offsetModels(modelList, new Length(top, 'm'))
				def c = new DefaultContainer()
				c.addAll(modelList)
				containers[sectionName] = c

				def base = top + GeoUtils.getLength(modelList)
				top = base
			}
		}
		return containers
	}

	// return a list of models within secMap.startSecDepth and secMap.endSecDepth, trimmed
	// to fit start/endSecDepth and downscaled to fit the secMap.startMcd to secMap.endMcd interval
	private gatherModels(project, secMap, includeModels) {
		def sectionModels = [:]
		Length secTop = new Length(secMap.startSecDepth, 'cm')
		Length secBase = new Length(secMap.endSecDepth, 'cm')
		BigDecimal totalModelLength = 0
		secMap.sections.eachWithIndex { section, secIndex ->
			String sectionName = section.projSec
			String sectionNum = section.sectionNum
			Length trimMin = (sectionNum.equals(secMap.startSec)) ? secTop : null
			Length trimMax = (sectionNum.equals(secMap.endSec)) ? secBase : null

			def models = GeoUtils.getModels(project, sectionName).findAll { includeModels.contains(it.modelType) }
			GeoUtils.trimModels(project, models, trimMin, trimMax)

			if (models.size() > 0) {
				totalModelLength += GeoUtils.getLength(models)
				models.add(new Section(name:sectionName, top:GeoUtils.getMinTop(models), base:GeoUtils.getMaxBase(models)))
				sectionModels[sectionName] = models
			} else {
				logger.info("No trimmed models found in $section, skipping.")
			}
		}
		BigDecimal intervalLength = secMap.endMcd - secMap.startMcd
		logger.info("totalLength = $totalModelLength vs. interval length of ${secMap.endMcd} - ${secMap.startMcd} = $intervalLength")
		
		if (totalModelLength > intervalLength) {
			BigDecimal scalingFactor = intervalLength / totalModelLength
			sectionModels.each { key, modelList -> GeoUtils.scaleModels(modelList, scalingFactor) }
		}
		return sectionModels
	}

	private String makeSectionName(siRow, String secCol, String expName=null) {
		def site = siRow[Site]
		def hole = siRow[Hole]
		def core = siRow[Core]
		def tool = siRow[this.toolHeaderName]
		def sec = siRow[secCol]
		return (expName ? "$expName-" : "") + "$site$hole-$core$tool-$sec"
	}
	
	private String parseSection(String fullSectionName) {
		return fullSectionName.substring(fullSectionName.lastIndexOf('-') + 1)
	}
	
	// return list of section names, starting with startSec, ending with endSec,
	// and including any sections that fall between the two
	private List<String> getSectionNames(String startSec, String endSec) {
		def startnum, endnum
		try {
			startnum = parseSection(startSec) as Integer
			endnum = parseSection(endSec) as Integer
		} catch (e) {
			return []
		}
		
		if (endnum < startnum) {
			return []
		}
		
		def sections = []
		def baseName = startSec.substring(0, startSec.lastIndexOf('-') + 1)
		(startnum..endnum).each { sections <<  baseName + it }
		
		return sections
	}	
}
