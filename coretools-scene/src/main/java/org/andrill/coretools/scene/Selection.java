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
package org.andrill.coretools.scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.andrill.coretools.Adaptable;
import org.andrill.coretools.AdapterManager;
import org.andrill.coretools.Platform;

/**
 * Represents a selection.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class Selection implements Adaptable, Iterable<Object> {
	public static final Selection EMPTY = new Selection(Collections.emptyList());
	protected final List<Object> selection;
	protected final AdapterManager manager;

	/**
	 * Create a new empty Selection.
	 */
	public Selection() {
		this(Collections.emptyList());
	}

	/**
	 * Create a new Selection with the specified list of objects.
	 * 
	 * @param objects
	 *            the list of selected objects.
	 */
	public Selection(final List<Object> objects) {
		this(objects, Platform.getService(AdapterManager.class));
	}

	/**
	 * Create a new Selection with the specified list of objects and AdapterManager.
	 * 
	 * @param objects
	 *            the list of selected objects.
	 * @param manager
	 *            the adapter manager.
	 */
	public Selection(final List<Object> objects, final AdapterManager manager) {
		selection = objects;
		this.manager = manager;
	}

	/**
	 * Create a new Selection with the specified object.
	 * 
	 * @param selection
	 *            the selected object.
	 */
	public Selection(final Object selection) {
		this(Arrays.asList(selection));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!Selection.class.isAssignableFrom(obj.getClass())) {
			return false;
		}
		Selection other = (Selection) obj;
		if (selection == null) {
			if (other.getSelectedObjects() != null) {
				return false;
			}
		} else if (!selection.equals(other.getSelectedObjects())) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> E getAdapter(final Class<E> adapterType) {
		for (final Object o : this) {
			Object result = null;
			if (o instanceof Adaptable) {
				result = ((Adaptable) o).getAdapter(adapterType);
			} else if (manager != null) {
				result = manager.getAdapter(o, adapterType);
			}
			if (result != null) {
				return adapterType.cast(result);
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> List<E> getAllAdapters(final Class<E> adapterType) {
		final List<E> results = new ArrayList<E>();
		for (final Object o : this) {
			Object result = null;
			if (o instanceof Adaptable) {
				result = ((Adaptable) o).getAdapter(adapterType);
			} else if (manager != null) {
				result = manager.getAdapter(o, adapterType);
			}
			if (result != null) {
				results.add(adapterType.cast(o));
			}
		}
		return results;
	}

	/**
	 * Gets the first object in this selection.
	 * 
	 * @return the first selected object or null if this selection is empty.
	 */
	public Object getFirstObject() {
		if (selection.size() == 0) {
			return null;
		} else {
			return selection.get(0);
		}
	}

	/**
	 * Gets the list of objects in this selection.
	 * 
	 * @return the list of selected objects.
	 */
	public List<Object> getSelectedObjects() {
		return Collections.unmodifiableList(selection);
	}

	/**
	 * Gets the size of this selection.
	 * 
	 * @return the size.
	 */
	public int getSize() {
		return selection.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((selection == null) ? 0 : selection.hashCode());
		return result;
	}

	/**
	 * Checks whether this selection is empty or not.
	 * 
	 * @return true if this selection is empty, false otherwise.
	 */
	public boolean isEmpty() {
		return selection.size() == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public Iterator<Object> iterator() {
		return selection.iterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "Selection: " + selection;
	}
}
