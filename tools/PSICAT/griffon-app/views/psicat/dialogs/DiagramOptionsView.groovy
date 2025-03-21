package psicat.dialogs

import java.awt.*
import javax.swing.*

import groovy.swing.SwingBuilder

import net.miginfocom.swing.MigLayout

import org.andrill.coretools.scene.*
import org.andrill.coretools.misc.util.StringUtils
import org.andrill.coretools.geology.ui.ImageTrack
import org.andrill.coretools.geology.ui.RulerTrack


class TrackElementRenderer implements ListCellRenderer {
	private final model
	private final Font trackNameFont = new Font("SansSerif", Font.BOLD, 14)

	// Because we're stuck with ancient Groovy 1.6.4 (Griffon 0.2), we can't just reverse
	// TRACK_TO_CLASS with collectEntries() here.
	static public final HashMap<String, String> CLASS_TO_TRACK = [
		"org.andrill.coretools.geology.ui.AnnotationTrack" : "Annotation",
		"org.andrill.coretools.geology.ui.csdf.BeddingTrack" : "Bedding",
		"org.andrill.coretools.geology.ui.csdf.FeatureTrack" : "Feature",
		"org.andrill.coretools.geology.ui.csdf.GrainSizeTrack" : "Grain Size",
		"org.andrill.coretools.geology.ui.ImageTrack" : "Image",
		"org.andrill.coretools.geology.ui.csdf.LegendTrack" : "Legend",
		"org.andrill.coretools.geology.ui.csdf.LithologyTrack" : "Lithology (CSD Facility)",
		"org.andrill.coretools.geology.ui.IntervalTrack" : "Lithology (Andrill)",
		"org.andrill.coretools.geology.ui.csdf.SectionNameTrack" : "Section Name",
		"org.andrill.coretools.geology.ui.OccurrenceTrack" : "Symbol (Andrill)",
		"org.andrill.coretools.geology.ui.RulerTrack" : "Ruler",
		"org.andrill.coretools.geology.ui.csdf.TextureTrack" : "Texture",
		"org.andrill.coretools.geology.ui.csdf.UnitTrack" : "Unit (CSD Facility)",
		"org.andrill.coretools.geology.ui.UnitTrack" : "Unit (Andrill)" 
	]

	public TrackElementRenderer(model) { this.model = model }
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		return makePanel((Track)value, isSelected)
	}
	
	def makePanel(Track track, boolean isSelected) {
		def selectColor = javax.swing.UIManager.getDefaults().getColor("List.selectionBackground")
		def bgcolor = isSelected ? selectColor : java.awt.Color.WHITE
		def border = BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.BLACK)

		def widthConstraint = this.model.scene.getTrackConstraints(track)
		if (widthConstraint.equals("")) {
			widthConstraint = "${track.contentSize.width} (default)"
		}
		if (track instanceof ImageTrack) {
			widthConstraint = "Varies to maintain image aspect ratio"
		}

		return new SwingBuilder().panel(layout:new MigLayout("fillx, wrap, insets 5", "[grow]", ""), background:bgcolor, border:border) {
			def trackTypeLabel = track instanceof RulerTrack ? "Ruler" : track.header
			label("$trackTypeLabel", font:trackNameFont, constraints:'grow, w 200')
			label("Width: $widthConstraint", constraints:'grow')
			label("Column Type: ${CLASS_TO_TRACK[track.class.name]}", constraints: 'grow')
		}
	}
}

panel(id:'root', layout: new MigLayout('', "[grow,fill][]", "")) {
	label("${model.diagramTypeText}", id:'diagramTypeLabel', constraints:'grow, span 2, wrap, gapbottom 15')
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
		button("Edit Column Width...",
		 	// prevent editing of ImageTrack width; colleague insists no one will ever want stretched imagery
			enabled: bind { model.selectedTrackIndex != -1 && !(trackList.selectedValue instanceof ImageTrack) },
			actionPerformed: { evt -> controller.editColumnWidth(evt) },
			constraints:'grow')
		button('Add Column...',
			actionPerformed: { evt -> controller.addColumn(evt) },
			constraints:'grow')
		button('Remove Column',
			enabled: bind { model.selectedTrackIndex != -1},
			actionPerformed: { evt -> controller.removeColumn(evt) },
			constraints:'grow')
		button('Move Up',
			enabled: bind { model.selectedTrackIndex != -1},
			actionPerformed: { evt -> controller.moveColumnUp(evt) },
			constraints:'grow')
		button('Move Down',
			enabled: bind { model.selectedTrackIndex != -1},
			actionPerformed: { evt -> controller.moveColumnDown(evt) },
			constraints:'grow')
	}
}

String promptForTrack(trackTypes) {
	def result = JOptionPane.showInputDialog(
		view.root,
		"Select a column to add:",
		"Add Column",
		JOptionPane.PLAIN_MESSAGE,
		null,
		trackTypes as String[],
		trackTypes[0])
	return result
}

String promptForWidth(String initWidth) {
	def newWidth = JOptionPane.showInputDialog(
		view.root,
		"Enter a new width.\nEnter an asterisk (*) to use available space.\nLeave blank to use the column's default width.",
		initWidth
	)
	return newWidth
}