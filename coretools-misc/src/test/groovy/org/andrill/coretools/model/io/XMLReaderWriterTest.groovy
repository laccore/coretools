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
package org.andrill.coretools.model.io

import org.andrill.coretools.Platform
import org.andrill.coretools.misc.io.XMLReaderWriter
import org.andrill.coretools.model.DefaultContainer;
import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.ModelManager;
import org.andrill.coretools.model.io.ModelFormatManager;

import groovy.util.GroovyTestCase
class XMLReaderWriterTest extends GroovyTestCase {
	
	void setUp() {
		Platform.start()
	}
	
	void testFormat() {
		assert "xml" == Platform.getService(ModelFormatManager.class).getReader('xml').format
	}
	
	// tests the wiring
	void testRegistered() {
		def formats = Platform.getService(ModelFormatManager.class)
		assert null != formats.getReader("xml")
	}

	void testRead() {
		def xmlrw = Platform.getService(ModelFormatManager.class).getReader('xml')
		Platform.getService(ModelManager.class).register(new TestFactory())
		
		def xml = new ByteArrayInputStream(
			"""	<container>
					<model type="Test">
						<property name="top">0 m</property>
						<property name="base">10 m</property>
					</model>
				</container>
			""".getBytes("UTF-8")
		)
		
		def container = new DefaultContainer()
		xmlrw.read(container, xml)
		
		assert 1 == container.models.size()
		def model = container.models[0]
		assert "Test" == model.modelType
		assert "0 m" == model.modelData?.top?.toString()
		assert "10 m" == model.modelData?.base?.toString()
	}
	
	void testWrite() {
		def xmlrw = Platform.getService(ModelFormatManager.class).getReader('xml')
		def container = new DefaultContainer()
		container.add(new TestModel(modelType:"Test", modelData:["top":"0 m", "base":"10 m"]))
		
		def stream = new ByteArrayOutputStream()
		xmlrw.write(container, stream)

		assert """<container>
  <model type='Test'>
    <property name='top'>0 m</property>
    <property name='base'>10 m</property>
  </model>
</container>""" == stream.toString()
	}
}

class TestFactory implements ModelManager.Factory {
	Model build(String type, Map<String, String> data) {
		return new TestModel(modelType: type, modelData: data)
	}

	String[] types = ["Test"] as String[]
}

class TestModel implements Model {
	ModelContainer container
	String modelType
	Map<String,String> modelData
	void updated() { }
	def getAdapter(Class<?> adapter) { null }
}
