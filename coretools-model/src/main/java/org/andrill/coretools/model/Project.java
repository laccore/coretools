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

import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Represents a collection of model containers and configuration.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Project {
	public String CONTAINERS_KEY = "containers";

	/**
	 * Adds a property change listener.
	 * 
	 * @param l
	 *            the listener.
	 */
	void addPropertyChangeListener(PropertyChangeListener l);

	/**
	 * Closes the specified model container.
	 * 
	 * @param container
	 *            the container.
	 */
	void closeContainer(ModelContainer container);

	/**
	 * Creates a new model container with the specified name.
	 * 
	 * @param name
	 *            the name.
	 * @return the new model container.
	 */
	ModelContainer createContainer(String name);

	/**
	 * Gets the configuration of this project.
	 * 
	 * @return the configuration.
	 */
	Map<String, String> getConfiguration();

	/**
	 * Gets the list of model container names.
	 * 
	 * @return the containers.
	 */
	List<String> getContainers();

	/**
	 * Gets the name of this project.
	 * 
	 * @return the name.
	 */
	String getName();

	/**
	 * Test the path of this project.
	 * 
	 * @return the path.
	 */
	URL getPath();

	/**
	 * Gets the list of scene URLs associated with this Project.
	 * 
	 * @return the list of scene URLs.
	 */
	List<URL> getScenes();

	/**
	 * Opens the model container with the specified name.
	 * 
	 * @param name
	 *            the name.
	 * @return the model container.
	 */
	ModelContainer openContainer(String name);

	/**
	 * Removes a property change listener.
	 * 
	 * @param l
	 *            the listener.
	 */
	void removePropertyChangeListener(PropertyChangeListener l);

	/**
	 * Saves the project.
	 */
	void save();

	/**
	 * Saves the specified model container.
	 * 
	 * @param container
	 *            the container.
	 */
	void saveContainer(ModelContainer container);
}
