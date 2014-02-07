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

class NewSectionWizardTests extends GroovyTestCase {
	IGriffonApplication app
	def project
	def model
	def view
	def controller
	
	void setUp() {
		project = ProjectHelper.createProject('Test Project', 'top')
		(model, view, controller) = GH.createMVCGroup(app, 'NewSectionWizard', 'NewSectionWizard', [project: project])
	}
	
	void tearDown() {
		GH.destroyMVCGroup(app, 'NewSectionWizard')
	}
	
	void testName() {
		model.name = 'Section 1'
		
		def section = controller.createSection()
		assert section
		assert section == 'Section 1'
		assert 'Section 1' in project.containers
		
		def container = project.openContainer(section)
		assert container
		assert !container.models
	}
	
	void testNameAndTop() {
		model.name = 'Section 1'
		model.top = "3.0"
		
		def section = controller.createSection()
		assert section
		assert section == 'Section 1'
		assert 'Section 1' in project.containers
		
		def container = project.openContainer(section)
		assert container
		assert container.models.size() == 1
		
		def model = container.models[0]
		assert model
		assert model.modelType == 'Section'
		assert model.top.value == 3
		assert model.base.value == 3
	}
	
	void testNameAndTopAndBase() {
		model.name = 'Section 1'
		model.top = "3.0"
		model.base = "4.0"
		
		def section = controller.createSection()
		assert section
		assert section == 'Section 1'
		assert 'Section 1' in project.containers
		
		def container = project.openContainer(section)
		assert container
		assert container.models.size() == 1
		
		def model = container.models[0]
		assert model
		assert model.modelType == 'Section'
		assert model.top.value == 3
		assert model.base.value == 4
	}
	
	void testNameAndTopAndBaseAndImage() {
		model.name = 'Section 1'
		model.top = "3.0"
		model.base = "4.0"
		model.filePath = new File(project.directory, 'section-image.jpeg').absolutePath
		
		def section = controller.createSection()
		assert section
		assert section == 'Section 1'
		assert 'Section 1' in project.containers
		
		def container = project.openContainer(section)
		assert container
		assert container.models.size() == 2
		
		def model = container.models[0]
		assert model
		assert model.modelType == 'Section'
		assert model.top.value == 3
		assert model.base.value == 4
		
		def image = container.models[1]
		assert image
		assert image.modelType == 'Image'
		assert image.top.value == 3
		assert image.base.value == 4
		assert image.path == new File(project.directory, 'section-image.jpeg').toURI().toURL()
	}
	
	void testError() {
		shouldFail {
			controller.createSection()
		}
	}
}