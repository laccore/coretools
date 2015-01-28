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
package psicat.util

import javax.swing.JFileChooser
import javax.swing.JOptionPane

class Dialogs {
	// last open/save dirs are loaded from prefs in Initialize.groovy
	static File currentOpenDir = new File(System.getProperty("user.home"))
	static File currentSaveDir = new File(System.getProperty("user.home"))
	
	static void showErrorDialog(title, message, parent = null) { 
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE) 
	}
	
	static File showSaveDialog(title, filter = null, defaultExtension = null, parent = null) {
		def fc = new JFileChooser(currentSaveDir)
		fc.fileSelectionMode = JFileChooser.FILES_ONLY
		if (title) { fc.dialogTitle = title }
		if (filter) { fc.addChoosableFileFilter(filter) }
		if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			currentSaveDir = fc.currentDirectory
			def file = fc.selectedFile
			if (defaultExtension && !file.name.contains('.')) {
				file = new File(file.parent, file.name + defaultExtension)
			}
			return file
		} else {
			return null
		}
	}

	static File showSaveDirectoryDialog(title, filter = null, parent = null) {
		def fc = new JFileChooser(currentSaveDir)
		fc.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
		if (title) { fc.dialogTitle = title }
		if (filter) { fc.addChoosableFileFilter(filter) }
		if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			def file = fc.selectedFile
			if (file.exists() && file.isDirectory()) {
				currentSaveDir = file
			} else {
				currentSaveDir = fc.currentDirectory
			}
			return fc.selectedFile
		} else {
			return null
		}
	}
	
	static File showOpenDialog(title, filter = null, parent = null) {
		def fc = new JFileChooser(currentOpenDir)
		fc.fileSelectionMode = JFileChooser.FILES_ONLY
		if (title) { fc.dialogTitle = title }
		if (filter) { fc.addChoosableFileFilter(filter) }
		if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			currentOpenDir = fc.currentDirectory
			return fc.selectedFile
		} else {
			return null
		}
	}
	
	static File[] showOpenMultipleDialog(title, filter = null, parent = null) {
		def fc = new JFileChooser(currentOpenDir)
		fc.fileSelectionMode = JFileChooser.FILES_ONLY
		fc.multiSelectionEnabled = true
		if (title) { fc.dialogTitle = title }
		if (filter) { fc.addChoosableFileFilter(filter) }
		if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			currentOpenDir = fc.currentDirectory
			return fc.selectedFiles
		} else {
			return null
		}
	}
	
	static File showOpenDirectoryDialog(title, filter = null, parent = null) {
		def fc = new JFileChooser(currentOpenDir)
		fc.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
		if (title) { fc.dialogTitle = title }
		if (filter) { fc.addChoosableFileFilter(filter) }
		if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			currentOpenDir = fc.currentDirectory
			return fc.selectedFile
		} else {
			return null
		}
	}
	
	static def showInputDialog(title, message, parent = null) {
		JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE)
	}
	
	static void showMessageDialog(title, message, parent = null) {
		JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE)
	}
	
	static boolean showCustomDialog(title, message, parent = null) {
		JOptionPane.showOptionDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null) == JOptionPane.OK_OPTION
	}
}