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

import com.google.common.collect.ImmutableSet;

/**
 * Defines the interface for a scheme.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Scheme {

	/**
	 * Get all entries in the scheme.
	 * 
	 * @return list of entries.
	 */
	ImmutableSet<SchemeEntry> getEntries();

	/**
	 * Gets the matching entry in the scheme for the specified code.
	 * 
	 * @param code
	 *            the code.
	 * @return the matching entry or null.
	 */
	SchemeEntry getEntry(String code);

	/**
	 * Gets the id of this scheme.
	 * 
	 * @return the id.
	 */
	String getId();

	/**
	 * Gets the name of this scheme.
	 * 
	 * @return the name.
	 */
	String getName();

	/**
	 * Gets the type of this scheme.
	 * 
	 * @return the type.
	 */
	String getType();
}