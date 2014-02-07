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
package psicat.dialogs;

import griffon.util.IGriffonApplication
import griffon.util.GriffonApplicationHelper as GH

import org.andrill.coretools.Platform
import org.andrill.coretools.model.ModelManager

import jxl.Workbook;
import psicat.ProjectHelper

class ExportTabularWizardTests extends GroovyTestCase {
	IGriffonApplication app
	def project
	def model
	def view
	def controller
	def dir
	
	void setUp() {
		dir = ProjectHelper.createTempDir()
		
		def factory = Platform.getService(ModelManager.class)
		project = ProjectHelper.createProject('Test Project', 'top')
		def sec1 = project.createContainer('Section 1')
		sec1.add(factory.build('Section', [top: '0 m', base: '1 m']))
		sec1.add(factory.build('Image', [top: '0 m', base: '1 m', path: 'file:section1.jpeg']))
		sec1.add(factory.build('Interval', [top: '0 m', base: '1 m', lithology: 'psicat:mud']))
		def sec2 = project.createContainer('Section 2')
		sec2.add(factory.build('Section', [top: '1 m', base: '2 m']))
		sec2.add(factory.build('Image', [top: '1 m', base: '2 m', path: 'file:section2.jpeg']))
		sec2.add(factory.build('Interval', [top: '1 m', base: '2 m', lithology: 'psicat:sand']))
		
		(model, view, controller) = GH.createMVCGroup(app, 'ExportTabularWizard', 'ExportTabularWizard', [project: project])
	}
	
	void tearDown() {
		GH.destroyMVCGroup(app, 'ExportTabularWizard')
	}
	
	void testExportAll() {
		app.views['exportTabularSections'].section.selectedIndex = 0
		model.filePath = new File(dir, 'TestAll.xls').absolutePath
		assert controller.exportTabular(app.controllers['exportTabularSections'].containers, 'XLS') == 'Exported Test Project'
		verify(new File(dir, 'TestAll.xls'), 
					[	Sections: [['Top', 'Base'],
		              	           ['{$value}', '{$value}'],
		              	           ['0 m', '1 m'],
		              	           ['1 m', '2 m']], 
		              	Images:   [['Top', 'Base', 'Path'],
		              	           ['{$value}', '{$value}', '{$value}'],
		              	           ['0 m', '1 m', 'file:section1.jpeg'],
		              	           ['1 m', '2 m', 'file:section2.jpeg']], 
		              	Intervals:[['Top', 'Base', 'Lithology'],
		              	           ['{$value}', '{$value}', '{$value}'],
		              	           ['0 m', '1 m', 'psicat:mud'],
		              	           ['1 m', '2 m', 'psicat:sand']]])
	}
	
	void testExportEach() {
		app.views['exportTabularSections'].section.selectedIndex = 1
		model.filePath = new File(dir, 'TestEach.xls').absolutePath
		assert controller.exportTabular(app.controllers['exportTabularSections'].containers, 'XLS') == 'Exported each section'
		verify(new File(dir, 'TestEach_Section 1.xls'), 
				[	Sections: [['Top', 'Base'],
				 	           ['{$value}', '{$value}'],
				 	           ['0 m', '1 m']], 
				 	Images:   [['Top', 'Base', 'Path'],
				 	           ['{$value}', '{$value}', '{$value}'],
				 	           ['0 m', '1 m', 'file:section1.jpeg']], 
					Intervals:[['Top', 'Base', 'Lithology'],
					           ['{$value}', '{$value}', '{$value}'],
					           ['0 m', '1 m', 'psicat:mud']]])
		verify(new File(dir, 'TestEach_Section 2.xls'), 
				[	Sections: [['Top', 'Base'],
				 	           ['{$value}', '{$value}'],
				 	           ['1 m', '2 m']], 
				 	Images:   [['Top', 'Base', 'Path'],
				 	           ['{$value}', '{$value}', '{$value}'],
				 	           ['1 m', '2 m', 'file:section2.jpeg']], 
				 	Intervals:[['Top', 'Base', 'Lithology'],
				 	           ['{$value}', '{$value}', '{$value}'],
				 	           ['1 m', '2 m', 'psicat:sand']]])
	}
	
	void testExportOne() {
		app.views['exportTabularSections'].section.selectedIndex = 2
		model.filePath = new File(dir, 'Section1.xls').absolutePath
		assert controller.exportTabular(app.controllers['exportTabularSections'].containers, 'XLS') == 'Exported Section 1'
		verify(new File(dir, 'Section1.xls'), 
				[	Sections: [['Top', 'Base'],
				 	           ['{$value}', '{$value}'],
				 	           ['0 m', '1 m']], 
				 	Images:   [['Top', 'Base', 'Path'],
				 	           ['{$value}', '{$value}', '{$value}'],
				 	           ['0 m', '1 m', 'file:section1.jpeg']], 
				    Intervals:[['Top', 'Base', 'Lithology'],
				               ['{$value}', '{$value}', '{$value}'],
				               ['0 m', '1 m', 'psicat:mud']]])
	}
	
	void verify(file, sheets) {
		Workbook workbook = Workbook.getWorkbook(file)
		assert workbook.sheets.length == sheets.size()
		sheets.each { k,v ->
			def sheet = workbook.getSheet(k)
			assert sheet
			v.eachWithIndex { r,i ->
				r.eachWithIndex { c,j ->
					assert sheet.getCell(j, i).getString() == c
				}
			}
		}
	}
}