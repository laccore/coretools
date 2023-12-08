package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class GrainSizeInterval extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String description
	
	static constraints = [
		top:			[linkTo: 'base', handle: 'north', group: '1', widgetType: "LengthWidget", widgetProperties: [label: 'Range']],
		base:			[linkTo: 'top', handle: 'south', group: '1', widgetType: "LengthWidget", widgetProperties: [label: '-']],
		scheme:			[nullable: true, widgetProperties: ['schemeType': 'grainsize']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}