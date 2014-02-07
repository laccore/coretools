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

import java.util.Map;

import com.google.inject.ImplementedBy;

/**
 * Manages factories to constructs {@link Model}s from a type and a map of data.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@ImplementedBy(DefaultModelManager.class)
public interface ModelManager {
	/**
	 * Constructs {@link Model}s of specific types.
	 */
	interface Factory {
		/**
		 * Builds the model for the specified type and key-value map.
		 * 
		 * @param type
		 *            the model type.
		 * @param data
		 *            the model data.
		 * @return a model or null.
		 */
		Model build(String type, Map<String, String> data);

		/**
		 * Gets the model types this factory can construct.
		 * 
		 * @return the array of model types.
		 */
		String[] getTypes();
	}

	/**
	 * Builds a Model from the specified type and data. This queries all configured {@link ModelFactory}s.
	 * 
	 * @param type
	 *            the model type.
	 * @param data
	 *            the model data.
	 * @return the model or null if none found.
	 */
	Model build(String type, Map<String, String> data);

	/**
	 * Registers a model factory with this manager.
	 * 
	 * @param factory
	 *            the model factory.
	 */
	void register(Factory factory);

	/**
	 * Unregisters a model factory with this manager.
	 * 
	 * @param factory
	 *            the factory to unregister.
	 */
	void unregister(Factory factory);
}
