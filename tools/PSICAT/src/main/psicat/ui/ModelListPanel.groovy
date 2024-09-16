// A panel with a horizontally laid-out series of checkboxes, one for each 
// GeologyModel class String provided in the ModelListPanel.create() method.
// Checkboxes are labeled with humanized class names e.g. "GrainSizeInterval" -> "Grain Size".

package psicat.ui

import javax.swing.*
import net.miginfocom.swing.MigLayout
import org.andrill.coretools.misc.util.StringUtils


class ModelListPanel extends JPanel {
	private HashMap<String, JCheckBox> modelMap

	static ModelListPanel create(List<String> models) {
		ModelListPanel panel = new ModelListPanel(models.sort())
		return panel
	}

	private ModelListPanel(List<String> models) {
		super(new MigLayout("fillx, insets 5"))
		modelMap = new HashMap<String, JCheckBox>()
		models.each { modelType ->
			def cb = new JCheckBox(StringUtils.uncamel(modelType).replace(" Interval", ""))
			this.add(cb)
			this.modelMap.put(modelType, cb)
		}
	}

	public List<String> getSelectedModels() {
		def models = []
		this.modelMap.each { modelType, cb ->
			if (cb.isSelected()) { models << modelType }
		}
		return models
	}
}