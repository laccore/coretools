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

import groovy.util.GroovyTestCase;

class AbstractCommandTest extends GroovyTestCase {

    void testCommand() {
        def command = new TestCommand()

        // test initial state
        assert command.canExecute()
        assert !command.canUndo()
        assert command.label == "TestCommand"
        assert null == command.command

        // test execute
        command.execute()
        assert "execute" == command.command
        assert !command.canExecute()
        assert command.canUndo()

        // test undo
        command.undo()
        assert "undo" == command.command
        assert command.canExecute()
        assert !command.canUndo()

        // test redo
        command.redo()
        assert "execute" == command.command
        assert !command.canExecute()
        assert command.canUndo()
    }
}

class TestCommand extends AbstractCommand {
    String label = "TestCommand"
    String command

    protected void executeCommand() { command = "execute" }
    protected void undoCommand()    { command = "undo"}
}