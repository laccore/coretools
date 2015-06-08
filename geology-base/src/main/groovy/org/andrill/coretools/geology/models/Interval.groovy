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

/**
 * Models intervals.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class Interval extends GeologyModel {
	Length top
	Length base
	BigDecimal grainSizeTop	 // grain size is always mm
	BigDecimal grainSizeBase
	SchemeRef lithology
	String description
	
	static constraints = [
		top:			[linkTo: 'base', handle: 'north', group:'1', widgetProperties: [label: 'Range']],
		base:			[linkTo: 'top', handle: 'south', group: '1', widgetProperties: [label: '-']],
		lithology:		[nullable: true, widgetProperties: ['schemeType': 'lithology']],
		grainSizeTop:	[nullable: true, group: '2', widgetProperties: [label: 'Grain Size', unitLabel: 'mm   to   ']],
		grainSizeBase:	[nullable: true, group: '2', widgetProperties: [label: '', unitLabel: 'mm']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}
