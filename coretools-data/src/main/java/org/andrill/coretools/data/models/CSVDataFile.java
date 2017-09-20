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
package org.andrill.coretools.data.models;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.andrill.coretools.data.DataSet;
import org.andrill.coretools.data.DataSource;
import org.andrill.coretools.data.DefaultDataSet;
import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

public class CSVDataFile implements Model, DataSource {
	private static Logger LOGGER = LoggerFactory.getLogger(CSVDataFile.class);
	protected ModelContainer container = null;
	protected Map<Integer, DataSet> datasets = null;
	protected String path;
	protected char separator;
	protected char quote;
	protected int skipLines;
	protected int column; // index of the 'x' column, typically depth

	public CSVDataFile(final String path, final String separator, final String quote, final int skipLines,
	        final int column) {
		this.path = path;
		this.separator = (separator == null ? CSVReader.DEFAULT_SEPARATOR : separator.charAt(0));
		this.quote = (quote == null ? CSVReader.DEFAULT_QUOTE_CHARACTER : quote.charAt(0));
		this.skipLines = (skipLines < 0 ? 0 : skipLines);
		this.column = (column < 0 ? 0 : column);
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> E getAdapter(final Class<E> adapter) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ModelContainer getContainer() {
		return container;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<DataSet> getDatasets() {
		if (datasets == null) {
			datasets = new HashMap<Integer, DataSet>();
			try {
				parse(new CSVReader(new InputStreamReader(new URL(path).openStream()), separator, quote, skipLines));
			} catch (MalformedURLException e) {
				LOGGER.error("Unable to parse csv data file", e);
			} catch (IOException e) {
				LOGGER.error("Unable to parse csv data file", e);
			}
		}
		return new ArrayList<DataSet>(datasets.values());
	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, String> getModelData() {
		Map<String, String> data = new HashMap<String, String>();
		data.put("path", path);
		data.put("separator", "" + separator);
		data.put("quote", "" + quote);
		data.put("skipLines", "" + skipLines);
		data.put("column", "" + column);
		return data;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getModelType() {
		return getClass().getSimpleName();
	}

	protected void parse(final CSVReader csv) throws IOException {
		// parse headers
		String[] headers = csv.readNext();
		for (int i = 0; i < headers.length; i++) {
			if (i != column) {
				datasets.put(i, new DefaultDataSet(headers[i]));
			}
		}

		// parse data
		String[] row = null;
		while ((row = csv.readNext()) != null) {
			try {
				double x = Double.parseDouble(row[column]);
				for (int i = 0; i < row.length; i++) {
					if (i != column) {
						try {
							datasets.get(i).put(x, Double.parseDouble(row[i]));
						} catch (NumberFormatException nfe2) {
							// skip just this value
						}
					}
				}
			} catch (NumberFormatException nfe1) {
				// ignore whole row
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContainer(final ModelContainer container) {
		this.container = container;
	}
}
