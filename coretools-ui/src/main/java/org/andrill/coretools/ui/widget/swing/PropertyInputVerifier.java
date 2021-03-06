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

import java.awt.Color;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.andrill.coretools.model.edit.EditableProperty;

/**
 * An InputVerifier that is EditableProperty-aware.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class PropertyInputVerifier extends InputVerifier {
	protected final EditableProperty property;

	/**
	 * Create a new PropertyInputVerifier for the specified EditableProperty.
	 * 
	 * @param property
	 *            the property.
	 */
	public PropertyInputVerifier(final EditableProperty property) {
		this.property = property;
	}

	protected String getValue(final JComponent input) {
		String value = null;
		if (input instanceof JTextField) {
			value = ((JTextField) input).getText();
		}
		// TODO handle other components

		// make blanks a null
		if ((value != null) && "".equals(value.trim())) {
			value = null;
		}

		return value;
	}

	@Override
	public boolean verify(final JComponent input) {
		boolean valid = property.isValid(getValue(input));
		input.setBackground((valid ? Color.white : Color.pink));
		return valid;
	}
}
