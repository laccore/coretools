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

import griffon.util.IGriffonApplication
import griffon.util.GriffonApplicationHelper as GH
import psicat.ProjectHelper
	
class ImportTabularWizardTests extends GroovyTestCase {
	IGriffonApplication app
	def project
	def model
	def view
	def controller
	
	void setUp() {
		project = ProjectHelper.createProject('Test Project', 'top')
		project.createContainer('Section 1')
		(model, view, controller) = GH.createMVCGroup(app, 'ImportTabularWizard', 'ImportTabularWizard', [project: project])
	}
	
	void tearDown() {
		GH.destroyMVCGroup(app, 'ImportTabularWizard')
	}
	
	void testImport() {
		model.filePath = getClass().getResource("tests.xls").toExternalForm().replace("file:", "")
		controller.importTabular('Section 1')
		
		def container = project.openContainer('Section 1')
		assert container
		assert container.models.size() == 1
		
		def image = container.models[0]
		assert image
		assert image.modelType == 'Image'
		assert image.top.value == 0
		assert image.base.value == 1
		assert image.group == 'split'
		assert image.path.toExternalForm() == 'file:test.jpeg'
	}
	
	void testError() {
		shouldFail() {
			controller.importTabular('Section 1')
		}
	}
}