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
import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelManager.Factory;

/**
 * The geology models factory.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class GeologyFactory implements Factory {
	private static final String[] TYPES = [Annotation.class, Interval.class, Occurrence.class, Unit.class, Image.class, Section.class].collect { it.simpleName } as String[]
	
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
			default:			return null
		}
	}
	 
	private def init(obj, data) {
		data.each { k,v ->
			if (obj.properties.containsKey(k)) { obj."$k" = v }
		}
		return obj
	}
	 
	// 1/13/2015 brg: Convert old Length-based grain sizes to BigDecimals, grain size is always mm
	private def initInterval(obj, data) {
		data.each { k,v ->
			if (obj.properties.containsKey(k) && ['grainSizeTop', 'grainSizeBase'].contains(k)) {
				def bdValue = null
				try { // v is a String that may be a BigDecimal or Length...try parsing as BigDecimal and treat as Length if it fails
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
