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

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The default implementation of the {@link ModelFormatManager} interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@Singleton
public class DefaultModelFormatManager implements ModelFormatManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelFormatManager.class);
	private final Set<ModelReader> readers = new HashSet<ModelReader>();
	private final Set<ModelWriter> writers = new HashSet<ModelWriter>();

	/**
	 * Create a new DefaultModelFormatManager.
	 */
	DefaultModelFormatManager() {
		LOGGER.debug("initialized");
	}

	/**
	 * {@inheritDoc}
	 */
	public ModelReader getReader(final String format) {
		for (ModelReader r : getReaders()) {
			if (format.equalsIgnoreCase(r.getFormat())) {
				return r;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableSet<ModelReader> getReaders() {
		return ImmutableSet.copyOf(readers);
	}

	/**
	 * {@inheritDoc}
	 */
	public ModelWriter getWriter(final String format) {
		for (ModelWriter w : getWriters()) {
			if (format.equalsIgnoreCase(w.getFormat())) {
				return w;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableSet<ModelWriter> getWriters() {
		return ImmutableSet.copyOf(writers);
	}

	@Inject(optional = true)
	void injectReaders(final Set<ModelReader> injected) {
		readers.addAll(injected);
	}

	@Inject(optional = true)
	void injectWriters(final Set<ModelWriter> injected) {
		writers.addAll(injected);
	}

	/**
	 * {@inheritDoc}
	 */
	public void registerReader(final ModelReader reader) {
		readers.add(reader);
	}

	/**
	 * {@inheritDoc}
	 */
	public void registerWriter(final ModelWriter writer) {
		writers.add(writer);
	}

	/**
	 * {@inheritDoc}
	 */
	public void unregisterReader(final ModelReader reader) {
		readers.remove(reader);
	}

	/**
	 * {@inheritDoc}
	 */
	public void unregisterWriter(final ModelWriter writer) {
		writers.remove(writer);
	}
}
