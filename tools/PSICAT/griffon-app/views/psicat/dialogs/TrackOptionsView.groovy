package psicat.dialogs

import javax.swing.*

import net.miginfocom.swing.MigLayout

import org.andrill.coretools.scene.TrackParameter

actions { }

panel(id:'root', layout: new MigLayout('fill, wrap'), border: etchedBorder()) {}

def addComponent(JComponent parent, int type, String label, String description, Object value) {
	def comp = null
	if (type == TrackParameter.Type.BOOLEAN) { // checkbox for boolean
		comp = new JCheckBox(label)
		comp.setSelected(Boolean.parseBoolean(value))
	} else { // edit field for integer, float, or String
		comp = new JTextField(value as String)
		parent.add(new JLabel(label), "split 2")
	}
	parent.add(comp, "grow")
	if (description) {
		def desc = new JLabel(description)
		desc.setFont(desc.font.deriveFont((float)(desc.font.size - 2.0f)))
		parent.add(desc, "gapbottom 10")
	}
	return comp
}
