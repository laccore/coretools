package psicat.dialogs

import javax.swing.*

import org.andrill.coretools.scene.*
import org.andrill.coretools.misc.util.StringUtils

import psicat.util.Dialogs


class TrackOptionsController {
	def model
	def view

	void mvcGroupInit(Map args) {
		model.track = args.track
		model.parameterValues = new HashMap<TrackParameter, JComponent>()
	}

	def show() {
		model.track.trackParameters.each {
			def value = model.track.hasParameter(it.key) ? model.track.getParameter(it.key, "") : it.defaultValue
			def comp = view.addComponent(view.root, it.type, it.label, it.description, value)
			model.parameterValues[it] = comp
		}
		def result = Dialogs.showCustomDialog("${StringUtils.uncamel(model.track.class.simpleName).replace('Track', 'Column')} Options", view.root, app.appFrames[0], false)
		if (result) {
			getParameterValues()
		}
		return result
	}

	def getParameterValues() {
		def vals = [:]
		model.parameterValues.each { param, component ->
			vals[param.key] = getComponentValue(param.type, component)
		}
		return vals
	}

	private String getComponentValue(int parameterType, JComponent component) {
		if (parameterType == TrackParameter.Type.BOOLEAN) {
			def comp = (JCheckBox)component
			return comp.isSelected() as String
		} else { // TrackParameter.Type.INTEGER, FLOAT, or STRING
			def comp = (JTextField)component
			return comp.getText()
		}
	}
}