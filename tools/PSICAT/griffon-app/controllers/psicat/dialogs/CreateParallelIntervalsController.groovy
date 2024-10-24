package psicat.dialogs

import javax.swing.JOptionPane

import org.andrill.coretools.model.edit.*
import org.andrill.coretools.geology.models.Length
import org.andrill.coretools.misc.util.StringUtils

import psicat.util.*

class CreateParallelIntervalsController {
    def model
    def view

    void mvcGroupInit(Map args) { }

    public void show() {
        view.createParallelIntervalsDialog.setLocationRelativeTo(app.appFrames[0])
        view.createParallelIntervalsDialog.show()
    }

    def actions = [
		'create': { evt = null ->
			create()
		},
		'close': { evt = null ->
            view.createParallelIntervalsDialog.setVisible(false)
		}
    ]

    def create() {
        Length depth = null
        try {
            depth = new Length(Double.parseDouble(view.depth.text), "cm")
        } catch (NumberFormatException) {
            Dialogs.showErrorDialog("Invalid Depth", "The entered depth '${view.depth.text}' is not a number.", view.root)
            view.depth.requestFocus()
            return
        }

        def selectedModels = view.modelListPanel.getSelectedModels()
        if (selectedModels.size() == 0) {
            Dialogs.showErrorDialog("No Components Selected", "At least one component must be selected.")
            return
        }

        def maxes = [:]
        selectedModels.each { String modelType ->
            def max = new Length("0 cm")
            def typeMax = GeoUtils.getMaxBase(model.diagram.scene.models.findAll { it.modelType.equals(modelType) } )
            maxes[modelType] = typeMax ?: max
        }

        boolean maxesEqual = true
        def vals = maxes.values().toArray()
        for (int i = 0; i < vals.length - 1; i++) {
            if (!vals[i].equals(vals[i+1])) {
                maxesEqual = false
                break
            }
        }
        def maxesStrList = maxes.collect { modelType, max -> "${StringUtils.humanizeModelName(modelType)} ($max)".toString() }.sort()

        if (depth.compareTo(vals[0]) == 0 || depth.compareTo(vals[0]) == -1) {
            final msg = "The entered depth $depth must exceed the current bottommost bases of selected components:\n\n${maxesStrList.join('\n')}\n"
            Dialogs.showMessageDialog("Cannot Create Intervals", msg, view.root)
            return
        }

        if (model.showUnmatchedBottomsWarning && !maxesEqual) {
            final msg = "The current bottommost bases of selected components:\n\n${maxesStrList.join('\n')}\n\nare not equal. Continue?"
            def choice = JOptionPane.showOptionDialog(view.root, msg, "Component Bases Differ", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, ["No", "Yes", "Yes, Stop Asking Me!"] as String[], "No")
            if (choice == 0) { // No
                return
            } else if (choice == 2) { // "Yes, stop asking me!"
                model.showUnmatchedBottomsWarning = false
            }
        }

        def createCommands = []
        view.modelListPanel.selectedClasses.eachWithIndex { c, index ->
            def modelTypeTopDepth = maxes[selectedModels[index]]
            def m = c.newInstance(top:modelTypeTopDepth, base:depth)
            createCommands << new CreateCommand(m, model.diagram.scene.models)
        }

        def command = new CompositeCommand("Create Parallel Intervals", createCommands as Command[])
        model.diagram.commandStack.execute(command)

        model.lastSelectedModels = view.modelListPanel.selectedModels // retain last-selected models
    }
}