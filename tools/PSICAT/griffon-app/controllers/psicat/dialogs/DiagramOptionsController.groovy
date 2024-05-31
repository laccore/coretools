package psicat.dialogs

import org.andrill.coretools.scene.*

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