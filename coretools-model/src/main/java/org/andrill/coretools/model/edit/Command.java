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
 * Defines the interface for a command which encapsulates some action that may be undoable.
 * 
 * @author Josh Reed (jareed@psicat.org)
 */
public interface Command {

	/**
	 * Check whether this command can execute.
	 * 
	 * @return true if the command can execute, false otherwise.
	 */
	boolean canExecute();

	/**
	 * Checks whether this command can be undone.
	 * 
	 * @return true if the command can be undone, false otherwise.
	 */
	boolean canUndo();

	/**
	 * Execute this command.
	 */
	void execute();

	/**
	 * Gets the label for this command.
	 * 
	 * @return the label.
	 */
	String getLabel();

	/**
	 * Re-execute this command.
	 */
	void redo();

	/**
	 * Undo this command.
	 */
	void undo();
}
