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

import org.andrill.coretools.Adaptable;

/**
 * Represents a domain object.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Model extends Adaptable {

	/**
	 * Gets the container this model belongs to.
	 * 
	 * @return the container.
	 */
	ModelContainer getContainer();

	/**
	 * Gets the model data.
	 * 
	 * @return the model data.
	 */
	Map<String, String> getModelData();

	/**
	 * Gets the model type.
	 * 
	 * @return the model type.
	 */
	String getModelType();

	/**
	 * Sets the container this model belongs to.
	 * 
	 * @param container
	 *            the container.
	 */
	void setContainer(ModelContainer container);
}
