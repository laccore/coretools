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

import java.util.List

import au.com.bytecode.opencsv.CSVReader

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.andrill.coretools.misc.io.BOMAwareCSVReader
import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.geology.models.Interval
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.geology.models.csdf.*
import org.andrill.coretools.geology.ui.Scale

import org.andrill.coretools.Platform
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.model.DefaultModelManager
import org.andrill.coretools.model.DefaultProject;

class GeoUtils {
	private static logger = LoggerFactory.getLogger(GeoUtils.class)
	
	static setLogger(def logger) {
		GeoUtils.logger = logger ?: LoggerFactory.getLogger(GeoUtils.class)
	}
	
	// parse alternate grain size CSV file: row 1 should be a valid Scale string, remaining rows
	// consist of code and grain size columns. Returns map with 'scale' for scale string, 'gs' map 
	// of grain size values keyed on code
	static parseAlternateGrainSizeFile(altGSFile) throws Exception {
		BOMAwareCSVReader reader = null
		try {
			reader = new BOMAwareCSVReader(new CSVReader(new FileReader(altGSFile)))
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
	static ModelContainer copyContainer(container) {
		def modelManager = Platform.getService(DefaultModelManager.class)
		def copy = Platform.getService(ModelContainer.class)
		container.models.each { m ->
			copy.add(modelManager.build(m.modelType, m.modelData))
		}
		//container.project = model.project // need project for e.g. grain size
		//container.models.sort { it.top }
		
		return copy
	}
	
	static void zeroBaseContainer(container) {
		if (container.models.size() > 0) {
			GeoUtils.zeroBase(container.models)
		}
	}
	
	// find smallest top depth of models in modelList - subtract that
	// depth from all models in modelList
	static void zeroBase(List<GeologyModel> models) {
		def minDepth = null
		models.each {
			if (!minDepth || it.top.compareTo(minDepth) == -1) {
				minDepth = it.top
			}
		}
		models.each { m ->
			m.top -= minDepth
			m.base -= minDepth
		}
	}

	static List<GeologyModel> getModels(DefaultProject project, String sectionName) {
		def projContainer = project.openContainer(sectionName)
		def section = copyContainer(projContainer)
		return section.getMutableModels()
	}

	// Trim models (in-place) that overlap the interval min - max, trimming out any
	// non-overlapping portions of each model. Then zero-base to remove top gap
	// resulting from trimming, if any.
	// min and max are section-relative depths i.e. section top is 0.
	// min and/or max can be null.
	static List<GeologyModel> trimModels(DefaultProject project, List<GeologyModel> models, Length min, Length max) {
		List<GeologyModel> trimmedModels = []
		zeroBase(models) // ensure models' top/base are section-relative
		logger.info("   Trimming components to range $min - $max...")
		for (GeologyModel mod : models) {
			if (min) {
				def cmp = mod.base.compareTo(min)
				if (cmp == -1) {
					logger.info("   $mod is out of range, culling")
					continue
				}
				if (cmp == 0) {
					logger.info.("  base of $mod is at min range $min, culling")
					continue
				}
				if (mod.top.compareTo(min) == -1 && mod.base.compareTo(min) == 1) {
					logger.info("   top of $mod is above min range $min, trimming")
					mod.top = min.to(project.units)
					logger.info("   trimmed component = $mod")
				}
			}
			if (max) {
				def cmp = mod.top.compareTo(max)
				if (cmp == 1) {
					logger.info("   $mod is out of range, culling")
					continue;
				}
				if (cmp == 0) {
					logger.info("   top of $mod is at max range $max, culling")
					continue
				}
				if (mod.top.compareTo(max) == -1 && mod.base.compareTo(max) == 1) {
					logger.info("   base of $mod is below max range $max, trimming...")
					mod.base = max.to(project.units)
					logger.info("   trimmed component = $mod")
				}
			}
			trimmedModels.add(mod)
		}
		zeroBase(trimmedModels)
		return trimmedModels
	}
	
	static void offsetModels(List<GeologyModel> models, Length offset) {
		models.each {
			it.top += offset
			it.base += offset
		}
	}
	
	static void scaleModels(List<GeologyModel> models, BigDecimal scale) {
		models.each {
			it.top *= scale
			it.base *= scale
		}
	}

	// Assumes intervalLength is in meters
	static void compressModels(List<GeologyModel> models, BigDecimal intervalLength) {
		if (models.size() > 0) {
			def maxBase = getMaxBase(models)
			def scalingFactor = 1.0
			if (maxBase.value > 0.0) { // avoid divide by zero
				scalingFactor = intervalLength / maxBase.to('m').value
			}
			logger.info("Length of all included components = ${maxBase.to('m').value} m")
			if (scalingFactor < 1.0) {
				logger.info("   Components are too long for metadata interval of length $intervalLength m. Downscaling by $intervalLength m / ${maxBase.to('m').value} m = $scalingFactor to fit.")
				scaleModels(models, scalingFactor)
				logger.info("   Downscaled components: $models")
			} else {
				logger.info("   Included components fit in metadata interval $intervalLength m, no scaling needed.")
			}
		}
	}
	
	static Length getMinTop(List<GeologyModel> models) {
		def min = null
		models.each {
			if (!min || it.top.compareTo(min) == -1) {
				min = it.top
			}
		}
		return min
	}
	
	static Length getMaxBase(List<GeologyModel> models) {
		def max = null
		models.each {
			if (!max || it.base.compareTo(max) == 1) {
				max = it.base
			}
		}
		return max
	}
	
	// return meter distance between topmost and bottommost model in modelList 
	static BigDecimal getLength(List<GeologyModel> models) {
		if (models.size() == 0) { return 0 }

		Length diff = getMaxBase(models) - getMinTop(models)
		return diff.to('m').value
	}
	
	// notify listeners of change by default, but provide option to avoid doing so
	// in cases where we need to temporarily adjust to section depth (e.g. auditing
	// project) without "modified" asterisk showing up in open diagrams. 
	static void adjustUp(container, sectionTop, update=true) {
		container.models.each { m ->
			if (m.hasProperty('top') && m.top) {
				m.top = m.top - sectionTop
			}
			if (m.hasProperty('base') && m.base) {
				m.base = m.base - sectionTop
			}
			if (update) {
				m.updated()
			}
		}
	}
	
	static void adjustDown(container, sectionTop, update=true) {
		container.models.each { m ->
			if (m.hasProperty('top') && m.top) {
				m.top = m.top + sectionTop
			}
			if (m.hasProperty('base') && m.base) {
				m.base = m.base + sectionTop
			}
			if (update) {
				m.updated()
			}
		}
	}

	// is model an in instance of an interval that requires contiguity?
	static boolean isIntervalInstance(model) {
		return model instanceof Interval ||
			model instanceof BeddingInterval ||
			model instanceof GrainSizeInterval ||
			model instanceof LithologyInterval ||
			model instanceof UnitInterval
	}
}