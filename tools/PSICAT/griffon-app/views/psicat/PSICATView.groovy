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

import java.awt.Color
import java.awt.Font
import java.awt.event.*

import javax.swing.*
import javax.swing.event.ChangeListener

import java.util.prefs.Preferences

import groovy.swing.SwingBuilder

import ca.odell.glazedlists.swing.EventListModel

import net.miginfocom.swing.MigLayout

import org.andrill.coretools.ui.PropertiesPanel
import org.andrill.coretools.misc.util.StringUtils

import psicat.ui.*
import psicat.util.Dialogs

// build actions
build(PSICATActions)

// build our properties panel
def propertiesPanel = new PropertiesPanel()

// restore previous session's PSICAT window dimensions
def prefs = Preferences.userNodeForPackage(PSICATController)
def mainWidth = prefs.getDouble('psicat.mainViewWidth', 800.0) as Integer
def mainHeight = prefs.getDouble('psicat.mainViewHeight', 600.0) as Integer
def xpos = prefs.getDouble('psicat.mainViewX', 50.0) as Integer
def ypos = prefs.getDouble('psicat.mainViewY', 50.0) as Integer

def subversion = app.applicationProperties['app.subversion'] ?: ""

// build our application
application(title:"PSICAT ${app.applicationProperties['app.version']} $subversion", id:'mainView', size:[mainWidth,mainHeight],
			location:[xpos,ypos],
			layout: new MigLayout('fill'), 
			defaultCloseOperation: 0,
			windowClosing: { evt -> if (controller.canClose(evt)) app.shutdown() },
			iconImage: imageIcon('/psicat-icon-64.png').image, iconImages: [imageIcon('/psicat-icon-64.png').image,
			imageIcon('/psicat-icon-32.png').image, imageIcon('/psicat-icon-16.png').image]) {

	// menu			
	menuBar(build(PSICATMenuBar))
	
	// content area
	splitPane(orientation: JSplitPane.HORIZONTAL_SPLIT, dividerLocation: 200, resizeWeight: 0.0, border: emptyBorder(5), constraints:'grow') {
		widget(id:'sidePanel', buildMVCGroup('Project', 'project', openHandler: { evt, mvc -> controller.actions.openSection(evt) }).view.root)
		splitPane(orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: mainHeight * 0.65, resizeWeight: 0.65, border: emptyBorder(0), constraints: 'grow') {
			tabbedPane(id:'diagrams', constraints: 'grow')
			scrollPane(constraints: 'grow', border: emptyBorder(0)) {
				panel(layout: new MigLayout("fill, wrap, insets 0", "", "[][grow]")) {
					button(id: 'createIntervals', text: 'Create Parallel Intervals...',
						actionPerformed: { evt -> app.controllers['PSICAT'].actions.createIntervals(evt) },
						enabled: bind { model.activeDiagram != null })
					widget(id:'propertiesPanel', propertiesPanel, constraints:'grow')
				}
			}
		}
	}
	
	// status area
	label(id: 'status', border: emptyBorder([0, 10, 5, 10]), text: bind { model.status }, foreground: Color.darkGray, 
		font: new Font("Sans Serif", 0, 10), constraints: 'dock south, hmin 20')
}

// listen for tab changes
diagrams.addChangeListener({ controller.activeDiagramChanged() } as ChangeListener)

class ProgressBarFactory {
	static create(String msg) {
		def progressBar = new SwingBuilder().frame(id:'progressFrame', layout:new MigLayout('fillx, wrap 1'), undecorated:true, alwaysOnTop:true) {
			label(msg)
			progressBar(id:'progressBar', indeterminate:true, constraints:'grow')
		}
		progressBar.pack()
		return progressBar
	}
}

