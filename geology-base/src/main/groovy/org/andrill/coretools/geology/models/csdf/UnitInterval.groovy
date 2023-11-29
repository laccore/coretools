package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class UnitInterval extends GeologyModel {
	Length top
	Length base
	String name
	String description
	
	static constraints = [
		top:			[linkTo: 'base', handle: 'north', group: '1', widgetType: "LengthWidget", widgetProperties: [label: 'Range']],
		base:			[linkTo: 'top', handle: 'south', group: '1', widgetType: "LengthWidget", widgetProperties: [label: '-']],
		name:			[nullable: true],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}
