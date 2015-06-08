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
import java.beans.PropertyChangeSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.internal.MapMaker;

/**
 * An abstract implementation of the Project interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class AbstractProject implements Project {
	static class Ref {
		ModelContainer container;
		AtomicInteger count = new AtomicInteger(0);

		Ref() {
		}

		Ref(final ModelContainer container) {
			this.container = container;
		}
	}

	protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	protected final Map<String, String> configuration = new MapMaker().makeMap();
	protected final ConcurrentMap<String, Ref> containers = new MapMaker().makeMap();
	protected final List<String> containerNames = new ArrayList<String>();
	protected final List<URL> scenes = new ArrayList<URL>();
	protected String name;
	protected URL path;
	protected boolean parsed = false;

	/**
	 * {@inheritDoc}
	 */
	public void addPropertyChangeListener(final PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	/**
	 * {@inheritDoc}
	 */
	public void closeContainer(final ModelContainer container) {
		for (Ref ref : containers.values()) {
			if (ref.container == container) {
				synchronized (ref) {
					if (ref.count.decrementAndGet() == 0) {
						ref.container = null;
						closed(container);
					}
				}
			}
		}
	}

	protected void closed(final ModelContainer container) {
		// hook to remove listeners
	}

	protected abstract ModelContainer create(String name);

	/**
	 * {@inheritDoc}
	 */
	public ModelContainer createContainer(final String name) {
		if (containers.containsKey(name)) {
			throw new IllegalArgumentException("Container with name '" + name + "' already exists!");
		}

		// build our reference
		if (containers.putIfAbsent(name, new Ref(create(name))) == null) {
			containerNames.add(name);
			containers.get(name).container.setProject(this);
			pcs.firePropertyChange(CONTAINERS_KEY, null, name);
		}

		// get the reference and return our container
		Ref ref = containers.get(name);
		ref.count.incrementAndGet();
		return ref.container;
	}
	
	protected abstract void delete(final String name);
	
	public void deleteContainer(final String name) {
		if (containers.containsKey(name)) {
			containers.remove(name);
			containerNames.remove(name);
			delete(name);
			pcs.firePropertyChange(CONTAINERS_KEY, name, null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, String> getConfiguration() {
		return configuration;
	}

	protected String getContainerName(final ModelContainer container) {
		for (Entry<String, Ref> entry : containers.entrySet()) {
			if (entry.getValue().container == container) {
				return entry.getKey();
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getContainers() {
		return containerNames;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	public URL getPath() {
		return path;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<URL> getScenes() {
		return scenes;
	}

	protected void init() {
		if (!parsed) {
			parsed = true;
			containerNames.addAll(load());
			for (String name : containerNames) {
				containers.put(name, new Ref());
			}
		}
	}

	protected abstract List<String> load();

	protected abstract ModelContainer open(String name);

	/**
	 * {@inheritDoc}
	 */
	public ModelContainer openContainer(final String name) {
		// get the reference
		Ref ref = containers.get(name);
		if (ref == null) {
			throw new IllegalArgumentException("No container with name '" + name + "'");
		}

		synchronized (ref) {
			if (ref.count.getAndIncrement() == 0) {
				ref.container = open(name);
				ref.container.setProject(this);
			}
			return ref.container;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePropertyChangeListener(final PropertyChangeListener l) {
		pcs.removePropertyChangeListener(l);
	}

	/**
	 * {@inheritDoc}
	 */
	public void save() {
		// save all of the open containers
		for (Ref ref : containers.values()) {
			if (ref.count.get() > 0) {
				saveContainer(ref.container);
			}
		}
	}

	protected abstract void save(ModelContainer container);

	/**
	 * {@inheritDoc}
	 */
	public void saveContainer(final ModelContainer container) {
		save(container);
	}

	/**
	 * Sets the name of this project.
	 * 
	 * @param name
	 *            the name.
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Sets the path of this project.
	 * 
	 * @param path
	 *            the path.
	 */
	public void setPath(final URL path) {
		this.path = path;
	}
}
