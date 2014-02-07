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

/**
 * A composite command contains multiple other commands that should be treated as a single command.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class CompositeCommand implements Command {
	protected final Command[] commands;
	protected final String label;
	protected boolean executed = false;

	/**
	 * Create a new CompositeCommand with the specified label and commands.
	 * 
	 * @param label
	 *            the label.
	 * @param commands
	 *            the commands.
	 */
	public CompositeCommand(final String label, final Command... commands) {
		this.label = label;
		this.commands = commands;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canExecute() {
		boolean executable = true;
		for (Command c : commands) {
			executable &= c.canExecute();
		}
		return !executed && executable;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canUndo() {
		boolean undoable = true;
		for (Command c : commands) {
			undoable &= c.canUndo();
		}
		return executed && undoable;
	}

	/**
	 * {@inheritDoc}
	 */
	public void execute() {
		if (canExecute()) {
			for (Command c : commands) {
				c.execute();
			}
			executed = true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * {@inheritDoc}
	 */
	public void redo() {
		execute();
	}

	/**
	 * {@inheritDoc}
	 */
	public void undo() {
		if (canUndo()) {
			for (Command c : commands) {
				c.undo();
			}
			executed = false;
		}
	}
}
