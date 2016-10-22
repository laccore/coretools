import ca.odell.glazedlists.swing.EventListModel
import ca.odell.glazedlists.GlazedLists

import groovy.beans.Bindable

class ColorChooserModel {
	def schemeEntries = null
	def selectedColor = null
	def uniqueColors = []
	@Bindable def colorListModel = new EventListModel(GlazedLists.eventList([]))
}