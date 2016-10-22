import java.util.Map

import java.awt.Color

import javax.swing.JColorChooser
import javax.swing.JOptionPane

import ca.odell.glazedlists.swing.EventListModel
import ca.odell.glazedlists.GlazedLists

class EntryColor extends Color implements Comparable {
	public EntryColor(Color c) { super(c.getRGB()) }
	public EntryColor(String colorStr) { super(SchemeHelper.parseColorString(colorStr).getRGB()) }
	public int compareTo(Object obj) {
		def c = (Color)obj
		return (this.red <=> c.red ?: this.green <=> c.green ?: this.blue <=> c.blue)
	}
	@Override
	public String toString() {
		return "${this.red},${this.green},${this.blue}"
	}
}

class ColorChooserController {
	def model
	def view
	
	void mvcGroupInit(Map args) {
		model.selectedColor = new EntryColor(args.selectedColor)

		def colors = args.entries.findAll { it.color != null }.collect { new EntryColor(it.color as String) }
		model.uniqueColors = colors.unique { a, b -> a <=> b }
		Collections.sort(model.uniqueColors)
		updateList()
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
		def color = JColorChooser.showDialog(view.colorChooser, "Choose Color", view.colorList.selectedValue)
    	if (color) {
			model.uniqueColors << new EntryColor(color)
			updateList()
			select(color)
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