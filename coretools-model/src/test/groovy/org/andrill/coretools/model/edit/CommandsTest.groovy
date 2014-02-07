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
package org.andrill.coretools.model.edit

import org.andrill.coretools.model.*

import groovy.util.GroovyTestCase

class CommandsTest extends GroovyTestCase {

    void testCompositeCommand() {
        def c1 = new TestCommand()
        def c2 = new TestCommand()

        // test initial state
        def command = new CompositeCommand("Composite", c1, c2)
        assert "Composite" == command.label
        assert null == c1.command
        assert null == c2.command

        // test execute
		assert command.canExecute()
        command.execute()
        assert "execute" == c1.command
        assert "execute" == c2.command

        // test undo
		assert command.canUndo()
        command.undo()
        assert "undo" == c1.command
        assert "undo" == c2.command

        // test redo
		assert command.canExecute()
        command.redo()
        assert "execute" == c1.command
        assert "execute" == c2.command
    }

    void testCreateCommand() {
        def model = new TestModel()
        def container = new DefaultContainer()
        assert 0 == container.models.size()

        def command = new CreateCommand(model, container)
        assert "Create: TestModel" == command.label
        
        command.execute()
        assert 1 == container.models.size()
        assert model == container.models[0]

        command.undo()
        assert 0 == container.models.size()
    }

    void testDeleteCommand() {
        def model = new TestModel()
        def container = new DefaultContainer()
		container.add(model)
        assert 1 == container.models.size()
        assert model == container.models[0]

        def command = new DeleteCommand(model, container)
        assert "Delete: TestModel" == command.label

        command.execute()
        assert 0 == container.models.size()

        command.undo()
        assert 1 == container.models.size()
        assert model == container.models[0]
    }
}