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
package org.andrill.coretools.model.edit;

import java.util.Map;

/**
 * Describes an editable property.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface EditableProperty {

	/**
	 * Gets the command to change the property to the specified value.
	 * 
	 * @param value
	 *            the value.
	 * @return the command.
	 */
	Command getCommand(String value);

	/**
	 * Gets the constraints for this property.
	 * 
	 * @return the constraints.
	 */
	Map<String, String> getConstraints();

	/**
	 * Gets the name of this property.
	 * 
	 * @return the name.
	 */
	String getName();

	/**
	 * Gets the value of this property.
	 * 
	 * @return the value.
	 */
	String getValue();

	/**
	 * Gets the properties to configure the widget.
	 * 
	 * @return the widget properties.
	 */
	Map<String, String> getWidgetProperties();

	/**
	 * Gets the type of widget used to use to edit this property in the GUI.
	 * 
	 * @return the widget type.
	 */
	String getWidgetType();

	/**
	 * Validate the specified value.
	 * 
	 * @param value
	 *            the value.
	 * @return null if the value is valid, an error message if the value is not valid.
	 */
	boolean isValid(String value);
}
