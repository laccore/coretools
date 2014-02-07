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
package org.andrill.coretools.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The default {@link ModelManager} implementation.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@Singleton
public class DefaultModelManager implements ModelManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelManager.class);
	private final Set<Factory> factories = new HashSet<Factory>();

	/**
	 * Create a new DefaultModelManager.
	 */
	DefaultModelManager() {
		LOGGER.debug("Initialized");
	}

	/**
	 * {@inheritDoc}
	 */
	public Model build(final String type, final Map<String, String> data) {
		for (Factory f : factories) {
			if (Arrays.asList(f.getTypes()).contains(type)) {
				Model m = f.build(type, data);
				if (m != null) {
					return m;
				}
			}
		}
		return null;
	}

	@Inject(optional = true)
	void injectFactories(final Set<Factory> injected) {
		LOGGER.info("Injected factories: {}", injected);
		factories.addAll(injected);
	}

	/**
	 * {@inheritDoc}
	 */
	public void register(final Factory factory) {
		factories.add(factory);
	}

	/**
	 * {@inheritDoc}
	 */
	public void unregister(final Factory factory) {
		factories.remove(factory);
	}
}
