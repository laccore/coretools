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

import java.util.List;

/**
 * Defines the interface for a data set.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface DataSet {

	/**
	 * Defines the interface for a hash function.
	 */
	public interface Hash {
		/**
		 * Hashes the specified x value to an index.
		 * 
		 * @param x
		 *            the x value.
		 * @return the index.
		 */
		int hash(double x);
	}

	/**
	 * Defines the interface for an interpolation strategy.
	 */
	public interface Interpolation {
		/**
		 * Interpolate a datum for the specified x value.
		 * 
		 * @param x
		 *            the x value.
		 * @param dataset
		 *            the data set.
		 * @return the interpolated data point or null for none.
		 */
		public Datum interpolate(double x, DataSet dataset);
	}

	/**
	 * Gets a data point in this data set.
	 * 
	 * @param x
	 *            the x value.
	 * @return the datum.
	 */
	Datum get(double x);

	/**
	 * Gets a list of data points between the two specified x values, inclusive.
	 * 
	 * @param x1
	 *            the starting x value.
	 * @param x2
	 *            the ending x value.
	 * 
	 * @return the list of data points.
	 */
	List<Datum> get(double x1, double x2);

	/**
	 * Gets the data points at and around x.
	 * 
	 * @param x
	 *            the x value.
	 * @param before
	 *            the number of data points to grab before x.
	 * @param after
	 *            the number of data points to grab after x.
	 * @return the list of data.
	 */
	public List<Datum> get(double x, int before, int after);

	/**
	 * Gets all data in this data set.
	 * 
	 * @return the list of all data points.
	 */
	List<Datum> getAllData();
	
	/**
	 * Gets the maximum value of this data set.
	 * 
	 * @return the maximum value.
	 */
	double getMax();
	
	/**
	 * Gets the minimum value of this data set.
	 * 
	 * @return the minimum value.
	 */
	double getMin();

	/**
	 * Gets the name of this data set.
	 * 
	 * @return the name.
	 */
	String getName();

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
	        final Interpolation interpolation);

	/**
	 * Interpolate a datum for the specified x value.
	 * 
	 * @param x
	 *            the x value.
	 * @param interpolation
	 *            the interpolation strategy.
	 * @return the interpolated data point or null for none.
	 */
	public Datum interpolate(final double x, final Interpolation interpolation);

	/**
	 * Puts a data point in this data set.
	 * 
	 * @param datum
	 *            the data point.
	 */
	void put(Datum datum);

	/**
	 * Puts a data point in this data set.
	 * 
	 * @param x
	 *            the x value.
	 * @param y
	 *            the y value.
	 */
	void put(double x, double y);

	/**
	 * Removes the specified data point from this data set.
	 * 
	 * @param datum
	 *            the data point.
	 */
	public void remove(Datum datum);

	/**
	 * Removes all data points at x value from this data set.
	 * 
	 * @param x
	 *            the x-value to remove.
	 */
	public void remove(final double x);
}
