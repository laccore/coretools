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
package org.andrill.coretools.model;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.inject.ImplementedBy;

/**
 * Acts as a container for {@link Model}s.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@ImplementedBy(DefaultContainer.class)
public interface ModelContainer {

	/**
	 * Listens to a {@link ModelContainer} for changes.
	 */
	public interface Listener {

		/**
		 * Called when a model is added to the container.
		 * 
		 * @param model
		 *            the added model.
		 */
		void modelAdded(Model model);

		/**
		 * Called when a model is removed from the container.
		 * 
		 * @param model
		 *            the removed model.
		 */
		void modelRemoved(Model model);

		/**
		 * Called when a model in the container is updated.
		 * 
		 * @param model
		 *            the updated model.
		 */
		void modelUpdated(Model model);
	}

	/**
	 * Adds a model to this container.
	 * 
	 * @param model
	 *            the model.
	 */
	void add(Model model);

	/**
	 * Adds a container listener.
	 * 
	 * @param listener
	 *            the listener.
	 */
	void addListener(Listener listener);

	/**
	 * Gets an immutable list of models in this container.
	 * 
	 * @return the list of models.
	 */
	ImmutableList<Model> getModels();

	/**
	 * Gets a list of models in this container.
	 * 
	 * @return the list of models.
	 */
	List<Model> getMutableModels();

	/**
	 * Gets the project this container belongs to.
	 * 
	 * @return the container.
	 */
	Project getProject();

	/**
	 * Removes a model from this container.
	 * 
	 * @param model
	 *            the model.
	 */
	void remove(Model model);

	/**
	 * Removes a container listener.
	 * 
	 * @param listener
	 *            the listener.
	 */
	void removeListener(Listener listener);

	/**
	 * Sets the project this container belongs to.
	 * 
	 * @param project
	 *            the project.
	 */
	void setProject(Project project);

	/**
	 * Indicate that the specified model has been modified.
	 * 
	 * @param model
	 *            the model.
	 */
	void update(Model model);
}
