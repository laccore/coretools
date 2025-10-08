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
import java.awt.event.KeyEvent

import org.andrill.coretools.scene.Selection 
import org.andrill.coretools.scene.Scene
import org.andrill.coretools.scene.Scene.ScenePart
import org.andrill.coretools.ui.ScenePanel
import org.andrill.coretools.ui.ScenePanel.Orientation
import org.andrill.coretools.ui.ScenePanel.SelectionProvider
import org.andrill.coretools.ui.ScenePanel.KeySelectionProvider

import org.andrill.coretools.model.edit.CreateCommand
import org.andrill.coretools.geology.models.GeologyModel
import org.andrill.coretools.geology.models.Occurrence
import org.andrill.coretools.geology.models.csdf.*

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

	// clone selected Feature if Alt key is down
	final cloneables = [Feature, Occurrence, Caementa, Mortar, Matrix, Pores, Fractures]
	if (o && o.class in cloneables && e.isAltDown() && !scene.selection.isEmpty()) {
		def existing = scene.selection.firstObject
		def props = [:]
		println "Cloning Feature $o"
		def m = o.class.newInstance(top:o.top, base:o.base, scheme:o.scheme)
		scene.commandStack.execute(new CreateCommand(m, existing.container))
		return null
	} else {
		println "Selected object $o"
		return (o == null) ? Selection.EMPTY : new Selection(o)
	}
} as SelectionProvider

// customize keyboard-based selection
// Shift+UP selects next object of same type above
// Shift+DOWN selects next object of same type below
contents.keySelectionProvider = { scene, e ->
	def newSelection = null
	if (e.getKeyCode() == KeyEvent.VK_DOWN && e.isShiftDown()) {
		newSelection = getNextObject(scene)
	} else if (e.getKeyCode() == KeyEvent.VK_UP && e.isShiftDown()) {
		newSelection = getNextObject(scene, false)
	}
	return newSelection
} as KeySelectionProvider

// In the specified Scene, if an Object is selected, find the 'next' Object of
// the same type in the specified direction and return it as a Selection.
def getNextObject(Scene s, boolean lower=true) {
	def cur = s.selection.firstObject
	def sel = null
	if (cur) {
		def mods = cur.container.models.collect { it } // container.models is immutable but we need to sort
		mods.sort { it.top.to('m').value }
		if (lower) {
			// all non-cur objects of cur's type with top depth >= cur.top or greater index
			// in mods if top depth == cur.top
			def siblings = mods.findAll { it != cur && it.modelType == cur.modelType && 
				(it.top.compareTo(cur.top) > 0 ||
				(it.top.compareTo(cur.top) == 0 && mods.indexOf(it) > mods.indexOf(cur))) }
			sel = siblings.min { Math.abs(cur.top.minus(it.top).to('m').value) }
		} else {
			// all non-cur objects of cur's type with top depth <= cur.top or lesser index
			// in mods if top depth == cur.top
			def siblings = mods.findAll { it != cur && it.modelType == cur.modelType && 
				(it.top.compareTo(cur.top) < 0 || (it.top.compareTo(cur.top) == 0 &&
				mods.indexOf(it) < mods.indexOf(cur))) }
			// Want selection order to be opposite of 'lower' block. Given [a,b,c] where a == b == c,
			// min() returns a because it's first in the list, but we want c first, thus reverse().
			sel = siblings.reverse().min { Math.abs(cur.top.minus(it.top).to('m').value) }
		}
	}
	return sel == null ? s.selection : new Selection(sel)
}