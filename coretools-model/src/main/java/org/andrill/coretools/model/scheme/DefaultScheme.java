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
package org.andrill.coretools.model.scheme;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * Default implementation of the {@link Scheme} interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DefaultScheme implements EditableScheme {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheme.class);
	protected final String id;
	protected final String name;
	protected final String type;
	protected final Map<String, SchemeEntry> entries;

	/**
	 * Create a new DefaultScheme.
	 * 
	 * @param id
	 *            the id.
	 * @param name
	 *            the name.
	 * @param type
	 *            the type.
	 */
	public DefaultScheme(final String id, final String name, final String type) {
		this.id = id;
		this.name = name;
		this.type = type;
		entries = new LinkedHashMap<String, SchemeEntry>();
	}

	/**
	 * {@inheritDoc}
	 */
	public void addEntry(final SchemeEntry entry) {
		String code = entry.getCode();
		entries.put(code, entry);
		entry.setScheme(this);
		LOGGER.debug("Registered scheme entry {} in scheme {}", code, id);
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableSet<SchemeEntry> getEntries() {
		return ImmutableSet.copyOf(entries.values());
	}

	/**
	 * {@inheritDoc}
	 */
	public SchemeEntry getEntry(final String code) {
		return entries.get(code);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId() {
		return id;
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
	public String getType() {
		return type;
	}
}
