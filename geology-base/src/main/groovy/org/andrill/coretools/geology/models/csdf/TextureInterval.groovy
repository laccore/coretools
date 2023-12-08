package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class TextureInterval extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String description
	
	static constraints = [
		top:			[handle: 'north', group: '1', widgetType: "LengthWidget", widgetProperties: [label: 'Range']],
		base:			[handle: 'south', group: '1', widgetType: "LengthWidget", widgetProperties: [label: '-']],
		scheme:			[nullable: true, widgetProperties: ['schemeType': 'texture']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}