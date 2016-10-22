import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;

import groovy.swing.SwingBuilder
import net.miginfocom.swing.MigLayout

action(id:'newColorAction', name:'Choose New Color', closure:controller.newColor)
action(id:'normalizeAction', name:'Normalize Colors', closure:controller.normalizeColors)

panel(id:'colorChooser', layout: new MigLayout('fill, insets 5, wrap'), border:titledBorder('Currently-used colors')) {
	scrollPane(constraints:'grow, wrap, hmin 400') {
		list(id:'colorList', cellRenderer: new ColorListRenderer(), model:bind { model.colorListModel })
	}
	button(action:newColorAction)
	button(id:'normalizeButton', enabled:false, action:normalizeAction)
}

colorList.addListSelectionListener(controller)

class ColorListRenderer extends DefaultListCellRenderer {
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
		def label = super.getListCellRendererComponent(list, value, index, isSelected, hasFocus)
		EntryColor color = (EntryColor)value
		label.setText(value.toString());
		label.setIcon(new ColorIcon(color));
		return label
	}
}

class ColorIcon implements Icon {
	private Color color = null;
	public ColorIcon(Color color) {
		this.color = color;
	}
	public int getIconHeight() { return 25; }
	public int getIconWidth() { return 25; }
	public void paintIcon(Component c, Graphics g, int x, int y) {
		g.setColor(this.color);
		g.fillRect(x, y, x + getIconWidth(), y + getIconHeight());
		g.setColor(Color.BLACK);
		g.drawRect(x, y, x + getIconWidth(), y + getIconHeight() - 1);
	}
}
