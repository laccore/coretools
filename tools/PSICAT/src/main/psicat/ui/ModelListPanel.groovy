// A panel with a horizontally laid-out series of checkboxes, one for each 
// GeologyModel class String provided in the ModelListPanel.create() method.
// Checkboxes are labeled with humanized model names e.g. "GrainSizeInterval" -> "Grain Size".

package psicat.ui

import java.awt.LayoutManager
import javax.swing.*
import net.miginfocom.swing.MigLayout
import org.andrill.coretools.misc.util.StringUtils


class ModelListPanel extends JPanel {
	private HashMap<String, JCheckBox> modelMap

	static ModelListPanel create(List<String> models, boolean check, LayoutManager layout=null) {
		if (!layout) { layout = new MigLayout("fillx, insets 5")}
		ModelListPanel panel = new ModelListPanel(models.sort(), check, layout)
		return panel
	}

	private ModelListPanel(List<String> models, boolean check, LayoutManager layout) {
		super(layout)
		modelMap = new HashMap<String, JCheckBox>()
		models.each { modelType ->
			def cb = new JCheckBox(StringUtils.humanizeModelName(modelType), check)
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

	public checkModelTypes(List<String> modelTypes, boolean check) {
		modelTypes.each { mt ->
			this.modelMap[mt].setSelected(check)
		}
	}
}