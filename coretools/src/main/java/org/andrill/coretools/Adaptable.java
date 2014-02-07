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

/**
 * Provides a mechanism for adapting an object of one type into another.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Adaptable {

	/**
	 * Returns an object which is an instance of the given class associated with this object. Returns null if no such
	 * object can be found.
	 * 
	 * @param adapter
	 *            the adapter class.
	 * @return a object castable to the given class, or null if this object does not have an adapter for the given
	 *         class.
	 */
	<E> E getAdapter(Class<E> adapter);
}
