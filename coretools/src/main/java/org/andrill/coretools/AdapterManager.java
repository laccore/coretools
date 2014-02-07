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

import com.google.inject.ImplementedBy;

/**
 * Defines the AdapterManager interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@ImplementedBy(DefaultAdapterManager.class)
public interface AdapterManager {

	/**
	 * An adapter factory.
	 */
	interface Factory {
		/**
		 * Returns an object which is an instance of the given class associated with the given object.
		 * 
		 * @param adaptableObject
		 *            the object to adapt.
		 * @param adapterType
		 *            the desired adapter.
		 * @return an object which is an instance of the given class associated with the given object.
		 */
		<E> E getAdapter(Object adaptableObject, Class<E> adapterType);

		/**
		 * Gets the list of adapter types this factory can handle.
		 * 
		 * @return the list of adapter types.
		 */
		Class<?>[] getAdapterTypes();
	}

	/**
	 * Returns an object which is an instance of the given class associated with the given object.
	 * 
	 * @param adaptableObject
	 *            the object to adapt.
	 * @param adapterType
	 *            the desired adapter.
	 * @return an object which is an instance of the given class associated with the given object.
	 */
	<E> E getAdapter(Object adaptableObject, Class<E> adapterType);

	/**
	 * Register an adapter factory.
	 * 
	 * @param factory
	 *            the adapter factory.
	 * @param adaptable
	 *            the adaptable class.
	 */
	void register(Factory factory, Class<?> adaptable);

	/**
	 * Unregister an adapter factory.
	 * 
	 * @param factory
	 *            the adapter factory.
	 * @param adaptable
	 *            the adaptable class.
	 */
	void unregister(Factory factory, Class<?> adaptable);
}
