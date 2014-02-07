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

import java.net.URL;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.google.inject.ImplementedBy;

/**
 * Defines the interface for finding resources.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@ImplementedBy(DefaultResourceLoader.class)
public interface ResourceLoader {

	/**
	 * Add a resource to this resource loader.
	 * 
	 * @param resource
	 *            the resource loader.
	 */
	void addResource(URL resource);

	/**
	 * Find a resource.
	 * 
	 * @param path
	 *            the path to the resource.
	 * 
	 * @return the resource URL or null if not found.
	 */
	URL getResource(String path);

	/**
	 * Find all instances of a resource.
	 * 
	 * @param path
	 *            the path.
	 * @return the list of resource URLs.
	 */
	List<URL> getResources(String path);

	/**
	 * Gets an implementation for the specified service class.
	 * 
	 * @param service
	 *            the service class.
	 * @return an implementation or null if no implementations.
	 */
	<E> E getService(final Class<E> service);

	/**
	 * Gets all implementations for the specified service class.
	 * 
	 * @param service
	 *            the service class.
	 * @return the list of implementations or an empty list if no implementations.
	 */
	<E> ImmutableSet<E> getServices(Class<E> service);
}
