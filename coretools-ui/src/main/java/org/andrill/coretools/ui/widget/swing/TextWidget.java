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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.andrill.coretools.model.edit.EditableProperty;
import org.andrill.coretools.ui.widget.AbstractWidget;

/**
 * A widget for editing text.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class TextWidget extends AbstractWidget implements FocusListener, ActionListener, DocumentListener {
	protected JTextComponent component = null;

	/**
	 * Create a new TextWidget.
	 * 
	 * @param p
	 *            the editable property.
	 * @param readOnly
	 *            the read only flag.
	 */
	public TextWidget(final EditableProperty p, final boolean readOnly) {
		super(p, readOnly);
	}

	public void actionPerformed(final ActionEvent e) {
		fireChange();
	}

	public void focusGained(final FocusEvent e) {
		// ignored
	}

	public void focusLost(final FocusEvent e) {
		fireChange();
	}
	
	public void insertUpdate(DocumentEvent e) {
		fireChange();
	}
	
	public void removeUpdate(DocumentEvent e) {
		fireChange();
	}
	
	public void changedUpdate(DocumentEvent e) {
		fireChange();
	}

	@Override
	protected JTextComponent getEditableUI() {
		if (component == null) {
			if (TEXTAREA_TYPE.equalsIgnoreCase(property.getWidgetType())) {
				JTextArea widget = new JTextArea(property.getValue(), 3, 40);
				widget.getDocument().addDocumentListener(this);
				widget.setLineWrap(true);
				widget.setWrapStyleWord(true);
				component = widget;
			} else {
				JTextField widget = new JTextField(property.getValue(), 10);
				widget.addActionListener(this);
				
				// on any change, verify JTextField contents to provide feedback 
				widget.getDocument().addDocumentListener(new DocumentListener() {
					private void update(DocumentEvent e) {
						component.getInputVerifier().verify(component);
					}
					public void insertUpdate(DocumentEvent e) { update(e); }
					public void removeUpdate(DocumentEvent e) { update(e); }
					public void changedUpdate(DocumentEvent e) { update(e); }
				});
				
				component = widget;
			}
			component.setBorder(BorderFactory.createEtchedBorder());
			component.setInputVerifier(new PropertyInputVerifier(property));
			component.addFocusListener(this);
		}
		return component;
	}

	@Override
	protected Object getReadOnlyUI() {
		if (TEXTAREA_TYPE.equalsIgnoreCase(property.getWidgetType())) {
			JTextArea widget = new JTextArea(property.getValue(), 1, 40);
			widget.setLineWrap(true);
			widget.setWrapStyleWord(true);
			widget.setEditable(false);
			widget.setBackground(new JLabel().getBackground());
			widget.setBorder(null);
			return widget;
		} else {
			return new JLabel(property.getValue());
		}
	}

	@Override
	protected String getWidgetValue() {
		String value = (component == null) ? null : component.getText();
		if ((value != null) && "".equals(value.trim())) {
			value = null;
		}
		return value;
	}

	/**
	 * {@inheritDoc}
	 */
	// brg 11/21/2023: AFAICT this is never used, the widget is always rebuilt
	// from scratch and populated in getEditableUI().
	public void setValue(final String value) {
		if (component != null) {
			component.setText(value);
		}
	}
}
