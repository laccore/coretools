package org.andrill.coretools.geology.models.csdf

import org.andrill.coretools.geology.models.*

class Matrix extends GeologyModel {
	Length top
	Length base
	SchemeRef scheme
	String color
	String texture
	String bindingMatrix
	String description
	
	// widgets with matching group values appear in the same row
	static constraints = [
		top:			[handle: 'north', group: '1', widgetType: "LengthWidget", widgetProperties: [label: 'Range']],
		base:			[handle: 'south', group: '1', widgetType: "LengthWidget", widgetProperties: [label: '-']],
		scheme:			[nullable: true, widgetProperties: [schemeType: 'matrix']],
		color:			[nullable: true, widgetProperties: [label: 'Overall Color']],
		texture:		[nullable: true],
		bindingMatrix:	[nullable: true, widgetProperties: [label: 'Binding Matrix']],
		description:	[nullable: true, widgetType: "TextArea"]
	]
}