package psicat.dialogs

import java.awt.LayoutManager
import javax.swing.*
import java.awt.event.ActionListener

import net.miginfocom.swing.MigLayout
import org.andrill.coretools.misc.util.StringUtils

import psicat.ui.DefaultModelListPanel
import psicat.util.*


class CreateClassListPanel extends DefaultModelListPanel {
    def modelTypeToClassMap = [:]
    public CreateClassListPanel(List<Class> modelClasses, boolean select, LayoutManager layout) {
        super(layout)
        modelClasses.each { c ->
            def mt = c.getSimpleName()
            modelTypeToClassMap[mt] = c
        }
        (modelTypeToClassMap.keySet() as List).sort().each { mt ->
            def cb = new JCheckBox(StringUtils.humanizeModelName(mt), select)
            this.modelMap[mt] = cb
            this.add(cb)
        }
    }

    // Return list of selected Classes parallel to list of selected model type Strings returned in getSelectedModels()
    // i.e. it is safe to assume Class at index N in returned list corresponds to modelType String at index N
    // in list returned from getSelectedModels().
    public List<Class> getSelectedClasses() {
		def classes = []
		this.modelMap.each { modelType, cb ->
			if (cb.isSelected()) { classes << modelTypeToClassMap[modelType] }
		}
		return classes
    }
}

actions {
    action(id:'createAction', name:'Create Intervals', closure:controller.actions.create)
    action(id:'closeAction', name:'Close', closure:controller.actions.close)
}

def modelListPanel = new CreateClassListPanel(model.modelClasses, false, new MigLayout("insets 10, fill, wrap"))
if (model.lastSelectedModels) { // restore last-selected models if any
    modelListPanel.selectModels(model.lastSelectedModels, true)
}

dialog(id:'createParallelIntervalsDialog', title:'Create Parallel Intervals', owner:app.appFrames[0], pack:true, modal:true, resizable:false) {
	panel(id:'root', layout: new MigLayout("fill, wrap, insets 5", "", "[][]15[]")) {
        label("Fill to bottom depth (cm):", constraints:'split 2')
        textField(id:'depth', constraints:'wmin 100, grow')

        widget(id:'modelListPanel', modelListPanel, border:titledBorder('Components to create'), constraints:'grow')

        button(action:closeAction, constraints:'split 2, tag cancel')
        button(id:'createButton', action:createAction, constraints:'tag ok')
	}
}

createParallelIntervalsDialog.getRootPane().setDefaultButton(createButton)
