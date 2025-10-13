package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class Fractures extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String segments
	String lengths
	String displacements
	String mineralPrecipitates
	String description
	
	// widgets with matching group values appear in the same row
	static constraints = [
		top:			[handle: 'north', group: '1', widgetType: "LengthWidget", widgetProperties: [label: 'Range']],
		base:			[handle: 'south', group: '1', widgetType: "LengthWidget", widgetProperties: [label: '-']],
		scheme:			[nullable: true, widgetProperties: [schemeType: 'fractures']],
		segments:		[nullable: true],
		lengths:		[nullable: true],
		displacements:	[nullable: true],
		mineralPrecipitates: [nullable: true, widgetProperties: [label: 'Mineral precipitates']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}