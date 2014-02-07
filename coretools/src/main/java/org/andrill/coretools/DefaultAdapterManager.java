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
package org.andrill.coretools;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.inject.Singleton;

/**
 * The default {@link AdapterManager} implementation.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@Singleton
public class DefaultAdapterManager implements AdapterManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAdapterManager.class);
	protected final SetMultimap<Class<?>, Factory> factories;

	/**
	 * Create a new DefaultAdapterManager.
	 */
	public DefaultAdapterManager() {
		factories = LinkedHashMultimap.create();
		LOGGER.debug("initialized");
	}

	protected <E> E adapt(final Object object, final Class<?> from, final Class<E> to) {
		// bail if null
		if ((object == null) || (from == null) || (to == null)) {
			return null;
		}

		// check the factories
		for (Factory f : factories.get(from)) {
			if (Arrays.asList(f.getAdapterTypes()).contains(to)) {
				final Object adapter = f.getAdapter(object, to);
				if (adapter != null) {
					return to.cast(adapter);
				}
			}
		}
		return adapt(object, from.getSuperclass(), to);
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> E getAdapter(final Object adaptableObject, final Class<E> adapterType) {
		return adapt(adaptableObject, adaptableObject.getClass(), adapterType);
	}

	/**
	 * {@inheritDoc}
	 */
	public void register(final Factory factory, final Class<?> adaptable) {
		factories.put(adaptable, factory);
		LOGGER.debug("AdapterFactory {} registered for {}", factory, adaptable);
	}

	/**
	 * {@inheritDoc}
	 */
	public void unregister(final Factory factory, final Class<?> adaptable) {
		factories.remove(adaptable, factory);
		LOGGER.debug("AdapterFactory {} unregistered for {}", factory, adaptable);
	}
}
