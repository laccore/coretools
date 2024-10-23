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

import org.apache.log4j.Logger

import psicat.stratcol.StratColumnMetadata
import psicat.stratcol.StratColumnMetadataTypes as types
import psicat.stratcol.StratColumnMetadataUtils as utils
import psicat.stratcol.SpliceIntervalMetadataParser

import psicat.util.GeoUtils


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
	private static Logger logger = Logger.getLogger(SpliceIntervalMetadata.class)

	public SpliceIntervalMetadata(metadataPath) {
		this.metadataPath = metadataPath
	}
	public setLogger(_logger) {
		logger = _logger ?: Logger.getLogger(SpliceIntervalMetadata.class)
	}
	public int getType() { return types.SpliceIntervalFile }
	public String getTypeName() { return "Splice Interval" }

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

	def getContainers(project, includeModels=[]) {
		def containers = [:]
		this.metadata.eachWithIndex { secMap, idx ->
			logger.info("### Metadata Interval ${idx+1}: project sections ${secMap['sections'].collect{ it['projSec'] }} ###")
			logger.info("Start depth: ${secMap['startMcd']} m")
			logger.info("End depth: ${secMap['endMcd']} m")
			logger.info("Start section: ${secMap['startSec']} at section depth ${secMap['startSecDepth']} cm")
			logger.info("End section: ${secMap['endSec']} at section depth ${secMap['endSecDepth']} cm")
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

			logger.info("   Gathering components in section ${section['projSec']} range ${secTop} - ${secBase}...")

			def models = GeoUtils.getModels(project, sectionName).findAll { includeModels.contains(it.modelType) }
			def trimmedModels = GeoUtils.trimModels(project, models, trimMin, trimMax)

			if (trimmedModels.size() > 0) {
				totalModelLength += GeoUtils.getLength(trimmedModels)
				trimmedModels.add(new Section(name:sectionName, top:GeoUtils.getMinTop(trimmedModels), base:GeoUtils.getMaxBase(trimmedModels)))
				sectionModels[sectionName] = trimmedModels
			} else {
				logger.info("   Post-trimming, no components remained in $section.")
			}
		}
		BigDecimal intervalLength = secMap.endMcd - secMap.startMcd
		logger.info("Length of all included components = $totalModelLength m")
		
		if (totalModelLength > intervalLength) {
			BigDecimal scalingFactor = intervalLength / totalModelLength
			logger.info("Components are too long for metadata interval of length $intervalLength m. Downscaling by $intervalLength m / $totalModelLength m = $scalingFactor to fit.")
			sectionModels.each { key, modelList -> GeoUtils.scaleModels(modelList, scalingFactor) }
			logger.info("Downscaled components: $sectionModels")
		} else {
			logger.info("   Included components fit in metadata interval $intervalLength m, no scaling needed.")
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
