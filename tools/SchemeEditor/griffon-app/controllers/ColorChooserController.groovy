import java.util.Map

import javax.swing.JColorChooser
import javax.swing.JOptionPane

import ca.odell.glazedlists.swing.EventListModel
import ca.odell.glazedlists.GlazedLists

class ColorChooserController {
	def model
	def view
	
	void mvcGroupInit(Map args) {
		model.selectedColor = args.selectedColor

		def colors = args.entries.collect { it.color }.findAll { it != null }
		model.uniqueColors = colors.unique { a, b -> a <=> b }.sort()
		updateList()
		//model.colorListModel = new EventListModel(GlazedLists.eventList(model.uniqueColors))
	}
	
	def show = { evt = null ->
		select(model.selectedColor)
		final int option = JOptionPane.showConfirmDialog(app.appFrames[0], [ view.colorChooser ].toArray(),
			"Choose Color", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
		if (option == JOptionPane.OK_OPTION)
			return view.colorList.selectedValue
		else
			return null
	}
	
	def newColor = { evt = null ->
		def color = JColorChooser.showDialog(view.colorChooser, "Choose Color", makeColor(view.colorList.selectedValue))
    	if (color) {
			def strColor = "${color.red},${color.green},${color.blue}"
			model.uniqueColors << strColor
			updateList()
			select(strColor)
    	}
	}
	
	def makeColor(colorStr) {
		return SchemeHelper.parseColorString(colorStr)
	}
	
	def updateList() {
		model.colorListModel = new EventListModel(GlazedLists.eventList(model.uniqueColors))
	}
	
	def select(color) {
		if (color) {
			view.colorList.setSelectedValue(color, true)
		}
	}
}