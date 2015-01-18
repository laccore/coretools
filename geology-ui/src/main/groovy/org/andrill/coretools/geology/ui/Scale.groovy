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
package org.andrill.coretools.geology.ui

import org.andrill.coretools.geology.models.Length

/**
 * A class for scaling values based on a scale definition in the form of 'value1<label1<value2<label2<value3', 
 * e.g. '0<Mud<0.0625<Sand<2<Gravel<4096'
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class Scale {
	// default to Phi scale (http://simple.wikipedia.org/wiki/Particle_size_%28grain_size%29)
	static DEFAULT = "0.2:0<Colloid<0.001<Clay<0.004<Silt<0.0625<Very fine sand" +
					 "<0.125<Fine sand<0.250<Medium sand<0.5<Coarse sand<1<Very coarse sand" +
					 "<2<Very fine gravel<4<Fine gravel<8<Medium gravel<16<Coarse gravel" +
					 "<32<Very coarse gravel<64<Cobble<256<Boulder<1024"
	
	def offset = 0.0
	def values = []
    def labels = []

	Scale(String code) {
		int colon = code.indexOf(':')
		if (colon > -1) {
			offset = code[0..<colon] as Double
			code = code.substring(colon + 1)
		}
		code.split("<").eachWithIndex { str, i ->
			if (i % 2 == 0) {
				values << (str as Double)
			} else {
				labels << str.trim()
			}
		}
	}

	double toScreen(value) {
		if (value <= 0) { return offset }
		for (i in 0..values.size) {
			if (value <= values[i]) {
				def pct = (value - values[i-1]) / (values[i] - values[i-1])
				def tick = (1.0 - offset) / labels.size
				return Math.min(1.0, pct * tick + (i - 1) * tick + offset)
			}
		}
		return 1.0
	}

	double toValue(screen) {
		if (screen >= 1.0) { return values.last() }
		def tick = (1.0 - offset) / labels.size
		def i = Math.floor(screen / tick) as int
		def pct = (screen - (i*tick + offset)) / tick
		return pct * (values[i+1] - values[i]) + values[i]
	}

	String getValueLabel(value) {
		return getScreenLabel(toScreen(value))
	}

	String getScreenLabel(screen) {
		if (screen <= offset) { return labels.first() }
		if (screen >= 1.0) { return labels.last() }
		return labels[Math.floor((screen - offset) / (1.0 - offset) * labels.size) as int] 
	}
}