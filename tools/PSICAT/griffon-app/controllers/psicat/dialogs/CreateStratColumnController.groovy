package psicat.dialogs

import org.apache.log4j.Logger
import org.apache.log4j.FileAppender
import org.apache.log4j.SimpleLayout

import psicat.util.*

class CreateStratColumnController {
    def model
    def view

    private static logger = Logger.getLogger(CreateStratColumnController.class)

    void mvcGroupInit(Map args) {}
    public void show() {
        view.createStratColumnDialog.setLocationRelativeTo(app.appFrames[0])
        view.createStratColumnDialog.show()
    }

    def actions = [
		'create': { evt = null ->
			create()
		},
        'chooseLogFile': { evt = null ->
            chooseLogFile()
        },
        'logSelected': { evt = null ->
            logSelected()
        },
		'close': { evt = null ->
            view.createStratColumnDialog.setVisible(false)
		}
    ]

    private errbox(title, msg) { Dialogs.showErrorDialog(title, msg, view.root) } // view.root as parent to ensure visibility

    def create() {
        boolean success = false

        final String sectionName = view.stratColumnName.text
        if (sectionName.length() == 0) {
            errbox('Name Required', 'A strat column name is required.')
            return
        }
        if (model.project.containers.contains(sectionName)) {
            errbox('Duplicate Name', "A section named $sectionName already exists, enter a unique strat column name.")
            return
        }
        if (view.modelListPanel.getSelectedModels().size() == 0) {
            errbox('No Components Selected', 'At least one component type must be selected.')
            return
        }

        doOutside {
            try {
                view.progress.indeterminate = true
                view.progressText.text = 'Creating strat column...'

                final boolean createLogFile = view.logCheckbox.isSelected()
                if (createLogFile) { // withLog() method?
                    def appender = new FileAppender(new SimpleLayout(), model.logFilePath, false)
                    logger.addAppender(appender)
                    logger.setAdditivity(false)
                    model.stratMetadata.setLogger(logger)
                    GeoUtils.setLogger(logger)
                }

                def containers = model.stratMetadata.getContainers(model.project, view.modelListPanel.getSelectedModels())

                if (createLogFile) {
                    logger.removeAllAppenders()
                    model.stratMetadata.setLogger(null)
                    GeoUtils.setLogger(null)
                }
                
                def stratColumnContainer = null
                edt { stratColumnContainer = model.project.createContainer(sectionName) }

                containers.each { sectionNameKey, c -> stratColumnContainer.addAll(c.models) }

                edt {
                    if (stratColumnContainer.models.size() > 0) {
                        model.project.saveContainer(stratColumnContainer)
                        success = true
                    } else {
                        errbox('No Data', 'No data was found in the specified metadata depth intervals.')
                    }
                }

                view.progressText.text = "Strat column creation ${success ? 'succeeded' : 'failed'}."
            } catch (Exception e) {
                errbox('Error', e.message)
                view.progressText.text = 'Strat column creation failed.'
            }
            view.progress.indeterminate = false
        }
    }

	private boolean chooseLogFile() {
        boolean fileSelected = false
		File selectedLogFile = Dialogs.showSaveDialog('Select a Log File', null, null, view.root)
		if (selectedLogFile) {
			model.logFilePath = selectedLogFile.getAbsolutePath()
            fileSelected = true
		}
        return fileSelected
	}

	private void logSelected() {
		final boolean enable = view.logCheckbox.isSelected()
        if (enable && model.logFilePath.equals(model.NULL_LOG_FILE)) {
            // ensure a file was selected, otherwise uncheck logCheckbox
            final fileSelected = chooseLogFile()
            if (!fileSelected) {
                view.logCheckbox.selected = false
                return
            }
        }
		view.logFileLabel.setEnabled(enable)
		view.logFileButton.setEnabled(enable)
	}
}