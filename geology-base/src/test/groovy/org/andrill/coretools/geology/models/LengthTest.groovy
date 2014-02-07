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
package org.andrill.coretools.geology.models

import groovy.util.GroovyTestCase
import org.andrill.coretools.geology.models.Length

public class LengthTest extends GroovyTestCase {

	void testCreateDirect() {
		def l1 = new Length(1, "m")
		assert 1 == l1.value
		assert "m" == l1.unit
		
		def l2 = new Length(2, "ft")
		assert 2 == l2.value
		assert "ft" == l2.unit
	}
	
	void testCreateString() {
		def l1 = new Length("1 m")
		assert 1 == l1.value
		assert "m" == l1.unit
		
		def l2 = new Length("2 ft")
		assert 2 == l2.value
		assert "ft" == l2.unit
	}
	
	void testConvertMToCm() {
		def l1 = new Length("1 m")
		def l2 = l1.to("cm")
		def l3 = l1.cm
		
		assert 100 == l2.value
		assert "cm" == l2.unit

		assert 100 == l3.value
		assert "cm" == l3.unit
	}
	
	void testConvertMToFt() {
		def l1 = new Length("1 m")
		def l2 = l1.to("ft")
		def l3 = l1.ft
		
		assert 3.2808399 == l2.value
		assert "ft" == l2.unit

		assert 3.2808399 == l3.value
		assert "ft" == l3.unit
	}
	
	void testConvertFtToIn() {
		def l1 = new Length("1 ft")
		def l2 = l1.to("in")
		def l3 = l1."in"
		
		assert 12 == l2.value
		assert "in" == l2.unit

		assert 12 == l3.value
		assert "in" == l3.unit
	}

}
