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
package org.andrill.coretools.geology.models.util;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.andrill.coretools.geology.models.GeologyModel;
import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;

import java.util.*;

/**
 * Indexes GeologyModels so they can be efficiently queried by range intersection.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class GeologyModelIndex implements ModelContainer.Listener {
	/**
	 * Defines the interface for a index filter.
	 * 
	 * @author Josh Reed (jareed@andrill.org)
	 */
	public interface Filter {
		boolean accept(GeologyModel model);
	}

	// a simple bookkeeping class so we can efficiently update models
	static class IndexRange {
		int min;
		int max;

		IndexRange(int min, int max) {
			this.min = min;
			this.max = max;
		}
	}

	protected static Comparator<GeologyModel> SORT = new Comparator<GeologyModel>() {
		public int compare(GeologyModel o1, GeologyModel o2) {
			int result = Double.compare(o1.getIndexMin(), o2.getIndexMin());
			if (result == 0) {
				result = Double.compare(o1.getIndexMax(), o2.getIndexMax());
			}
			return result;
		}
	};

	protected Map<GeologyModel, IndexRange> rangeMap = new HashMap<GeologyModel, IndexRange>();
	protected Multimap<Integer, GeologyModel> indexMap = LinkedHashMultimap.create();
	protected ModelContainer container = null;

	/**
	 * Adds a model to the index.
	 * 
	 * @param model
	 *            the model to add.
	 */
	public void add(GeologyModel model) {
		int min = (int) model.getIndexMin();
		int max = (int) model.getIndexMax();
		rangeMap.put(model, new IndexRange(min, max));
		for (int i = min; i <= max; i++) {
			indexMap.put(i, model);
		}
	}

	/**
	 * Connects this index to a model container.
	 * 
	 * @param container
	 *            the model container.
	 */
	public void connect(ModelContainer container) {
		if (container != this.container) {
			reset();
			if (this.container != null) {
				this.container.removeListener(this);
			}
			this.container = container;
			if (this.container != null) {
				this.container.addListener(this);
				for (Model m : container.getModels()) {
					if (m instanceof GeologyModel) {
						add((GeologyModel) m);
					}
				}
			}
		}
	}

	/**
	 * Disconnects this index from a model container.
	 */
	public void disconnect() {
		reset();
		if (this.container != null) {
			this.container.removeListener(this);
		}
		this.container = null;
	}

	/**
	 * Gets the models for a specific value.
	 * 
	 * @param value
	 *            the value.
	 * @return the set of geology models.
	 */
	public List<GeologyModel> get(Number value) {
		return get(value, value);
	}

	/**
	 * Gets all models that might intersect the specified range.
	 * 
	 * @param start
	 *            the start.
	 * @param end
	 *            the end.
	 * @return the set of geology models.
	 */
	public List<GeologyModel> get(Number start, Number end) {
		return get(start, end, null);
	}

	/**
	 * Gets all models matching a filter that might intersect the specified range.
	 * 
	 * @param start
	 *            the start.
	 * @param end
	 *            the end.
	 * @param filter
	 *            the filter.
	 * @return the set of matching geology models.
	 */
	public List<GeologyModel> get(Number start, Number end, Filter filter) {
		List<GeologyModel> set = new ArrayList<GeologyModel>();
		for (int i = start.intValue(); i <= end.intValue(); i++) {
			Collection<GeologyModel> list = indexMap.get(i);
			if (list != null) {
				for (GeologyModel m : list) {
					if (!set.contains(m) && (filter == null || filter.accept(m))) {
						set.add(m);
					}
				}
			}
		}
		Collections.sort(set, SORT);
		return set;
	}

	/**
	 * Gets the set of all geology models in the index.
	 * 
	 * @return the set of all geology models.
	 */
	public List<GeologyModel> getAllModels() {
		return getAllModels(null);
	}

	/**
	 * Gets the set of all geology models in the index.
	 * 
	 * @param filter
	 *            the filter.
	 * 
	 * @return the set of all geology models.
	 */
	public List<GeologyModel> getAllModels(Filter filter) {
		List<GeologyModel> set = new ArrayList<GeologyModel>();
		for (GeologyModel m : rangeMap.keySet()) {
			if (!set.contains(m) && (filter == null || filter.accept(m))) {
				set.add(m);
			}
		}
		Collections.sort(set, SORT);
		return set;
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelAdded(Model model) {
		if (model instanceof GeologyModel) {
			add((GeologyModel) model);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelRemoved(Model model) {
		if (model instanceof GeologyModel) {
			remove((GeologyModel) model);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelUpdated(Model model) {
		if (model instanceof GeologyModel) {
			update((GeologyModel) model);
		}
	}

	/**
	 * Removes a model from the index.
	 * 
	 * @param model
	 *            the model.
	 */
	public void remove(GeologyModel model) {
		int min = (int) model.getIndexMin();
		int max = (int) model.getIndexMax();
		rangeMap.remove(model);
		for (int i = min; i <= max; i++) {
			indexMap.remove(i, model);
		}
	}

	/**
	 * Gets the minimum index value.
	 * 
	 * @return the minimum index value.
	 */
	public int getMinIndex() {
		Set<Integer> keys = indexMap.keySet();
		if (keys.size() == 0) {
			return 0;
		} else {
			return keys.toArray(new Integer[0])[0];
		}
	}

	/**
	 * Gets the maximum index value.
	 * 
	 * @return the maximum index value.
	 */
	public int getMaxIndex() {
		Set<Integer> keys = indexMap.keySet();
		if (keys.size() == 0) {
			return 0;
		} else {
			return keys.toArray(new Integer[0])[keys.size() - 1];
		}
	}

	/**
	 * Resets this index.
	 */
	public void reset() {
		rangeMap.clear();
		indexMap.clear();
	}

	/**
	 * Updates a model in the index.
	 * 
	 * @param model
	 *            the model.
	 */
	public void update(GeologyModel model) {
		IndexRange oldRange = rangeMap.get(model);
		for (int i = oldRange.min; i <= oldRange.max; i++) {
			indexMap.remove(i, model);
		}
		add(model);
	}
}
