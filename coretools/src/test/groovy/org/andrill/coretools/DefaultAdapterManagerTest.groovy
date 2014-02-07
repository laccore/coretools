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

class DefaultAdapterManagerTest extends GroovyTestCase {
	AdapterManager adapterManager

	void setUp() {
		adapterManager = new DefaultAdapterManager()
	}

	void testRegisterUnregisterAdapters() {
		assert null == adapterManager.getAdapter("FOOBAR", String.class)
		def factory = [
		    getAdapter: { obj, clazz ->  obj.toLowerCase() },
			getAdapterTypes: { -> [String.class] as Class[] }
		] as AdapterManager.Factory
		adapterManager.register(factory, String.class)
		assert "foobar" == adapterManager.getAdapter("FOOBAR", String.class)

		adapterManager.unregister(factory, String.class)
		assert null == adapterManager.getAdapter("FOOBAR", String.class)
	}
}