package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class BeddingInterval extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String description
	
	static constraints = [
		top:			[linkTo: 'base', handle: 'north', group: '1', widgetProperties: [label: 'Range', useProjectUnits: true]],
		base:			[linkTo: 'top', handle: 'south', group: '1', widgetProperties: [label: '-', useProjectUnits: true]],
		scheme:			[nullable: true, widgetProperties: ['schemeType': 'bedding']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}