package psicat.dialogs

import org.andrill.coretools.scene.*
import org.andrill.coretools.misc.util.SceneUtils
import org.andrill.coretools.geology.ui.*
import org.andrill.coretools.geology.ui.csdf.*

import psicat.util.Dialogs


class DiagramOptionsController {
	def model
	def view

	void mvcGroupInit(Map args) {
		model.scene = args.scene
	}

	def show() {
		model.scene.tracks.each { model.trackListModel.addElement(it) }
		def result = Dialogs.showCustomDialog("Diagram Options", view.root, app.appFrames[0], false)
		return result
	}

	private setTrackParams(scene, trackClass, paramValues) {
		def t = scene.getTrack(trackClass)
		paramValues.each { name, value ->
			t.setParameter(name, value)
		}
	}

	def trackOptions = { evt = null ->
		app.controllers['PSICAT'].withMVC('TrackOptions', track: view.trackList.selectedValue) { mvc ->
			if (mvc.controller.show()) {
				def paramValues = mvc.controller.getParameterValues()

				 // redraw all open diagrams
				app.models['PSICAT'].openDiagrams.each { diagram ->
					setTrackParams(diagram.model.scene, mvc.model.track.class, paramValues)
					diagram.model.scene.invalidate()
				}

				// update project scene
				setTrackParams(model.scene, mvc.model.track.class, paramValues)
			}
		}
	}

	static public final HashMap<String, String> TRACK_TO_CLASS = [
		"Annotation" : "org.andrill.coretools.geology.ui.AnnotationTrack.class",
		"Bedding" : "org.andrill.coretools.geology.ui.csdf.BeddingTrack",
		"Feature" : "org.andrill.coretools.geology.ui.csdf.FeatureTrack",
		"Grain Size" : "org.andrill.coretools.geology.ui.csdf.GrainSizeTrack",
		"Image" : "org.andrill.coretools.geology.ui.ImageTrack",
		"Legend" : "org.andrill.coretools.geology.ui.csdf.LegendTrack",
		"Lithology (CSD Facility)" : "org.andrill.coretools.geology.ui.csdf.LithologyTrack",
		"Lithology (Andrill)" : "org.andrill.coretools.geology.ui.LithologyTrack",
		"Section Name" : "org.andrill.coretools.geology.ui.csdf.SectionNameTrack",
		"Symbol (Andrill)" : "org.andrill.coretools.geology.ui.OccurrenceTrack",
		"Ruler" : "org.andrill.coretools.geology.ui.RulerTrack",
		"Texture" : "org.andrill.coretools.geology.ui.csdf.TextureTrack",
		"Unit" : "org.andrill.coretools.geology.ui.csdf.UnitTrack"
	]

	def editColumnWidth = { evt = null ->
		def constraints = this.model.scene.getTrackConstraints(view.trackList.selectedValue)
		def newWidth = view.promptForWidth(constraints)
		if (newWidth != null) {
			this.model.scene.setTrackConstraints(view.trackList.selectedValue, newWidth)
		}
	}

	def addColumn = { evt = null ->
		def trackName = view.promptForTrack(TRACK_TO_CLASS.keySet().toArray())
		if (trackName) {
			def t = SceneUtils.createTrack(TRACK_TO_CLASS[trackName])
			model.trackListModel.addElement(t)
			model.scene.addTrack(t, "72")
			model.scene.invalidate()
		}
	}

	def removeColumn = { evt = null ->
		model.scene.removeTrack(view.trackList.selectedValue)
		final idx = view.trackList.selectedIndex
		model.trackListModel.removeElementAt(view.trackList.selectedIndex)
		if (model.trackListModel.size() > 0) {
			idx < model.trackListModel.size() - 1 ? view.trackList.setSelectedIndex(idx) : view.trackList.setSelectedIndex(idx-1)
		}
		model.scene.invalidate()
	}

	def moveColumnUp = { evt = null ->
		final idx = view.trackList.selectedIndex
		if (idx > 0) {
			def t = view.trackList.selectedValue
			model.scene.moveTrack(t, idx-1)
			model.trackListModel.removeElementAt(view.trackList.selectedIndex)
			model.trackListModel.insertElementAt(t, idx-1)
			view.trackList.setSelectedIndex(idx-1)
			model.scene.invalidate()
		}
	}

	def moveColumnDown = { evt = null ->
		final idx = view.trackList.selectedIndex
		if (idx < model.trackListModel.size() - 1) {
			def t = view.trackList.selectedValue
			model.scene.moveTrack(t, idx+1)
			model.trackListModel.removeElementAt(view.trackList.selectedIndex)
			model.trackListModel.insertElementAt(t, idx+1)
			view.trackList.setSelectedIndex(idx+1)
			model.scene.invalidate()
		}
	}
}