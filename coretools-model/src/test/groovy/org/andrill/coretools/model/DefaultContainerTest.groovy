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
package org.andrill.coretools.model

import groovy.util.GroovyTestCase;

import java.util.Map;

class DefaultContainerTest extends GroovyTestCase {
    ModelContainer container
    TestListener listener

    void setUp() {
        container = new DefaultContainer()
        listener = new TestListener()
        container.addListener(listener)
    }

    void testAddUpdateRemoveModel() {
        // add
        def model = new TestModel()
        container.add(model)

        // test add
        assert container.models.size() == 1
        assert container.models[0] == model
        assert listener.added == model
        assert listener.removed == null
        assert listener.updated == null

        // update
        listener.added = null
        model.modelData['foo'] = 'bar'
        model.updated()

        // test update
        assert container.models.size() == 1
        assert container.models[0] == model
        assert listener.added == null
        assert listener.removed == null
        assert listener.updated == model

        // remove
        listener.updated = null
        container.remove(model)

        // test remove
        assert container.models.size() == 0
        assert listener.added == null
        assert listener.removed == model
        assert listener.updated == null

        // remove listener
        listener.removed = null
        container.removeListener(listener)
        container.add(model)

        // test remove
        assert container.models.size() == 1
        assert container.models[0] == model
        assert listener.added == null
        assert listener.removed == null
        assert listener.updated == null
    }
}

class TestListener implements ModelContainer.Listener {
    def added = null
    def removed = null
    def updated = null

    void modelAdded(final Model model)    { added = model }
    void modelRemoved(final Model model)  { removed = model }
    void modelUpdated(final Model model)  { updated = model }
}

class TestModel implements Model {
    ModelContainer container
    String modelType = "TestModel"
    Map<String, String> modelData = [:]
    void updated() { 
		if (container) { container.update(this) } 
	}
	public <E> E getAdapter(final Class<E> adapter) {
	    return null;
	}
}