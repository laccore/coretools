package psicat.dialogs

import net.miginfocom.swing.MigLayout

actions {
	action(id: 'browseAction', name:'...', closure: controller.actions.browse)
	action(id:'okAction', name:'OK', closure:controller.actions.okAction)
	action(id:'cancelAction', name:'Cancel', closure:controller.actions.cancelAction)
}

dialog(id:'openSCMD', title:'Choose Strat Column Metadata File', resizable:true, modal:true, pack:true, owner:app.appFrames[0]) {
	panel(id:'root', layout: new MigLayout('fill, wrap', '', '[][][grow][]')) {
		panel(border: titledBorder('Strat Column Metadata File'), layout: new MigLayout('fill, wrap, insets 5'), constraints:'growx') {
			label("Select a comma-separated values (.csv) file in one of these formats:")
			label("- Section Metadata: section ID, top depth, bottom depth")
			label("- Splice Interval Table")
			label("File:", constraints:"split 3")
			textField(text: bind(source:model, sourceProperty:'metadataPath', mutual:true), constraints:'width min(200px), growx')
			button(action:browseAction)
			label(id:'fileTypeText', text:bind(source:model, sourceProperty:'fileTypeText'), constraints:'hmin 10px')
		}
		
		//progressBar(id:'progress', indeterminate:true)
		label(id:'statusText', text:bind(source:model, sourceProperty:'statusText'))
		
		// table indicating section name mapping
		scrollPane(constraints:'grow') {
			table(id:'sectionMapTable', autoCreateColumnsFromModel:true)
		}
		
		panel(constraints:'growx') {
			button(id:'ok', action:okAction) //, enabled:bind { model.metadataPath } )
			button(id:'cancel', action:cancelAction)
		}
	}
}

controller.show()