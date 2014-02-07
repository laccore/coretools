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
package org.andrill.coretools.model.io;

import com.google.common.collect.ImmutableSet;
import com.google.inject.ImplementedBy;

/**
 * Manages {@link ModelReader}s and {@link ModelWriter}s.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@ImplementedBy(DefaultModelFormatManager.class)
public interface ModelFormatManager {

	/**
	 * Gets the reader for the specified format.
	 * 
	 * @param format
	 *            the format.
	 * @return the reader or null.
	 */
	ModelReader getReader(String format);

	/**
	 * Gets the set of readers.
	 * 
	 * @return the readers.
	 */
	ImmutableSet<ModelReader> getReaders();

	/**
	 * Gets the writer for the specified format.
	 * 
	 * @param format
	 *            the format.
	 * @return the writer or null.
	 */
	ModelWriter getWriter(String format);

	/**
	 * Gets the set of writers.
	 * 
	 * @return the writers.
	 */
	ImmutableSet<ModelWriter> getWriters();

	/**
	 * Registers a reader.
	 * 
	 * @param reader
	 *            the reader.
	 */
	void registerReader(ModelReader reader);

	/**
	 * Registers a writer.
	 * 
	 * @param writer
	 *            the writer.
	 */
	void registerWriter(ModelWriter writer);

	/**
	 * Unregisters a reader.
	 * 
	 * @param reader
	 *            the reader.
	 */
	void unregisterReader(ModelReader reader);

	/**
	 * Unregisters a writer.
	 * 
	 * @param writer
	 *            the writer.
	 */
	void unregisterWriter(ModelWriter writer);
}
