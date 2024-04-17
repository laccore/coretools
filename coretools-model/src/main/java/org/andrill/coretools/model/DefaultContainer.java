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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * A default implementation of the {@link ModelContainer} interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DefaultContainer implements ModelContainer, Iterable<Model> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultContainer.class);
	protected List<Model> models = new ArrayList<Model>();
	protected List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
	protected Project project;

	/**
	 * {@inheritDoc}
	 */
	public void add(final Model model) {
		if (models.add(model)) {
			model.setContainer(this);
			for (Listener l : listeners) {
				//LOGGER.debug("Notifiying {} of added model", l);
				l.modelAdded(model);
			}
			//LOGGER.debug("Added model {}", model);
		}
	}

	/**
	 * Adds all models to this container.
	 * 
	 * @param models
	 *            the list of models to add.
	 */
	public void addAll(final List<Model> models) {
		for (Model m : models) {
			add(m);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void addListener(final Listener listener) {
		listeners.add(listener);
		//LOGGER.debug("Added listener {}", listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableList<Model> getModels() {
		return ImmutableList.copyOf(models);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Model> getMutableModels() {
		return new ArrayList<Model>(models);
	}

	/**
	 * {@inheritDoc}
	 */
	public Project getProject() {
		return project;
	}

	/**
	 * {@inheritDoc}
	 */
	public Iterator<Model> iterator() {
		return models.iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	public void remove(final Model model) {
		if (models.remove(model)) {
			model.setContainer(null);
			for (Listener l : listeners) {
				//LOGGER.debug("Notifiying {} of removed model", l);
				l.modelRemoved(model);
			}
			//LOGGER.debug("Removed model {}", model);
		}
	}

	/**
	 * Removes all models from this container.
	 * 
	 * @param models
	 *            the list of models to remove.
	 */
	public void removeAll(final List<Model> models) {
		for (Model m : models) {
			remove(m);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeListener(final Listener listener) {
		listeners.remove(listener);
		//LOGGER.debug("Removed listener {}", listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setProject(final Project project) {
		this.project = project;
	}

	/**
	 * {@inheritDoc}
	 */
	public void update(final Model model) {
		if (models.contains(model)) {
			for (Listener l : listeners) {
				//LOGGER.debug("Notifiying {} of updated model", l);
				l.modelUpdated(model);
			}
			//LOGGER.debug("Updated model {}", model);
		}
	}
}
