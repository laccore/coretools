package psicat.ui

import java.awt.LayoutManager
import javax.swing.*

abstract class DefaultModelListPanel extends JPanel {
	protected HashMap<String, JCheckBox> modelMap

    public DefaultModelListPanel(LayoutManager layout) {
        super(layout)
        modelMap = new HashMap<String, JCheckBox>()
    }

	public List<String> getSelectedModels() {
		def models = []
		this.modelMap.each { modelType, cb ->
			if (cb.isSelected()) { models << modelType }
		}
		return models
	}

	public void selectModels(List<String> modelTypes, boolean select) {
		modelTypes.each { mt ->
			this.modelMap[mt].setSelected(select)
		}
	}

	public void selectAll(boolean selectAll) {
		this.modelMap.each { _, checkbox -> checkbox.setSelected(selectAll) }
	}
}