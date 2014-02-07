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

class DefaultModelManagerTest extends GroovyTestCase {
    ModelManager manager

    void setUp() {
        manager = new DefaultModelManager();
    }

    void testRegisterUnregister() {
        assert null == manager.build("TestModel", [:])
        def factory = new TestFactory()
        manager.register(factory)
        assert null != manager.build("TestModel", [:])
        manager.unregister(factory)
        assert null == manager.build("TestModel", [:])
    }

    void testMissingModel() {
        assert null == manager.build("MissingModel", [:])
    }
}

class TestFactory implements ModelManager.Factory {

    Model build(final String type, final Map<String, String> data) {
        if (type == "TestModel") {
            return new TestModel()
        }
        return null
    }
    
    String[] getTypes() {
        return ["TestModel"] as String[]
    }
}