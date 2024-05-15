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
		def result = Dialogs.showCustomDialog("${StringUtils.uncamel(model.track.class.simpleName)} Options", view.root, app.appFrames[0])
		if (result) {
			getParameterValues()
		}
		return result
	}

	def getParameterValues() {
	  def vals = [:]  
	  model.parameterValues.each { param, component ->
		println "Parameter ${param.key} UI component value = ${getComponentValue(param.type, component)}"
		vals[param.key] = getComponentValue(param.type, component)
	  }
	  return vals
	}

	private String getComponentValue(int parameterType, JComponent component) {
		if (parameterType == TrackParameter.Type.BOOLEAN) {
			def comp = (JCheckBox)component
			return comp.isSelected() as String
		} else if (parameterType == TrackParameter.Type.INTEGER || parameterType == TrackParameter.Type.FLOAT) {
			def comp = (JTextField)component
			return comp.getText()
		}
	}
}