package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class Feature extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String description
	
	static constraints = [
		top:			[handle: 'north'],
		base:			[handle: 'south'],
		scheme:			[nullable: true, widgetProperties: [schemeType: 'features']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}