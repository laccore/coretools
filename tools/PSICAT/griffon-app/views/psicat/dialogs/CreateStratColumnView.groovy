package psicat.dialogs

import java.awt.LayoutManager
import javax.swing.*
import java.awt.event.ActionListener

import net.miginfocom.swing.MigLayout
import org.andrill.coretools.misc.util.StringUtils

import psicat.ui.DefaultModelListPanel

// model list with optional help text for specified model types
class StratModelListPanel extends DefaultModelListPanel {
	public StratModelListPanel(HashMap<String,String> modelsAndHelpText, boolean select, LayoutManager layout) {
		super(layout)
        def sortedModelTypes = modelsAndHelpText.keySet().sort()
		sortedModelTypes.each { modelType ->
			def cb = new JCheckBox(StringUtils.humanizeModelName(modelType), select)
			this.modelMap.put(modelType, cb)
            this.add(cb)
            String helpText = modelsAndHelpText[modelType]
            if (helpText) {
                JLabel ht = new JLabel("$helpText")
                ht.font = ht.font.deriveFont((float)(ht.font.size - 2.0f))
                this.add(ht)
            }
		}
	}
}

actions {
	action(id:'createAction', name:'Create Strat Column', closure:controller.actions.create)
	action(id:'closeAction', name:'Close', closure:controller.actions.close)
    action(id:'chooseLogFile', name:'Select log file...', closure:controller.actions.chooseLogFile)
    action(id:'logSelected', name:'Create a text log of the strat column creation process', closure:controller.actions.logSelected)
}

def modelListPanel = new StratModelListPanel(model.modelsAndHelpText, false, new MigLayout("insets 5, fillx, wrap"))
def modelTypes = model.modelsAndHelpText.keySet() as List
modelListPanel.selectModels(modelTypes.findAll { it != "Image" }, true)

dialog(id:'createStratColumnDialog', title:'Create Strat Column Options', owner:app.appFrames[0], pack:true, modal:true, resizable:true) {
	panel(id:'root', layout: new MigLayout("fill, wrap, insets 10", "", "[]10[grow]10[][][][]")) {
		label('Strat Column Name', constraints:'split 2')
        textField(id:'stratColumnName', text:model.stratColumnName, constraints:'grow')

        widget(id:'modelListPanel', modelListPanel, border:titledBorder('Components to include'), constraints:'grow')

        checkBox(id:'logCheckbox', action:logSelected, selected:false)

        button(id:'logFileButton', action:chooseLogFile, constraints:'split 2', enabled:false)
        label(id:'logFileLabel', text:bind(source:model, sourceProperty:'logFilePath'), enabled:false, constraints:'gapbottom 15')

		String msg1 = "<html>Note: The created strat column is a snapshot of the included sections and their components.<br>"
		String msg2 = "It will not update to reflect future changes to included components.</html>"
        label(msg1+msg2, constraints:'gapbottom 15')

		panel(layout:new MigLayout('fill, insets 0', '[][grow]', ''), constraints:'growx') {
			button(id:'createButton', action:createAction, constraints:'gapright 10px')
			progressBar(id:'progress', minimum:0, maximum:100, stringPainted:true, string:'', constraints:'growx, align right, wrap, gapbottom 0')
			label(" ", id:'progressText', constraints:'skip 1')
		}

        separator(constraints: 'span, growx')
        button(action:closeAction, constraints:'align right')
	}
}
