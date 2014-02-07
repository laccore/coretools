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

import java.util.Map;

import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelManager;

/**
 * A factory for DataFiles.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DataFileFactory implements ModelManager.Factory {
	private static final String[] TYPES = { "CSVDataFile" };

	/**
	 * {@inheritDoc}
	 */
	public Model build(final String type, final Map<String, String> data) {
		String path = data.get("path");
		String separator = data.get("separator");
		String quote = data.get("quote");
		int skipLines = 0;
		if (data.containsKey("skipLines")) {
			Integer.parseInt(data.get("skipLines"));
		}
		int column = 0;
		if (data.containsKey("column")) {
			Integer.parseInt(data.get("column"));
		}
		return new CSVDataFile(path, separator, quote, skipLines, column);
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] getTypes() {
		return TYPES;
	}
}
