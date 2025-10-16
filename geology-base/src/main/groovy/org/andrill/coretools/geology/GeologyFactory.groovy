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
package org.andrill.coretools.geology

import org.andrill.coretools.geology.models.*
import org.andrill.coretools.geology.models.csdf.*
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.ModelManager.Factory

/**
 * The geology models factory.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class GeologyFactory implements Factory {
	private static final String[] TYPES = [
		Annotation.class, Interval.class, Occurrence.class, Unit.class, Image.class, Section.class,
		BeddingInterval.class, GrainSizeInterval.class, TextureInterval.class, Feature.class, LithologyInterval.class, UnitInterval.class,
		Caementa.class, Mortar.class, Matrix.class, Pores.class, Fractures.class, Discontinuity.class
	].collect { it.simpleName } as String[]
	
	public GeologyFactory() { }
	
	/**
	 * {@inheritDoc}
	 */
	Model build(String type, Map<String, String> data) {
		switch (type) {
			case 'Annotation':	return init(new Annotation(), data)
			case 'Occurrence':	return init(new Occurrence(), data)
			case 'Unit': 		return init(new Unit(), data)
			case 'Interval':	return initInterval(new Interval(), data)
			case 'Image':		return init(new Image(), data)
			case 'Section':		return init(new Section(), data)
			case 'BeddingInterval': return init(new BeddingInterval(), data)
			case 'TextureInterval': return init(new TextureInterval(), data)
			case 'GrainSizeInterval': return init(new GrainSizeInterval(), data)
			case 'Feature': return init(new Feature(), data)
			case 'LithologyInterval': return init(new LithologyInterval(), data)
			case 'UnitInterval': return init(new UnitInterval(), data)
			case 'Caementa': return init(new Caementa(), data)
			case 'Mortar': return init(new Mortar(), data)
			case 'Matrix': return init(new Matrix(), data)
			case 'Pores': return init(new Pores(), data)
			case 'Fractures': return init(new Fractures(), data)
			case 'Discontinuity': return init(new Discontinuity(), data)
			default:			return null
		}
	}

	// Set properties for all model types, except old (Andrill) Interval lithology models.
	private def init(obj, data) {
		data.each { k,v ->
			if (obj.properties.containsKey(k)) { obj."$k" = v }
		}
		return obj
	}

	// Set properties for old (Andrill) Interval lithology models, which include properties
	// requiring special handling. At one time, grainSizeTop and grainSizeBase were Lengths,
	// which caused all manner of confusion. Now those properties are converted to BigDecimals,
	// with unit assumed to be mm.
	private def initInterval(obj, data) {
		data.each { k,v ->
			if (obj.properties.containsKey(k) && ['grainSizeTop', 'grainSizeBase'].contains(k)) {
				def bdValue = null
				// String v may be a BigDecimal or, for older projects, a Length.
				// Try parsing as BigDecimal. If it fails, treat as a Length.
				try {
					bdValue = new BigDecimal(v)
					obj."$k" = v
				} catch (NumberFormatException nfe) {
					def len = new Length(v)	
					obj."$k" = len.value
				}
			} else {
				obj."$k" = v
			}
		}
		return obj
	}
		
	/**
	 * {@inheritDoc}
	 */
	public String[] getTypes() { TYPES }
}
