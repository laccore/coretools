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
import org.andrill.coretools.ui.widget.swing.SwingWidgetSet;

import com.google.inject.ImplementedBy;

/**
 * Defines the interface for a WidgetSet.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@ImplementedBy(SwingWidgetSet.class)
public interface WidgetSet {

	/**
	 * Gets the widget for the specified {@link EditableProperty}.
	 * 
	 * @param p
	 *            the property.
	 * @param readOnly
	 *            the flag indicating whether this widget should be read only.
	 * @return the widget or null.
	 */
	public abstract Widget getWidget(EditableProperty p, boolean readOnly);
}