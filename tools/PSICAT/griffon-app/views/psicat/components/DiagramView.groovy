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
package psicat.components

import java.awt.geom.Point2D

import org.andrill.coretools.scene.Selection 
import org.andrill.coretools.scene.Scene.ScenePart
import org.andrill.coretools.ui.ScenePanel
import org.andrill.coretools.ui.ScenePanel.Orientation
import org.andrill.coretools.ui.ScenePanel.SelectionProvider 

scrollPane(id:'viewer', columnHeaderView: widget(new ScenePanel(null, ScenePart.HEADER, Orientation.VERTICAL), id:'header'), constraints: 'grow', border: emptyBorder(0)) {
	widget(new ScenePanel(null, ScenePart.CONTENTS, Orientation.VERTICAL), id: 'contents')
}

//tweak styles
viewer.background = panel().background
viewer.verticalScrollBar.addAdjustmentListener(contents)
viewer.horizontalScrollBar.addAdjustmentListener(contents)

// customize our selection handling
contents.selectionProvider = { scene, e ->
	def o = scene.findAt(new Point2D.Double(e.x, e.y), e.target);
	if (o && e.isAltDown() && !scene.selection.isEmpty()) {
		def existing = scene.selection.firstObject
		def props = [:]
		o.properties.each { k,v ->
			if (existing.hasProperty(k) && !existing.getProperty(k)) {
				props[k] = v
			}
		}
		println "Clone mode"
		scene.commandStack.execute(new org.andrill.coretools.geology.GMultiCommand(source:existing, props:props))
		return null
	} else {
		return (o == null) ? Selection.EMPTY : new Selection(o)
	}
} as SelectionProvider
