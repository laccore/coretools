package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class CSDFGrainSizeInterval extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String description
	
	static constraints = [
		top:			[linkTo: 'base', handle: 'north', group:'1', widgetProperties: [label: 'Range']],
		base:			[linkTo: 'top', handle: 'south', group: '1', widgetProperties: [label: '-']],
		scheme:			[nullable: true, widgetProperties: ['schemeType': 'grainsize']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}