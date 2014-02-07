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
package org.andrill.coretools.model.edit;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * Provides edit support.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CommandStack {
	public static final String EXECUTE_PROP = "execute";
	public static final String UNDO_PROP = "undo";
	public static final String REDO_PROP = "redo";
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandStack.class);
	protected final Stack<Command> commandStack;
	protected final Stack<Command> redoStack;
	protected final PropertyChangeSupport pcs;
	protected boolean editable = true;

	/**
	 * Create a new editable CommandStack.
	 */
	public CommandStack() {
		this(true);
	}

	/**
	 * Create a new CommandStack.
	 * 
	 * @param editable
	 *            the editable flag.
	 */
	public CommandStack(final boolean editable) {
		this.editable = editable;
		commandStack = new Stack<Command>();
		redoStack = new Stack<Command>();
		pcs = new PropertyChangeSupport(this);
		LOGGER.debug("initialized");
	}

	public void addPropertyChangeListener(final PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	/**
	 * Check whether this command stack can execute commands.
	 * 
	 * @return true if it can, false otherwise.
	 */
	public boolean canExecute() {
		return editable;
	}

	/**
	 * Checks whether the last command can be redone.
	 * 
	 * @return true if the last command can be redone, false otherwise.
	 */
	public boolean canRedo() {
		return editable && !redoStack.isEmpty() && redoStack.peek().canExecute();
	}

	/**
	 * Checks whether the last command can be undone.
	 * 
	 * @return true if the last command can be undone, false otherwise.
	 */
	public boolean canUndo() {
		return editable && !commandStack.isEmpty() && commandStack.peek().canUndo();
	}

	/**
	 * Executes the specified command.
	 * 
	 * @param command
	 *            the command.
	 */
	public void execute(final Command command) {
		if (editable && command.canExecute()) {
			boolean oldUndo = canUndo();
			boolean oldRedo = canRedo();
			commandStack.push(command);
			command.execute();
			LOGGER.debug("Executed command {}", command);
			pcs.firePropertyChange(UNDO_PROP, oldUndo, canUndo());
			pcs.firePropertyChange(REDO_PROP, oldRedo, canRedo());
		}
	}

	/**
	 * Gets the (possibly abbreviated) list of commands that have been executed.
	 * 
	 * @return the list of commands.
	 */
	public ImmutableList<Command> getCommands() {
		return ImmutableList.copyOf(commandStack);
	}

	/**
	 * Gets whether this command stack is editable.
	 * 
	 * @return true if is editable, false otherwise.
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * Redo the last undone command.
	 */
	public void redo() {
		if (canRedo()) {
			boolean oldUndo = canUndo();
			boolean oldRedo = canRedo();
			Command command = redoStack.pop();
			commandStack.push(command);
			command.execute();
			LOGGER.debug("Redo command {}", command);
			pcs.firePropertyChange(UNDO_PROP, oldUndo, canUndo());
			pcs.firePropertyChange(REDO_PROP, oldRedo, canRedo());
		}
	}

	public void removePropertyChangeListener(final PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

	/**
	 * Sets whether this edit support is editable.
	 * 
	 * @param editable
	 *            the editable flag.
	 */
	public void setEditable(final boolean editable) {
		boolean oldExecute = this.editable;
		boolean oldUndo = canUndo();
		boolean oldRedo = canRedo();
		this.editable = editable;
		pcs.firePropertyChange(EXECUTE_PROP, oldExecute, editable);
		pcs.firePropertyChange(UNDO_PROP, oldUndo, canUndo());
		pcs.firePropertyChange(REDO_PROP, oldRedo, canRedo());
	}

	/**
	 * Undo the last command.
	 */
	public void undo() {
		if (canUndo()) {
			boolean oldRedo = canRedo();
			redoStack.push(commandStack.pop());
			redoStack.peek().undo();
			LOGGER.debug("Undo command {}", redoStack.peek());
			pcs.firePropertyChange(UNDO_PROP, true, canUndo());
			pcs.firePropertyChange(REDO_PROP, oldRedo, canRedo());
		}
	}
}
