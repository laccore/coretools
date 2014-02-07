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
package org.andrill.coretools.dis.models;

import org.andrill.coretools.geology.models.GeologyModel;
import org.andrill.coretools.geology.models.Length;
import org.andrill.coretools.geology.models.SchemeRef;

class SectionUnit extends GeologyModel {
	String id = 'N/A'
	String name = 'N/A'
	Length top
	Length base
	SchemeRef unitType
	SchemeRef lithology
	SchemeRef grainSize1
	SchemeRef grainSize2
	SchemeRef grainSize3
	SchemeRef structure1
	SchemeRef structure2
	SchemeRef structure3
	SchemeRef texture1
	SchemeRef texture2
	SchemeRef texture3
	SchemeRef component1
	SchemeRef component2
	SchemeRef component3
	MunsellColor color1
	MunsellColor color2
	MunsellColor color3
	
	int ratio = 100
	String description
	
	static constraints = [
	    top:			[linkTo: 'base', handle: 'north', group: '1', widgetProperties: [label: 'Depth']],
	    base:			[linkTo: 'top', handle: 'south', group: '1', widgetProperties: [label: '-']],
		unitType:		[widgetProperties: ['schemeType': 'unitType', editable: true]],
	    lithology:		[widgetProperties: ['schemeType': 'lithology', editable: true], group: '2'],
		ratio:			[group: '2', widgetProperties: [label: '%']],
		grainSize1:		[nullable: true, widgetProperties: ['schemeType': 'grainSize', editable: true, label: 'Grain Size'], group: '3'],
		grainSize2:		[nullable: true, widgetProperties: ['schemeType': 'grainSize', editable: true, label: ''], group: '3'],
		grainSize3:		[nullable: true, widgetProperties: ['schemeType': 'grainSize', editable: true, label: ''], group: '3'],
		structure1:		[nullable: true, widgetProperties: ['schemeType': 'structures', editable: true, label: 'Structures'], group: '4'],
		structure2:		[nullable: true, widgetProperties: ['schemeType': 'structures', editable: true, label: ''], group: '4'],
		structure3:		[nullable: true, widgetProperties: ['schemeType': 'structures', editable: true, label: ''], group: '4'],
		texture1:		[nullable: true, widgetProperties: ['schemeType': 'textures', editable: true, label: 'Textures'], group: '5'],
		texture2:		[nullable: true, widgetProperties: ['schemeType': 'textures', editable: true, label: ''], group: '5'],
		texture3:		[nullable: true, widgetProperties: ['schemeType': 'textures', editable: true, label: ''], group: '5'],
		component1:		[nullable: true, widgetProperties: ['schemeType': 'components', editable: true, label: 'Components'], group: '6'],
		component2:		[nullable: true, widgetProperties: ['schemeType': 'components', editable: true, label: ''], group: '6'],
		component3:		[nullable: true, widgetProperties: ['schemeType': 'components', editable: true, label: ''], group: '6'],
		color1:			[nullable: true, group: '7', widgetProperties: [label: 'Color']],
		color2:			[nullable: true, group: '7', widgetProperties: [label: '']],
		color3:			[nullable: true, group: '7', widgetProperties: [label: '']],
	    description:	[nullable: true, widgetType: "TextArea"]
	]
	
	void setProperty(String name, value) {
		def metaProperty = metaClass.getMetaProperty(name)
		if (metaProperty) {
			if (value != null && metaProperty.type == MunsellColor.class && !(value instanceof MunsellColor)) {
				def v = value.toString()
				metaProperty.setProperty(this, new MunsellColor(v))
				return
			}
		}
		super.setProperty(name, value)
	}
}