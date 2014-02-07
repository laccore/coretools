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
package org.andrill.coretools

import groovy.util.GroovyTestCase;

class DefaultResourceLoaderTest extends GroovyTestCase {
	ResourceLoader loader

	void setUp() {
		loader = new DefaultResourceLoader()
	}

	void testLoadClassPath() {
		def url = loader.getResource("classpath:/META-INF/services/org.andrill.coretools.TestService")
		checkExists url
		assert url == loader.getResource("rsrc:/META-INF/services/org.andrill.coretools.TestService")
	}

	void testHttp() {
		def url = loader.getResource("http://google.com")
		assert "http://google.com" == url.toExternalForm()
	}

	void testInvalid() {
		assert null == loader.getResource("rsrc://////invalid/resource")

		def file = loader.getResource("foobar").toExternalForm()
		assert file.startsWith("file:")
		assert file.endsWith("foobar")
	}

	void testAddResource() {
		def file = new File("./testFile")
		try {
			file.write("Test")
			loader.addResource(new URL("file:" + file.parentFile.absolutePath))

			def found = loader.getResource(file.absolutePath)
			checkExists found
		} finally {
			file.delete()
		}
	}

	void checkExists(url) {
		assert url != null
		assert url.toExternalForm().startsWith('file:/')
		assert new File(url.toURI()).exists()
	}
}