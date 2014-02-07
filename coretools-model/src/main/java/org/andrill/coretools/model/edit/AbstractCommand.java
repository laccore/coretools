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
 * An abstract implementation of the Command interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class AbstractCommand implements Command {
	protected boolean executed = false;

	/**
	 * {@inheritDoc}
	 */
	public boolean canExecute() {
		return !executed;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canUndo() {
		return executed;
	}

	/**
	 * {@inheritDoc}
	 */
	public void execute() {
		if (canExecute()) {
			executeCommand();
			executed = true;
		}
	}

	/**
	 * Execute the command action.
	 */
	protected abstract void executeCommand();

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
			undoCommand();
			executed = false;
		}
	}

	/**
	 * Undo the command action.
	 */
	protected abstract void undoCommand();
}
