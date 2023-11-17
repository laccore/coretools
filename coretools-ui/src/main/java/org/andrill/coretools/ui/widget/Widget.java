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
package org.andrill.coretools.ui.widget;

import org.andrill.coretools.model.edit.EditableProperty;

/**
 * Defines the interface of a Widget.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Widget {
	/**
	 * Defines the interface for a Widget listener.
	 */
	public interface Listener {

		/**
		 * Called when a widget changes.
		 * 
		 * @param widget
		 *            the widget.
		 */
		void widgetChanged(Widget widget);
	}

	String TEXTFIELD_TYPE = "TextField";
	String TEXTAREA_TYPE = "TextArea";
	String SCHEME_ENTRY_TYPE = "SchemeEntry";

	/**
	 * Adds a change listener to this widget.
	 * 
	 * @param l
	 *            the listener.
	 */
	void addListener(Listener l);

	/**
	 * Gets the label for this widget.
	 * 
	 * @return the label.
	 */
	String getLabel();

	/**
	 * Gets the unit label for this widget
	 * 
	 * @return the label.
	 */
	String getUnitLabel();

	/**
	 * Use project units as label?
	 * 
	 * @return boolean
	 */
	boolean useProjectUnits();

	/**
	 * Gets the {@link EditableProperty} for this widget.
	 * 
	 * @return the editable property.
	 */
	EditableProperty getProperty();

	/**
	 * Gets the UI object for this widget.
	 * 
	 * @return the UI object.
	 */
	Object getUI();

	/**
	 * Gets the value of this widget.
	 * 
	 * @return the value.
	 */
	String getValue();

	/**
	 * Removes a change listener from this widget.
	 * 
	 * @param l
	 *            the listener.
	 */
	void removeListener(Listener l);

	/**
	 * Sets the value of this widget.
	 * 
	 * @param value
	 *            the new value.
	 */
	void setValue(String value);
}
