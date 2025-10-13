package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class Pores extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String hostMaterial
	String size
	String shape
	String precipitates
	String description
	
	// widgets with matching group values appear in the same row
	static constraints = [
		top:			[handle: 'north', group: '1', widgetType: "LengthWidget", widgetProperties: [label: 'Range']],
		base:			[handle: 'south', group: '1', widgetType: "LengthWidget", widgetProperties: [label: '-']],
		scheme:			[nullable: true, widgetProperties: [schemeType: 'pores']],
		hostMaterial:	[nullable: true, widgetProperties: [label: 'Host Material']],
		size:			[nullable: true],
		shape:			[nullable: true],
		precipitates:	[nullable: true],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}