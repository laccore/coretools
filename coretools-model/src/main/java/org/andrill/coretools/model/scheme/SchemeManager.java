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

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.inject.ImplementedBy;

/**
 * Defines the SchemeManager interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@ImplementedBy(DefaultSchemeManager.class)
public interface SchemeManager {

	/**
	 * Creates schemes.
	 */
	interface Factory {

		/**
		 * Gets the set of schemes.
		 * 
		 * @return the schemes.
		 */
		Set<Scheme> getSchemes();
	}

	/**
	 * Gets a scheme entry by scheme id and code.
	 * 
	 * @param id
	 *            the scheme id.
	 * @param code
	 *            the entry code.
	 * @return the entry or null.
	 */
	SchemeEntry getEntry(String id, String code);

	/**
	 * Gets a scheme by id.
	 * 
	 * @param id
	 *            the scheme id.
	 * @return the scheme or null if not found.
	 */
	Scheme getScheme(String id);

	/**
	 * Gets all schemes.
	 * 
	 * @return the set of schemes.
	 */
	ImmutableSet<Scheme> getSchemes();

	/**
	 * Gets schemes by type.
	 * 
	 * @param type
	 *            the type.
	 * @return the set of schemes.
	 */
	ImmutableSet<Scheme> getSchemes(String type);

	/**
	 * Register a scheme.
	 * 
	 * @param scheme
	 *            the scheme.
	 */
	void registerScheme(Scheme scheme);

	/**
	 * Unregisters a scheme.
	 * 
	 * @param scheme
	 *            the scheme.
	 */
	void unregisterScheme(Scheme scheme);
}
