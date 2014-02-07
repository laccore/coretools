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

import java.awt.image.BufferedImage

import javax.imageio.ImageIO

import org.andrill.coretools.geology.models.Length;

import griffon.util.IGriffonApplication
import griffon.util.GriffonApplicationHelper as GH
import psicat.ProjectHelper

class ImportImageWizardTests extends GroovyTestCase {
	IGriffonApplication app
	def project
	def model
	def view
	def controller
	
	def imageDir
	
	ImportImageWizardTests() {
		// set up our image directory
		imageDir = ProjectHelper.createTempDir()
		image(imageDir, 'foo_5.00-6.00.jpeg', 100, 10000)
		image(imageDir, 'foo_6.00-7.00.jpeg', 100, 5000)
		image(imageDir, 'foo_7.00-8.00.jpeg', 100, 7500)

		// an extra file
		new File(imageDir, 'NotAnImage.txt').write("This is not an image")
	}
	
	void setUp() {
		project = ProjectHelper.createProject('Test Project', 'top')
		project.createContainer('Section 1')
		(model, view, controller) = GH.createMVCGroup(app, 'ImportImageWizard', 'ImportImageWizard', [project: project])
	}
	
	void tearDown() {
		GH.destroyMVCGroup(app, 'ImportImageWizard')
	}
	
	void testFindSetTopSetDPI() {
		model.filePath = imageDir.absolutePath
		model.parseTop = false
		model.top = '0.0'
		model.parseBase = false
		model.dpi = '254'
		
		def images = controller.findImages()
		assert images.size() == 3
		verify(images[0], 0.0, 1.0,  null, 'foo_5.00-6.00.jpeg')
		verify(images[1], 1.0, 1.5,  null, 'foo_6.00-7.00.jpeg')
		verify(images[2], 1.5, 2.25, null, 'foo_7.00-8.00.jpeg')
	}
	
	void testFindParseTopSetDPI() {
		model.filePath = imageDir.absolutePath
		model.parseTop = true
		model.parseBase = false
		model.dpi = '254'
		model.group = 'split'
		
		def images = controller.findImages()
		assert images.size() == 3
		verify(images[0], 5.0, 6.0,  'split', 'foo_5.00-6.00.jpeg')
		verify(images[1], 6.0, 6.5,  'split', 'foo_6.00-7.00.jpeg')
		verify(images[2], 7.0, 7.75, 'split', 'foo_7.00-8.00.jpeg')
	}
	
	void testFindSetTopParseBase() {
		model.filePath = imageDir.absolutePath
		model.parseTop = false
		model.top = '0.0'
		model.parseBase = true
		model.group = 'whole'
		
		def images = controller.findImages()
		assert images.size() == 3
		verify(images[0], 0.0, 5.0, 'whole', 'foo_5.00-6.00.jpeg')
		verify(images[1], 5.0, 6.0, 'whole', 'foo_6.00-7.00.jpeg')
		verify(images[2], 6.0, 7.0, 'whole', 'foo_7.00-8.00.jpeg')
	}
	
	void testFindParseTopParseBase() {
		model.filePath = imageDir.absolutePath
		model.parseTop = true
		model.parseBase = true
		
		def images = controller.findImages()
		assert images.size() == 3
		verify(images[0], 5.0, 6.0, null, 'foo_5.00-6.00.jpeg')
		verify(images[1], 6.0, 7.0, null, 'foo_6.00-7.00.jpeg')
		verify(images[2], 7.0, 8.0, null, 'foo_7.00-8.00.jpeg')
	}
	
	void testCreateSections() {
		model.filePath = imageDir.absolutePath
		model.parseTop = true
		model.parseBase = true
		
		controller.createSections(controller.findImages())

		assert project.containers.size() == 4
		['Section 1', 'foo_5.00-6.00', 'foo_6.00-7.00', 'foo_7.00-8.00'].each { assert it in project.containers }
		
		def c1 = project.openContainer('foo_5.00-6.00')
		assert c1
		assert c1.models.size() == 2
		verify(c1.models[0], new Length('5 m'), new Length('6 m'), null, 'foo_5.00-6.00.jpeg')
		def s1 = c1.models[1]
		assert s1
		assert s1.top == new Length('5 m')
		assert s1.base == new Length('6 m')
		assert s1.name == 'foo_5.00-6.00'
		
		def c2 = project.openContainer('foo_6.00-7.00')
		assert c2
		assert c2.models.size() == 2
		verify(c2.models[0], new Length('6 m'), new Length('7 m'), null, 'foo_6.00-7.00.jpeg')
		def s2 = c2.models[1]
		assert s2
		assert s2.top == new Length('6 m')
		assert s2.base == new Length('7 m')
		assert s2.name == 'foo_6.00-7.00'
		
		def c3 = project.openContainer('foo_7.00-8.00')
		assert c3
		assert c3.models.size() == 2
		verify(c3.models[0], new Length('7 m'), new Length('8 m'), null, 'foo_7.00-8.00.jpeg')
		def s3 = c3.models[1]
		assert s3
		assert s3.top == new Length('7 m')
		assert s3.base == new Length('8 m')
		assert s3.name == 'foo_7.00-8.00'
	}
	
	void testAddToContainer() {
		model.filePath = imageDir.absolutePath
		model.parseTop = true
		model.parseBase = true
		
		controller.createImages(controller.findImages(), 'Section 1')

		def container = project.openContainer('Section 1')
		assert container
		assert container.models.size() == 3
		verify(container.models[0], new Length('5 m'), new Length('6 m'), null, 'foo_5.00-6.00.jpeg')
		verify(container.models[1], new Length('6 m'), new Length('7 m'), null, 'foo_6.00-7.00.jpeg')
		verify(container.models[2], new Length('7 m'), new Length('8 m'), null, 'foo_7.00-8.00.jpeg')
	}
	
	void testAddToContainerBaseOrigin() {
		project.origin = 'base'
		model.filePath = imageDir.absolutePath
		model.parseTop = true
		model.parseBase = true
		
		controller.createImages(controller.findImages(), 'Section 1')
		
		def container = project.openContainer('Section 1')
		assert container
		assert container.models.size() == 3
		verify(container.models[0], new Length('6 m'), new Length('5 m'), null, 'foo_5.00-6.00.jpeg')
		verify(container.models[1], new Length('7 m'), new Length('6 m'), null, 'foo_6.00-7.00.jpeg')
		verify(container.models[2], new Length('8 m'), new Length('7 m'), null, 'foo_7.00-8.00.jpeg')
	}
	
	void testError() {
		shouldFail() {
			controller.findImages()
		}
	}
	
	private image(dir, name, width, height) {
		def img = new BufferedImage((int) width, (int) height, (int) 1)
		ImageIO.write(img, 'jpeg', new File(dir, name))
	}
	
	private verify(image, top, base, group, path) {
		assert image
		assert image.top == top
		assert image.base == base
		assert image.group == group
		assert image.path.toExternalForm().endsWith(path)
	}
}