package psicat.dialogs

import java.awt.*
import javax.swing.*

import groovy.swing.SwingBuilder

import net.miginfocom.swing.MigLayout

import org.andrill.coretools.scene.*
import org.andrill.coretools.misc.util.StringUtils


class TrackElementRenderer implements ListCellRenderer {
	private final model
	private final Font trackNameFont = new Font("SansSerif", Font.BOLD, 14)

	public TrackElementRenderer(model) { this.model = model }
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		return makePanel((Track)value, isSelected)
	}
	
	def makePanel(Track track, boolean isSelected) {
		def selectColor = javax.swing.UIManager.getDefaults().getColor("List.selectionBackground")
		def bgcolor = isSelected ? selectColor : java.awt.Color.WHITE
		def border = BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.BLACK)

		// TODO: handle floats and weirdness like ImageTrack where setting a width doesn't really make sense
		// because width adjusts to maintain the image's aspect ratio.
		def widthConstraint = this.model.scene.getTrackConstraints(track)
		if (widthConstraint.equals("")) { widthConstraint = track.contentSize.width }

		return new SwingBuilder().panel(layout:new MigLayout("fillx, wrap, insets 5", "[grow]", ""), background:bgcolor, border:border) {
			label("${StringUtils.uncamel(track.class.simpleName).replace(' Track', '')}", font:trackNameFont, constraints:'grow, w 200')
			label("Width: $widthConstraint", constraints:'grow')
		}
	}
}

panel(id:'root', layout: new MigLayout('', "[grow,fill][]", "")) {
	panel(id:'trackPanel', constraints:'grow', border: titledBorder("Diagram Columns")) {
		scrollPane() {
			list(id:'trackList',
				model:bind(source:model, sourceProperty:'trackListModel', mutual:true),
				valueChanged: { evt -> model.selectedTrackIndex = evt.source.selectedIndex },
				cellRenderer: new TrackElementRenderer(this.model))
		}
	}
	panel(id:'buttonPanel', layout: new MigLayout('fill, wrap')) {
		button("Column Options...",
			enabled: bind { model.selectedTrackIndex != -1 }, // bind to view.trackList doesn't seem to work
			actionPerformed: { evt -> controller.trackOptions(evt) }, 
			constraints:'grow')
		button('Add Column...', constraints:'grow')
		button('Remove Column', constraints:'grow')
		button('Move Up', constraints:'grow')
		button('Move Down', constraints:'grow')
	}
}