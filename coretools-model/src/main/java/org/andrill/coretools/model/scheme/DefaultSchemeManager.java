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

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implements the {@link SchemeManager} interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@Singleton
public class DefaultSchemeManager implements SchemeManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSchemeManager.class);
	private final Set<Scheme> schemes = new HashSet<Scheme>();

	/**
	 * Create a new DefaultSchemeManager.
	 */
	DefaultSchemeManager() {
		LOGGER.debug("initialized");
	}

	/**
	 * {@inheritDoc}
	 */
	public SchemeEntry getEntry(final String id, final String code) {
		Scheme scheme = getScheme(id);
		if (scheme == null) {
			return null;
		} else {
			return scheme.getEntry(code);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Scheme getScheme(final String id) {
		if (id != null) {
			for (Scheme s : getSchemes()) {
				if (id.equals(s.getId())) {
					return s;
				}
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableSet<Scheme> getSchemes() {
		return ImmutableSet.copyOf(schemes);
	}

	/**
	 * {@inheritDoc}
	 */
	public ImmutableSet<Scheme> getSchemes(final String type) {
		Set<Scheme> set = new HashSet<Scheme>();
		for (Scheme s : getSchemes()) {
			if (type.equals(s.getType())) {
				set.add(s);
			}
		}
		return ImmutableSet.copyOf(set);
	}

	@Inject(optional = true)
	void injectFactories(final Set<Factory> factories) {
		for (Factory set : factories) {
			schemes.addAll(set.getSchemes());
		}
	}

	@Inject(optional = true)
	void injectSchemes(final Set<Scheme> injected) {
		schemes.addAll(injected);
	}

	/**
	 * {@inheritDoc}
	 */
	public void registerScheme(final Scheme scheme) {
		schemes.add(scheme);
	}

	/**
	 * {@inheritDoc}
	 */
	public void unregisterScheme(final Scheme scheme) {
		schemes.remove(scheme);
	}
}
