package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class Feature extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String description
	
	// widgets with matching group values appear in the same row
	static constraints = [
		top:			[handle: 'north', group:'1', widgetProperties: [label: 'Range']],
		base:			[handle: 'south', group:'1', widgetProperties: [label: '-']],
		scheme:			[nullable: true, widgetProperties: [schemeType: 'features']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}