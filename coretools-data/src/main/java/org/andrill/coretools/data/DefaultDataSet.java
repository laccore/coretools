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
package org.andrill.coretools.data;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * An implementation of the DataSet interface that hashes x-space into fixed-size segments to provide efficient random
 * access and range queries.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DefaultDataSet implements DataSet {

	/**
	 * A simple hash function that simply floors the value.
	 */
	public static class FloorHash implements Hash {
		final double scale;
		
		public FloorHash() {
			scale = 1;
		}
		
		public FloorHash(double scale) {
			this.scale = scale;
		}
		
		public int hash(double x) {
			return (int) Math.floor((x + 0.000000001) * (scale + 0.000000001));
		}
	}

	/**
	 * Averages the y-values in a fixed size window.
	 */
	public static class Average implements Interpolation {
		final int window;
		final boolean center;

		/**
		 * Create a new moving average interpolation.
		 * 
		 * @param window
		 *            the window size in data points.
		 * @param center
		 *            whether the window is centered or previous only.
		 */
		public Average(final int window, final boolean center) {
			this.window = window;
			this.center = center;
		}

		public Datum interpolate(final double x, final DataSet dataset) {
			// check if we need interpolation
			Datum exact = dataset.get(x);
			if (exact != null) {
				return exact;
			}

			// average the data points in the window
			double sum = 0.0;
			int count = 0;
			for (Datum d : dataset.get(x, center ? window / 2 : window, center ? window / 2 : 0)) {
				sum += d.y;
				count++;
			}
			return new Datum(x, sum / count);
		}
	}

	protected static final DecimalFormat DEC = new DecimalFormat("0.0#####");

	/**
	 * Performs no interpolation, simply returns the actual value or null.
	 */
	public static final Interpolation NONE = new Interpolation() {
		public Datum interpolate(final double x, final DataSet dataset) {
			return dataset.get(x);
		}
	};

	/**
	 * Returns the y-value of the point nearest the actual point.
	 */
	public static final Interpolation NEAREST = new Interpolation() {
		public Datum interpolate(final double x, final DataSet dataset) {
			List<Datum> around = dataset.get(x, 1, 1);
			switch (around.size()) {
				case 0:
					return null;
				case 1:
					return new Datum(x, around.get(0).y);
				case 3:
					return around.get(1); // no interpolation needed
				default:
					if (x - around.get(0).x <= around.get(1).x - x) {
						return new Datum(x, around.get(0).y);
					} else {
						return new Datum(x, around.get(0).y);
					}
			}
		}
	};

	/**
	 * Averages the y-value of the data points on either side of the desired x-value.
	 */
	public static final Interpolation LINEAR = new Average(2, true);

	protected final String name;
	protected final Hash function;
	protected final SortedMap<Integer, List<Datum>> segments = new TreeMap<Integer, List<Datum>>();
	protected int count = 0;
	protected int segMax = 0;
	protected double max = Double.MIN_VALUE;
	protected double min = Double.MAX_VALUE;

	/**
	 * Create a new DefaultDataSet.
	 * 
	 * @param name
	 *            the name.
	 */
	public DefaultDataSet(final String name) {
		this.name = name;
		this.function = new FloorHash();
	}

	/**
	 * Create a new DefaultDataSet.
	 * 
	 * @param name
	 *            the name.
	 * @param hash
	 *            the hash.
	 */
	public DefaultDataSet(final String name, Hash hash) {
		this.name = name;
		this.function = hash;
	}

	/**
	 * {@inheritDoc}
	 */
	public Datum get(final double x) {
		List<Datum> segment = segments.get(function.hash(x));
		if (segment == null) {
			return null;
		} else {
			int index = Collections.binarySearch(segment, new Datum(x, 0.0));
			return (index < 0) ? null : segment.get(index);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Datum> get(final double x1, final double x2) {
		List<Datum> data = new ArrayList<Datum>();
		for (int i = function.hash(x1); i <= function.hash(x2); i++) {
			List<Datum> segment = segments.get(function.hash(i));
			if (segment != null) {
				for (Datum pt : segment) {
					if ((pt.x >= x1) && (pt.x <= x2)) {
						data.add(pt);
					}
				}
			}
		}
		return data;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Datum> get(final double x, final int before, final int after) {
		return getSegmented(x, before, after);
	}

	protected List<Datum> getAll(final double x, final int before, final int after) {
		List<Datum> all = getAllData();
		int i = Collections.binarySearch(all, new Datum(x, 0));
		if (i < 0) {
			i = -(i + 1);
		}
		return all.subList(Math.max(0, i - before), Math.min(all.size(), i + after));
	}

	/**
	 * {@inheritDoc}
	 */
	public List<Datum> getAllData() {
		List<Datum> data = new ArrayList<Datum>();
		for (int i : segments.keySet()) {
			data.addAll(segments.get(i));
		}
		return data;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public double getMax() {
		return max;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public double getMin() {
		return min;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the segment for the specified x-value.
	 * 
	 * @param x
	 *            the x value.
	 * @return the segment or null if not found.
	 */
	public List<Datum> getSegment(final double x) {
		return segments.get(function.hash(x));
	}

	protected List<Datum> getSegmented(final double x, final int before, final int after) {
		List<Datum> data = new ArrayList<Datum>();

		// our remaining count
		int remainingBefore = before;
		int remainingAfter = after;

		// get our starting segment
		int key = function.hash(x);
		List<Datum> segment = segments.get(key);
		if (segment != null) {
			int i = Collections.binarySearch(segment, new Datum(x, 0));
			// matched so add
			if (i >= 0) {
				data.add(segment.get(i));
			}

			// get points before in this same segment
			if (i < 0) {
				i = -(i + 1);
			}
			List<Datum> foo = tail(segment.subList(0, i), remainingBefore);
			data.addAll(foo);
			remainingBefore -= foo.size();

			// gets the points after in this same segment
			foo = head(segment.subList(i, segment.size()), remainingAfter);
			data.addAll(foo);
			remainingAfter -= foo.size();
		}

		// spans segments so check before and after
		if (remainingBefore > 0) {
			SortedMap<Integer, List<Datum>> headMap = segments.headMap(key);
			while (!headMap.isEmpty() && (remainingBefore > 0)) {
				List<Datum> foo = tail(headMap.get(headMap.lastKey()), remainingBefore);
				data.addAll(0, foo);
				remainingBefore -= foo.size();
				headMap = headMap.headMap(headMap.lastKey());
			}
		}
		if (remainingAfter > 0) {
			SortedMap<Integer, List<Datum>> tailMap = segments.tailMap(key);
			while (!tailMap.isEmpty() && (remainingAfter > 0)) {
				List<Datum> foo = head(tailMap.get(tailMap.firstKey()), remainingAfter);
				data.addAll(foo);
				remainingAfter -= foo.size();
				tailMap = tailMap.tailMap(tailMap.firstKey());
			}
		}
		return data;
	}

//	protected int getSegmentKey(final double x) {
//		return new BigDecimal(DEC.format(x)).divide(loadFactor).intValue();
//	}

	protected List<Datum> head(final List<Datum> list, final int count) {
		return list.subList(0, Math.min(count, list.size()));
	}

	/**
	 * Interpolate datums over a range.
	 * 
	 * @param start
	 *            the starting x-value.
	 * @param end
	 *            the ending x-value.
	 * @param step
	 *            the step size.
	 * @param interpolation
	 *            the interpolation strategy.
	 * @return the list of interpolated data.
	 */
	public List<Datum> interpolate(final double start, final double end, final double step,
	        final Interpolation interpolation) {
		List<Datum> data = new ArrayList<Datum>();
		for (double x = start; x <= end; x += step) {
			Datum d = interpolate(x, interpolation);
			if (d != null) {
				data.add(d);
			}
		}
		return data;
	}

	/**
	 * Interpolate a datum for the specified x value.
	 * 
	 * @param x
	 *            the x value.
	 * @param interpolation
	 *            the interpolation strategy.
	 * @return the interpolated data point or null for none.
	 */
	public Datum interpolate(final double x, final Interpolation interpolation) {
		return interpolation.interpolate(x, this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(final Datum datum) {
		// get the segment
		int segmentIndex = function.hash(datum.x);
		List<Datum> segment = segments.get(segmentIndex);
		if (segment == null) {
			segment = new ArrayList<Datum>();
			segments.put(segmentIndex, segment);
		}

		// insert the datum
		int i = Collections.binarySearch(segment, datum);
		if (i < 0) {
			i = -(i + 1);
		}
		segment.add(i, datum);

		// update stats
		count++;
		max = Math.max(max, datum.y);
		min = Math.min(min, datum.y);
		segMax = Math.max(segMax, segment.size());
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(final double x, final double y) {
		put(new Datum(x, y));
	}

	/**
	 * 
	 * @param datum
	 */
	public void remove(final Datum datum) {
		if (getSegment(datum.x).remove(datum)) {
			count--;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void remove(final double x) {
		List<Datum> segment = getSegment(x);
		if (segment != null) {
			int index = Collections.binarySearch(segment, new Datum(x, 0.0));
			while (index >= 0) {
				if (segment.remove(index) != null) {
					count--;
				}
				index = Collections.binarySearch(segment, new Datum(x, 0.0));
			}
		}
	}

	protected List<Datum> tail(final List<Datum> list, final int count) {
		return list.subList(Math.max(0, list.size() - count), list.size());
	}
	
	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("DefaultDataSet[name: " + name);
		s.append(", points: " + count);
		s.append(", segments: " + segments.size());
		s.append(", max: " + max);
		s.append(", min: " + min);
		s.append(", max seg: " + segMax);
		s.append("]");
		return s.toString();
	}
}
