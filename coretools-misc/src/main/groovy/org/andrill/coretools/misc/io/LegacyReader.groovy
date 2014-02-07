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
package org.andrill.coretools.misc.io

import com.google.inject.Inject 

import java.io.InputStream

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.ModelManager;
import org.andrill.coretools.model.io.ModelReader;
import org.andrill.coretools.model.scheme.SchemeManager;

/**
 * Reads legacy PSICAT files.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class LegacyReader implements ModelReader {
	private static Logger logger = LoggerFactory.getLogger(LegacyReader.class)

	private final SchemeManager schemes
	private final ModelManager factory
	
	@Inject
	LegacyReader(SchemeManager schemes, ModelManager factory) {
		this.schemes = schemes
		this.factory = factory
	}
	
	String getFormat() { "legacy-psicat" }
	
	void read(ModelContainer container, InputStream stream) {
		long start = System.currentTimeMillis()
		def xml = new XmlSlurper()
		def models = xml.parse(stream)
		
		def intervals = [:]
        def lithologies = [:]
		
		models.model.each { m ->
			long each = System.currentTimeMillis()
			String type = m?.@type.text()
			def model
			if (type.endsWith("Unit")) { 
				model = make("Unit", m, [group: 'lsu']) 
			} else if (type.endsWith("Facies"))	{ 
				model = make("Unit", m, [group: 'facies']) 
			} else if (type.endsWith("DrillingDisturbance")) {
				model = make("Occurrence", m, [group: 'disturbance'])
			} else if (type.endsWith("Symbol")) {
				model = make("Occurrence", m)
			} else if (type.endsWith("Clast") || type.endsWith("Nest")) {
				model = make("Occurrence", m, [group:'clast'])
			} else if (type.endsWith("Bioturbation")) {
				model = make("Occurrence", m, [group:'bioturbation'])
			} else if (type.endsWith("Comment")) {
				model = make("Annotation", m)
			} else if (type.endsWith("Interval")) {
				def id = m.@id.text()
				if (intervals.containsKey(id)) {
					intervals[id].node = m
				} else {
					intervals[id] = [node:m]
				}
				
			} else if (type.endsWith("Bed")) {
				def parent = m.@parentId.text()
				if (intervals.containsKey(parent)) {
					def interval = intervals[parent]
                    if (interval.containsKey('beds')) {
                    	interval.beds << m
                    } else {
                    	interval.beds = [m]
                    }
				} else {
					intervals[parent] = [beds:[m]]
				}
			} else if (type.endsWith("Lithology")) {
				def parent = m.@parentId.text()
				def ratio = m.property.find { it?.@name == 'ratio' }?.text() ?: '1.0' as Double
				def code = m.property.find { it?.@name == 'keywords' }.text()
				if (!lithologies.containsKey(parent) || ratio > lithologies[parent].ratio) {
					lithologies[parent] = [ratio: ratio, code: code]
				}
			} else {
				logger.warn("Unhandled model type: {}", type)
			}

			if (model) { container.add(model) }
		}

		// process our intervals
		intervals.each { k,v ->
			// create our interval
			def interval = make("Interval", v.node)
			if (lithologies.containsKey(k)) { 
				interval.lithology = guess('lithology', lithologies[k].code) 
			}

			// split on beds
			if (v.beds) {
				// make all the bed nodes into Interval models
				def beds = v.beds.collect { b ->
					def bed = make("Interval", b)
					def id = b.@id.text()
					if (lithologies.containsKey(id)) {
						bed.lithology = guess('lithology', lithologies[id].code)
					}
					return bed
				}.sort { a,b -> a?.top?.value <=> b?.top?.value }

				// track the top
				def d = interval.top.value
				beds.eachWithIndex { b, i ->
					// split the interval before the bed if necessary
					if (d < b.top.value) { 
						def clone = clone(interval, d, b.top.value)
						if (i == 0) {
							clone.description = interval?.description
						}
						container.add(clone) 
					}
					container.add(b)
					d = b.base.value
				}
				if (d < interval.base.value) { container.add(clone(interval, d, interval.base.value)) }
			} else {
				container.add(interval)
			}
		}
		
		logger.info("Legacy data converted in {} ms", (System.currentTimeMillis() - start))
	}

	private def clone(model, top, base) {
		def data = model.modelData
		data.top = "$top m"
		data.base = "$base m"
		data.remove("description")
		factory.build(model.modelType, data)
	}

	private def make(type, node, template = [:]) {
		def data = [:]
        data.putAll(template)
        
        def clast = [:]
        def bi = [:]
        node.property.each { p ->
        	def name = p?.@name?.text()
        	switch (name) {
        		case "depth.top":		data.top = p.text() + " m"; break
        		case "depth.base":		data.base = p.text() + " m"; break
        		case "description":		data.description = p.text(); break
        		case "grainsize.top":	data.grainSizeTop = grainsize((p.text() as Double)); break
        		case "grainsize.base":	data.grainSizeBase = grainsize((p.text() as Double)); break
        		case "label":			data.name = p.text(); break
        		case "facies.code":		data.scheme = guess("facies", p.text()); break
        		case "keywords":		data.scheme = guess("symbol", p.text()); break
        		case "disturbance.type":data.scheme = guess("disturbance", p.text().replace('-', ',')); break
        		case "size":			clast.s = p.text(); break
        		case "roundness":		clast.r = p.text(); break
        		case "lithology":		clast.l = p.text(); break
        		case "bi.top":			bi.top = p.text(); break
        		case "bi.base":			bi.base = p.text(); break
        		case "contact.top":		break	// ignored
        		case "contact.base":	break	// ignored
        		case "contact.type":	break	// ignored
        		case "contact.angle":	break	// ignored
        		case "interpretation":	break	// ignored
        		case "count":			break	// ignored
        		default:				logger.warn("Unhandled property: {}", name)
        	}
        }

		if (node?.@type?.text().endsWith("Clast")) {
			data.scheme = guess("clast", "${clast.s ?: ''},${clast.r ?: ''},${clast.l ?: ''}")
		} else if (node?.@type?.text().endsWith("Bioturbation")) {
			data.scheme = guess("bioturbation", "${bi.top ?: ''},${bi.base ?: ''}")
		}

		def model = factory.build(type, data)
		if (!model) {
			logger.warn("Unable to create model type: {}", type)
		}
		return model
	}

	private def grainsize(phi) {
		if (phi >= 14) { 
			return null
		} else {
			return "${Math.pow(2, -phi)} mm"
		}
	}

	private def guess(type, code) {
		// remove modifiers
		def split = code.toLowerCase().split(',') as List
		if (split[0] == 'clay') {
			split.remove(0)
		}
		split.remove('fragmented')
		split.remove('?')
		split.remove('stain')
		code = split.join(',')
		
		def guess
		for (scheme in schemes.getSchemes(type)) {
			guess = scheme.get(code)
			if (guess) break
		}
		
		if (guess) {
			return "${guess.scheme.id}:${guess.code}"
		} else {
			logger.warn("Unable to find scheme entry for '{}' in {}", code, type)
			return "psicat.$type:$code"
		}
	}
}
