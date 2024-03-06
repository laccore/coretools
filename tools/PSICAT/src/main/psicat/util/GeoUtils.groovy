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

package psicat.util

import java.util.List;

import au.com.bytecode.opencsv.CSVReader

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.geology.models.Interval
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.models.csdf.*
import org.andrill.coretools.geology.ui.Scale

import org.andrill.coretools.Platform
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.model.DefaultModelManager

class GeoUtils {
	private static logger = LoggerFactory.getLogger(GeoUtils.class)
	
	static setLogger(def logger) {
		GeoUtils.logger = logger ?: LoggerFactory.getLogger(GeoUtils.class)
	}
	
	// parse alternate grain size CSV file: row 1 should be a valid Scale string, remaining rows
	// consist of code and grain size columns. Returns map with 'scale' for scale string, 'gs' map 
	// of grain size values keyed on code
	static parseAlternateGrainSizeFile(altGSFile) throws Exception {
		CSVReader reader = null
		try {
			reader = new CSVReader(new FileReader(altGSFile))
		} catch (e) {
			throw new Exception("read/parse error: ${e.getMessage()}", e)
		}

		def result = [:]
		def gsmap = [:]
		if (reader) {
			def entries = reader.readAll()
			entries.eachWithIndex { row, index ->
				if (index == 0) { // parse and verify scale
					try {
						def testScale = new Scale(row[0])
					} catch (NumberFormatException e) {
						throw new Exception("Invalid grain size scale: ${e.getMessage()}", e)
					}
					result['scale'] = row[0]
				} else {
					def code = row[0]
					def gs = null
					try {
						gs = new BigDecimal(row[1])
					} catch (e) {
						throw new Exception("Row ${index + 1}: invalid grain size value '${row[1]}'", e)
					} 
					gsmap[code] = gs 
				}
			}
			result['gs'] = gsmap
			reader.close()
		}
		return result
	}

	/**
	 * Creates a new container with copies of all input containers' models
	 * (model data only). The copies can be freely manipulated without fear of
	 * corrupting the "true" set of models maintained in the project or
	 * disrupting listeners dependent on model/container association.
	 */
	static copyContainer(container) {
		def modelManager = Platform.getService(DefaultModelManager.class)
		def copy = Platform.getService(ModelContainer.class)
		container.models.each { m ->
			copy.add(modelManager.build(m.modelType, m.modelData))
		}
		//container.project = model.project // need project for e.g. grain size
		//container.models.sort { it.top }
		
		return copy
	}
	
	static zeroBaseContainer(container) {
		if (container.models.size() > 0) {
			GeoUtils.zeroBase(container.models)
		}
	}
	
	// find smallest top depth of models in modelList - subtract that
	// depth from all models in modelList
	static zeroBase(modelList) {
		def minDepth = null
		modelList.each {
			if (!minDepth || it.top.compareTo(minDepth) == -1)
				minDepth = it.top
		}
		modelList.each { m ->
			m.top -= minDepth
			m.base -= minDepth
		}
	}
	
	// cull models out of range, trim models that overlap range
	// min and max are section depths - Lengths in cm
	static getTrimmedModels(project, secname, min, max) {
		logger.info("Trimming $secname, min = $min, max = $max")
		def trimmedModels = []
		def projContainer = project.openContainer(secname)
		def cursec = copyContainer(projContainer)
		zeroBaseContainer(cursec)
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
					logger.info("   $mod out of range or base == $min, culling")
					continue;
				}
				if (mod.top.compareTo(min) == -1 && mod.base.compareTo(min) == 1) {
					logger.info("   $mod top above $min, trimming")
					mod.top = min.to(project.units)
					logger.info("$mod")
				}
			}
			if (max) {
				def cmp = mod.top.compareTo(max)
				if (cmp == 1 || cmp == 0) {
					logger.info("   $mod out of range or top == $max, culling")
					continue;
				}
				if (mod.top.compareTo(max) == -1 && mod.base.compareTo(max) == 1) {
					logger.info("   $mod bot below $max, trimming...")
					mod.base = max.to(project.units)
					logger.info("$mod")
				}
			}
			trimmedModels << mod
		}
		
		logger.info("   pre-zeroBase: trimmedModels = $trimmedModels")
		
		// now that we've trimmed, need to zero base *again* so scaling works properly
		zeroBase(trimmedModels)
		
		logger.info("   post-zeroBase: trimmedModels = $trimmedModels")
		return trimmedModels
	}
	
	static offsetModels(modelList, offset) {
		modelList.each {
			it.top += offset
			it.base += offset
		}
	}
	
	static scaleModels(modelList, scale) {
		modelList.each {
			it.top *= scale
			it.base *= scale
		}
	}

	// Assumes intervalLength is in meters
	static compressModels(models, intervalLength) {
		if (models.size() > 0) {
			def maxBase = getMaxBase(models)
			def scalingFactor = 1.0
			if (maxBase.value > 0.0) { // avoid divide by zero
				scalingFactor = intervalLength / maxBase.to('m').value
			}
			logger.info( "Interval length = $intervalLength, modelBase = $maxBase, scalingFactor = $scalingFactor")
			if (scalingFactor < 1.0) {
				logger.info("   Downscaling models...")
				scaleModels(models, scalingFactor)
				logger.info("   Downscaled: $models")
			} else {
				logger.info("   Scaling factor >= 1.0, leaving models as-is")
			}
		}
	}
	
	static getMinTop(modelList) {
		def min = null
		modelList.each {
			if (!min || it.top.compareTo(min) == -1)
				min = it.top
		}
		return min
	}
	
	static getMaxBase(modelList) {
		def max = null
		modelList.each {
			if (!max || it.base.compareTo(max) == 1)
				max = it.base
		}
		return max
	}
	
	// return meter distance between topmost and bottommost model in modelList 
	static getLength(modelList) {
		if (modelList.size() == 0)
			return 0

		def diff = getMaxBase(modelList) - getMinTop(modelList)
		return diff.to('m').value
	}
	
	// notify listeners of change by default, but provide option to avoid doing so
	// in cases where we need to temporarily adjust to section depth (e.g. auditing
	// project) without "modified" asterisk showing up in open diagrams. 
	static adjustUp(container, sectionTop, update=true) {
		container.models.each { m ->
			if (m.hasProperty('top') && m.top) {
				m.top = m.top - sectionTop
			}
			if (m.hasProperty('base') && m.base) {
				m.base = m.base - sectionTop
			}
			if (update)
				m.updated()
		}
	}
	
	static adjustDown(container, sectionTop, update=true) {
		container.models.each { m ->
			if (m.hasProperty('top') && m.top) {
				m.top = m.top + sectionTop
			}
			if (m.hasProperty('base') && m.base) {
				m.base = m.base + sectionTop
			}
			if (update)
				m.updated()
		}
	}

	// is model an in instance of an interval that requires contiguity?
	static isIntervalInstance(model) {
		return model instanceof Interval ||
			model instanceof BeddingInterval ||
			model instanceof GrainSizeInterval ||
			model instanceof LithologyInterval ||
			model instanceof UnitInterval
	}
}