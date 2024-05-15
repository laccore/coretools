package psicat.dialogs

import javax.swing.*

import groovy.beans.Bindable

import org.andrill.coretools.scene.*

class TrackOptionsModel {
	Track track
	List<TrackParameter> trackParameters
	HashMap<TrackParameter, JComponent> parameterValues
}