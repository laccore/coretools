package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class Pores extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String description
	
	// widgets with matching group values appear in the same row
	static constraints = [
		top:			[handle: 'north', group: '1', widgetType: "LengthWidget", widgetProperties: [label: 'Range']],
		base:			[handle: 'south', group: '1', widgetType: "LengthWidget", widgetProperties: [label: '-']],
		scheme:			[nullable: true, widgetProperties: [schemeType: 'pores']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}