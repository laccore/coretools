package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class UnitInterval extends GeologyModel {
	Length top
	Length base
	String name
	String description
	
	static constraints = [
		top:			[handle: 'north', linkTo: 'base'],
		base:			[handle: 'south', linkTo: 'top'],
		name:			[nullable: true],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}
