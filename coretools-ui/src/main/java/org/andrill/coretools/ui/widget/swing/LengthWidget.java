package org.andrill.coretools.ui.widget.swing;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.andrill.coretools.model.edit.EditableProperty;

// LengthWidget is a text field that, when set, omits the units of a Length e.g.
// "3.1415 m" becomes "3.1415", and restores the units in getWidgetValue(). All
// values are assumed to be in project units, which will be displayed as a label
// alongside the text field.

public class LengthWidget extends TextWidget {
    public LengthWidget(final EditableProperty p, final boolean readOnly) {
        super(p, readOnly);
    }

	@Override
	protected String getWidgetValue() {
		String value = (component == null) ? null : component.getText();

        // The underlying property is a true Length, including units.
        // Apply those units to the widget value here so a proper Length
        // is returned (as a String).
        String propertyUnits = property.getValue().split(" ")[1];
        value = value + " " + propertyUnits;

		if ((value != null) && "".equals(value.trim())) {
			value = null;
		}
		return value;
	}

    @Override
	protected JTextComponent getEditableUI() {
		if (component == null) {
            JTextField widget = new JTextField(property.getValue(), 7);

            // omit units portion of Length string
            final int spaceIdx = property.getValue().indexOf(' ');
            String value = (spaceIdx != -1) ? property.getValue().substring(0, spaceIdx) : property.getValue();
            widget.setText(value);

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

			component.setBorder(BorderFactory.createEtchedBorder());
			component.setInputVerifier(new PropertyInputVerifier(property));
			component.addFocusListener(this);
		}
		return component;
	}

    @Override
    public boolean useProjectUnits() { return true; }
}
