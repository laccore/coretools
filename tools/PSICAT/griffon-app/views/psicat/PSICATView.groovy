/*
 * Copyright (c) Josh Reed, 2009.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package psicat

import static javax.swing.SwingConstants.*
import static griffon.util.GriffonApplicationUtils.*
import griffon.util.Metadata

import java.awt.Color
import java.awt.Font
import java.awt.event.KeyEvent

import javax.swing.JSplitPane
import javax.swing.KeyStroke
import javax.swing.event.ChangeListener

import ca.odell.glazedlists.swing.EventListModel

import net.miginfocom.swing.MigLayout

import org.andrill.coretools.ui.PropertiesPanel

// build actions
build(PSICATActions)

// build our properties panel
def propertiesPanel = new PropertiesPanel()
def meta = Metadata.current

// build our application
application(title:"PSICAT ${meta['app.version']}", size:[800,600], locationByPlatform: true, layout: new MigLayout('fill'), 
			defaultCloseOperation: 0, windowClosing: { evt -> if (controller.canClose(evt)) app.shutdown() },
			iconImage: imageIcon('/psicat-icon-64.png').image, iconImages: [imageIcon('/psicat-icon-64.png').image,
			imageIcon('/psicat-icon-32.png').image, imageIcon('/psicat-icon-16.png').image]) {

	// menu			
	widget(build(PSICATMenuBar))
	
	// content area
	splitPane(orientation: JSplitPane.HORIZONTAL_SPLIT, dividerLocation: 200, resizeWeight: 0.0, border: emptyBorder(5), constraints:'grow') {
		widget(id:'sidePanel', buildMVCGroup('Project', 'project', openHandler: { evt, mvc -> controller.actions.openSection(evt) }).view.root)
		splitPane(orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: 400, resizeWeight: 0.50, border: emptyBorder(0), constraints: 'grow') {
			tabbedPane(id:'diagrams', constraints: 'grow')
		    scrollPane(constraints: 'grow', border: emptyBorder(0)) {
		    	widget(id:'propertiesPanel', propertiesPanel)
		    }
		}	
	}
	
	// status area
	label(id: 'status', border: emptyBorder([0, 10, 5, 10]), text: bind { model.status }, foreground: Color.darkGray, 
		font: new Font("Sans Serif", 0, 10), constraints: 'dock south')
}

// listen for tab changes
diagrams.addChangeListener({ controller.activeDiagramChanged() } as ChangeListener)