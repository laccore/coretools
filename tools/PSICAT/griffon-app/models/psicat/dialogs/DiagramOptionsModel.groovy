package psicat.dialogs

import org.andrill.coretools.scene.*

import javax.swing.*
import groovy.beans.Bindable

class DiagramOptionsModel {
	@Bindable scene
	@Bindable trackListModel = new DefaultListModel<Track>()
	@Bindable int selectedTrackIndex = -1
	@Bindable String diagramTypeText
	boolean sceneDirty
}