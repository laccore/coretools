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

import java.math.RoundingMode

import java.text.NumberFormat;
import java.util.Locale;

import groovy.lang.MissingPropertyException

/**
 * Models a length, which has a value and a unit, and can be converted to different units.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */class Length {
	private static Map CONVERSIONS = ["m":1, "cm":100, "mm":1000, "dm":0.1, "hm":0.01, "km":0.001, "in":39.3700787, "ft":3.2808399, "yd":1.0936133]
	BigDecimal value
	String unit

	static boolean validLength(String str) {
		def numericRegex = "-?\\d*(\\.\\d+)?" // numeric portion
		def unitRegex = " *(" + CONVERSIONS.keySet().join("|") + "){0,1} *" // optional unit portion with 0+ leading/trailing spaces
		return str.matches(numericRegex + unitRegex)
	}
	
	Length(BigDecimal value) { 
		this(value, "m") 
	}
	
	Length(BigDecimal value, String unit) {
		this.value = value
		this.unit = unit
	}
	
	Length(String str) {
		this(str, Locale.getDefault())
	}
	
	Length(String str, Locale locale) {
		NumberFormat format = NumberFormat.getInstance(locale ?: Locale.getDefault())
		str = str.trim().toLowerCase()
		if (str.isNumber()) {
			value = format.parse(str) as BigDecimal
			unit = "m"
		} else if (CONVERSIONS.keySet().contains(str[-2..-1])) {
			value = format.parse(str[0..-3]) as BigDecimal
			unit = str[-2..-1]
		} else if (str.endsWith("m")) {
			value = format.parse(str[0..-2]) as BigDecimal
			unit = "m"
		}
	}
	
	String toString() { 
		"${value.toPlainString()} $unit" 
	}
	
	Length to(String newUnit) {
		if (newUnit == unit) {
			return this
		} else {
			new Length(new BigDecimal(value / CONVERSIONS[unit] * CONVERSIONS[newUnit]).setScale(7, RoundingMode.HALF_UP), newUnit)
		}
	}
	
	boolean equals(other) { 
		value == other?.value && unit == other?.unit 
	}
	
	int compareTo(other) {
		def c = other.to(unit)
		return value.compareTo(c.value)
	}

	def propertyMissing(String name) {
		if (CONVERSIONS.keySet().contains(name)) {
			return to(name)
		} else {
			throw new MissingPropertyException(name, Length.class)
		}
	}

	// arithmetic operations
	def plus(Number num)		{ new Length(value + num, unit) }
	def plus(Length other)		{ new Length(value + other.to(unit).value, unit) }
	def minus(Number num)		{ new Length(value - num, unit) }
	def minus(Length other)		{ new Length(value - other.to(unit).value, unit) }
	def multiply(Number num)	{ new Length(value * num, unit) }
	def divide(Number num)		{ new Length(value / num, unit) }
}
