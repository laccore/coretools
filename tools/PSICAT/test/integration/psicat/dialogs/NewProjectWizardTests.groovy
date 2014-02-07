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
package psicat.dialogs

import java.io.File

import griffon.util.IGriffonApplication
import griffon.util.GriffonApplicationHelper as GH
import psicat.ProjectHelper

class NewProjectWizardTests extends GroovyTestCase {
	IGriffonApplication app
	def model
	def view
	def controller
	
	void setUp() {
		(model, view, controller) = GH.createMVCGroup(app, 'NewProjectWizard')
	}
	
	void tearDown() {
		GH.destroyMVCGroup(app, 'NewProjectWizard')
	}
	
	void testFileAndNameAndOrigin() {
		// initialize model
		def dir = ProjectHelper.createTempDir()
		model.filePath = dir.absolutePath
		model.name = 'Test Name'
		model.originTop = false
		
		// test creation
		def project = controller.createProject()
		assert project
		assert project.name == 'Test Name'
		assert project.origin == 'base'
		assert !project.containers
	}
	
	void testFile() {
		// initialize model
		def dir = ProjectHelper.createTempDir()
		model.filePath = dir.absolutePath
		
		// test creation
		def project = controller.createProject()
		assert project
		assert project.name == dir.name
		assert project.origin == 'top'
		assert !project.containers
	}
	
	void testError() {
		shouldFail() {
			controller.createProject()
		}
	}
}