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
package org.andrill.coretools.ui.widget.swing;

import org.andrill.coretools.model.edit.EditableProperty;
import org.andrill.coretools.model.scheme.SchemeManager;
import org.andrill.coretools.ui.widget.Widget;
import org.andrill.coretools.ui.widget.WidgetSet;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A widget set for Java Swing applications.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@Singleton
public class SwingWidgetSet implements WidgetSet {
	private final SchemeManager schemes;
	
	@Inject
	SwingWidgetSet(SchemeManager schemes) {
		this.schemes = schemes;
	}

	/**
	 * {@inheritDoc}
	 */
	public Widget getWidget(final EditableProperty p, final boolean readOnly) {
		boolean ro = readOnly || "false".equals(p.getConstraints().get("editable"));
		if (Widget.SCHEME_ENTRY_TYPE.equals(p.getWidgetType())) {
			return new SchemeEntryWidget(p, ro, schemes);
		} else {
			return new TextWidget(p, ro);
		}
	}
}
