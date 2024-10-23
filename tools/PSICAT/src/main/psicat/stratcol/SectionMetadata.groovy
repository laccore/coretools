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
import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.models.Section

import org.apache.log4j.Logger

import psicat.stratcol.SectionDrawData
import psicat.stratcol.StratColumnMetadata
import psicat.stratcol.StratColumnMetadataTypes as types
import psicat.stratcol.StratColumnMetadataUtils as utils

import psicat.util.GeoUtils

class SectionMetadata implements StratColumnMetadata {
	private metadataPath = null
	private metadata = null
	private static logger = Logger.getLogger(SectionMetadata.class)

	public SectionMetadata(metadataPath) {
		this.metadataPath = metadataPath
	}
	public setLogger(_logger) {
		this.logger = _logger ?: Logger.getLogger(SectionMetadata.class)
	}
	public int getType() { return types.SectionMetadataFile }
	public String getTypeName() { return "Section Metadata" }
	public getDrawData(project) {
		GeoUtils.setLogger(this.logger) 
		def drawData = createDrawData(project)
		GeoUtils.setLogger(null)
		return drawData
	}
	public mapSections(project) {
		parse(project.containers)
		return this.metadata
	}

	// consider only metadata sections which have a corresponding project section	
	public getTop() { return this.metadata?.findAll { it.section != null }.collect { it.top }.min() }
	public getBase() { return this.metadata?.findAll { it.section != null }.collect { it.base }.max() }
	
	// parse section top/base depth metadata file contents, return as a list of metadata maps, one per section
	private parse(projectSections) throws Exception {
		def metadata = []
		def metadataRows = utils.openMetadataFile(metadataPath)
		metadataRows.eachWithIndex { row, index ->
			def section = row[0]
			def projSection = utils.findSection(section, projectSections)
			// proceed, even if no matching project section is found - clients handle unmatched sections
			def top = null, base = null
			try {
				top = row[1] as BigDecimal
				base = row[2] as BigDecimal
				def secdata = ['metadataSection':section, 'section':projSection, 'top':top, 'base':base]
				metadata.add(secdata)
			} catch (e) {
				// ignore parsing error in first row, assume header row
				if (index > 0)
					throw new Exception("parsing error at row ${index + 1}: ${e.toString()}", e)
				else
					println "Parsing error in row 1 of $metadataPath - skipping on assumption it's a header row"
			}
		}
		
		this.metadata = metadata.sort { it.top }
	}
	
	// return list of top/base ranges and models to be drawn,
	// *only* for project sections mapped from metadata sections
	def createDrawData(project) {
		def intervalsToDraw = []
		this.metadata.findAll { it.section != null }.each {
			// gather and zero base models for each section
			def models = GeoUtils.getModels(project, it.section)
			compressToInterval(models, it.top, it.base)
			
			def intervalModels = [new SectionDrawData(it.section, it.top, it.base, models)]
			intervalsToDraw.add(['top':it.top, 'base':it.base, 'drawData':intervalModels])
		}
		
		return intervalsToDraw.sort { it.top }
	}

	def getContainers(project, includeModels=[]) {
		def containers = [:]
		this.metadata.each {
			def section = it['section']
			if (section != null) {
				BigDecimal top = it['top']
				BigDecimal base = it['base']

				logger.info("### Metadata section: $section ###")
				logger.info("Interval top: $top m")
				logger.info("Interval base: $base m")
				logger.info("Total interval length: ${base-top} m")

				// gather all requested models from the container
				def models = GeoUtils.getModels(project, section).findAll { includeModels.contains(it.modelType) }

				GeoUtils.zeroBase(models)
				compressToInterval(models, top, base)
				GeoUtils.offsetModels(models, new Length(top, 'm'))

				// Add Section with original section name and length of metadata interval.
				// Used to display each Section's name and interval in composite sections.
				models << new Section(name:section, top:new Length(top, 'm'), base:new Length(base, 'm'))

				def c = new DefaultContainer()
				c.addAll(models)
				containers[section] = c
			}
		}
		return containers
	}

	// If total length of models exceeds length of inteval top-base,
	// downscale models to fit.
	private void compressToInterval(List<GeologyModel> models, BigDecimal top, BigDecimal base) {
		BigDecimal intervalLength = base - top
		GeoUtils.compressModels(models, intervalLength)
	}
}