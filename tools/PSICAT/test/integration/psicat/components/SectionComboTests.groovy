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
package psicat.components;

import griffon.util.IGriffonApplication
import griffon.util.GriffonApplicationHelper as GH

import org.andrill.coretools.Platform
import org.andrill.coretools.model.ModelManager

import psicat.ProjectHelper

class SectionComboTests extends GroovyTestCase {
	IGriffonApplication app
	def project
	def model
	def view
	def controller
	
	void setUp() {
		def factory = Platform.getService(ModelManager.class)
		
		project = ProjectHelper.createProject('Test Project', 'top')
		def sec1 = project.createContainer('Section 1')
		sec1.add(factory.build('Section', [top: '0 m', base: '1 m']))
		sec1.add(factory.build('Image', [top: '0 m', base: '1 m', path: 'file:section1.jpeg']))

		def sec2 = project.createContainer('Section 2')
		sec2.add(factory.build('Section', [top: '1 m', base: '2 m']))
		sec2.add(factory.build('Image', [top: '1 m', base: '2 m', path: 'file:section2.jpeg']))

		(model, view, controller) = GH.createMVCGroup(app, 'SectionCombo', 'SectionCombo', [project: project])
	}
	
	void tearDown() {
		GH.destroyMVCGroup(app, 'SectionCombo')
	}
	
	void testExportAll() {
		view.section.selectedIndex = 0
		assert controller.selection == 'All sections' 
		def containers = controller.containers
		assert containers
		assert containers.size() == 1
		assert containers[project.name].models.size() == 4
	}
	
	void testExportEach() {
		view.section.selectedIndex = 1
		assert controller.selection == 'Each section' 
		def containers = controller.containers
		assert containers
		assert containers.size() == 2
		assert containers['Section 1'].models.size() == 2
		assert containers['Section 2'].models.size() == 2
	}
	
	void testExportOne() {
		view.section.selectedIndex = 2
		assert controller.selection == 'Section 1' 
		def containers = controller.containers
		assert containers
		assert containers.size() == 1
		assert containers['Section 1'].models.size() == 2
	}
}