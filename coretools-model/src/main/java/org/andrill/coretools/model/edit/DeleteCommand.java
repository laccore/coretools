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

import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;

/**
 * A command for deleting a model from a container.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DeleteCommand extends AbstractCommand {
	protected Model model = null;
	protected ModelContainer container = null;

	/**
	 * Create a new DeleteCommand.
	 * 
	 * @param model
	 *            the model.
	 * @param container
	 *            the container.
	 */
	public DeleteCommand(final Model model, final ModelContainer container) {
		this.model = model;
		this.container = container;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void executeCommand() {
		container.remove(model);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getLabel() {
		return "Delete: " + model.getModelType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void undoCommand() {
		container.add(model);
	}
}
