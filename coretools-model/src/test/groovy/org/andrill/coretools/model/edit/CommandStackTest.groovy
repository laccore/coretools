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

import java.beans.PropertyChangeEvent
import groovy.util.GroovyTestCase;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListener

class CommandStackTest extends GroovyTestCase {
    CommandStack edit

    void setUp() {
        edit = new CommandStack()
    }

    void testInitialReadOnly() {
        edit.editable = false
        assert !edit.canExecute()
        assert !edit.canUndo()
        assert !edit.canRedo()
        assert !edit.isEditable()
        assert 0 == edit.commands.size()
    }

    void testInitial() {
        assert edit.canExecute()
        assert !edit.canUndo()
        assert !edit.canRedo()
        assert edit.isEditable()
        assert 0 == edit.commands.size()
    }

    void testExecute() {
        def listener = new TestListener()
        edit.addPropertyChangeListener listener
        
        def command = new TestCommand()
        edit.execute command
        assert edit.canExecute()
        assert edit.canUndo()
        assert !edit.canRedo()
        assert 1 == edit.commands.size()
        assert command == edit.commands[0]
        assert "execute" == command.command
        assert "undo" == listener.last.propertyName

        // undo
        edit.undo()
        assert edit.canExecute()
        assert !edit.canUndo()
        assert edit.canRedo()
        assert 0 == edit.commands.size()
        assert "undo" == command.command
        assert "redo" == listener.last.propertyName

        // redo
        edit.removePropertyChangeListener listener
        listener.last = null
        edit.redo()
        assert edit.canExecute()
        assert edit.canUndo()
        assert !edit.canRedo()
        assert 1 == edit.commands.size()
        assert command == edit.commands[0]
        assert "execute" == command.command
        assert null == listener.last
    }
}

class TestListener implements PropertyChangeListener {
    PropertyChangeEvent last
    void propertyChange(final PropertyChangeEvent evt) { last = evt }
}