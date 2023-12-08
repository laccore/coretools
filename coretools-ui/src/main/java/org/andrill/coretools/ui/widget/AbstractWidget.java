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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.andrill.coretools.model.edit.EditableProperty;

/**
 * An abstract implementation of the Widget interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class AbstractWidget implements Widget {
	private static final String LABEL_KEY = "label";
	private static final String UNIT_LABEL_KEY = "unitLabel"; // label right of edit text, useful for e.g. fixed units

	protected Set<Listener> listeners = new CopyOnWriteArraySet<Listener>();
	protected EditableProperty property;
	protected boolean readOnly;
	protected String label = null;
	protected String unitLabel = null;

	/**
	 * Create a new {@link AbstractWidget}.
	 * 
	 * @param property
	 *            the property.
	 */
	public AbstractWidget(final EditableProperty property, final boolean readOnly) {
		this.property = property;
		this.readOnly = readOnly;
	}

	/**
	 * {@inheritDoc}
	 */
	public void addListener(final Listener l) {
		listeners.add(l);
	}

	protected void fireChange() {
		String pv = property.getValue();
		String wv = getWidgetValue();
		if (((pv == null) && (wv != null)) || ((pv != null) && (wv == null))
		        || ((pv != null) && (wv != null) && !pv.equals(wv))) {
			for (Listener l : listeners) {
				l.widgetChanged(this);
			}
		}
	}

	protected abstract Object getEditableUI();

	/**
	 * {@inheritDoc}
	 */
	public String getLabel() {
		if (label == null) {
			if (property.getWidgetProperties().containsKey(LABEL_KEY)) {
				label = property.getWidgetProperties().get(LABEL_KEY);
			} else {
				StringBuilder b = new StringBuilder();
				String name = property.getName();
				for (int i = 0; i < name.length(); i++) {
					char c = name.charAt(i);
					if (c == Character.toUpperCase(c)) {
						b.append(' ');
						b.append(c);
					} else if (c == '_') {
						b.append(' ');
					} else {
						b.append(c);
					}
				}
				b.setCharAt(0, Character.toUpperCase(b.charAt(0)));
				return b.toString();
			}
		}
		return label;
	}
	
	public String getUnitLabel() {
		if (property.getWidgetProperties().containsKey(UNIT_LABEL_KEY)) {
			unitLabel = property.getWidgetProperties().get(UNIT_LABEL_KEY);
		}
		return unitLabel;
	}

	public boolean useProjectUnits() { return false; }

	/**
	 * {@inheritDoc}
	 */
	public EditableProperty getProperty() {
		return property;
	}

	protected abstract Object getReadOnlyUI();

	/**
	 * {@inheritDoc}
	 */
	public Object getUI() {
		return readOnly ? getReadOnlyUI() : getEditableUI();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getValue() {
		return readOnly ? property.getValue() : getWidgetValue();
	}

	protected abstract String getWidgetValue();

	/**
	 * {@inheritDoc}
	 */
	public void removeListener(final Listener l) {
		listeners.remove(l);
	}
}
