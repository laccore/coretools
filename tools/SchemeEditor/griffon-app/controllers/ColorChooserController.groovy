import java.util.Map

import java.awt.Color

import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
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

class ColorChooserController implements ListSelectionListener {
	def model
	def view
	
	void mvcGroupInit(Map args) {
		model.schemeEntries = args.entries
		model.selectedColor = new EntryColor(args.selectedColor ?: Color.WHITE)
		def entryColors = model.schemeEntries.findAll { it.color != null }.collect { new EntryColor(it.color as String) }
		updateList(entryColors)
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
			updateList(model.uniqueColors)
			select(color)
    	}
	}
	
	def makeColor(colorStr) {
		return SchemeHelper.parseColorString(colorStr)
	}
	
	def updateList(sourceColors) {
		model.uniqueColors = sourceColors.unique { a, b -> a <=> b }
		Collections.sort(model.uniqueColors)
		model.colorListModel = new EventListModel(GlazedLists.eventList(model.uniqueColors))
	}
	
	def select(color) {
		if (color) {
			view.colorList.setSelectedValue(color, true)
		}
	}
	
	def normalizeColors = { evt = null ->
		String normColorStr = JOptionPane.showInputDialog(view.colorChooser,
			"Choose the normalization color and click OK.\nNote: This operation cannot be undone!",
			"Choose New Color",
			JOptionPane.PLAIN_MESSAGE,
			null,
			view.colorList.selectedValues,
			view.colorList.selectedValues[0])
		if (normColorStr != null) {
			def normColor = new EntryColor(normColorStr)
			//println "normColor = $normColor"
			def colorsToChange = view.colorList.selectedValues.findAll { it != normColor }
			colorsToChange.each { ctc ->
				def entries = model.schemeEntries.findAll { it.color != null && it.color.equals(ctc.toString()) }
				//println "entries matching $ctc: "
				//entries.each { println "   ${it.name}: ${it.color}" }
				entries.each { it.color = normColor.toString() }
			}
			updateList(model.schemeEntries.findAll { it.color != null }.collect { new EntryColor(it.color as String) })
			select(normColor)
		}
	}
	
	public void valueChanged(ListSelectionEvent e) {
		if (!e.valueIsAdjusting) {
			def sels = view.colorList.selectedIndices
			view.normalizeButton.setEnabled(sels.size() > 1)
		}
	}
}