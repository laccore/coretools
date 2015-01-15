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
package org.andrill.coretools.ui;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.andrill.coretools.Adaptable;
import org.andrill.coretools.Platform;
import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.edit.Command;
import org.andrill.coretools.model.edit.CommandStack;
import org.andrill.coretools.model.edit.EditableProperty;
import org.andrill.coretools.scene.Scene;
import org.andrill.coretools.scene.Selection;
import org.andrill.coretools.ui.widget.Widget;
import org.andrill.coretools.ui.widget.WidgetSet;

/**
 * A panel for displaying editable properties.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class PropertiesPanel extends JPanel implements Scene.SelectionListener, ModelContainer.Listener,
        Widget.Listener {
	private static final long serialVersionUID = 1L;
	protected Selection selection = Selection.EMPTY;
	protected EditableProperty[] properties = null;
	protected JLabel label;
	protected Map<Widget, JComponent> components = new HashMap<Widget, JComponent>();
	protected WidgetSet widgets;
	protected Scene scene = null;
	protected boolean fromWidget = false;
	private Font normal = new Font("SanSerif", Font.PLAIN, 12);
	private Font title = new Font("SanSerif", Font.BOLD, 14);

	/**
	 * Create a new PropertiesPanel
	 */
	public PropertiesPanel() {
		this(null);
	}

	/**
	 * Create a new PropertiesPanel for the specified scene.
	 * 
	 * @param scene
	 *            the scene.
	 */
	public PropertiesPanel(final Scene scene) {
		label = new JLabel("No properties");
		label.setFont(normal);
		widgets = Platform.getService(WidgetSet.class);
		setLayout(new MigLayout());
		add(label);
		setScene(scene);
	}

	/**
	 * Responsible for building the UI based on the editable properties of the selection.
	 */
	protected void buildUI() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				clearComponents();
				if ((properties == null) || (properties.length == 0)) {
					label.setFont(normal);
					label.setText("No properties");
					add(label);
				} else {
					label.setFont(title);
					label.setText(selection.getFirstObject().toString());
					add(label, "span, wrap");
					boolean readOnly = isReadOnly();
					for (int i = 0; i < properties.length; i++) {
						EditableProperty p = properties[i];
					
						// get our widget
						Widget w = widgets.getWidget(p, readOnly);
						if (!readOnly) {
							w.addListener(PropertiesPanel.this);
						}
						
						// build a label for the widget 
						JLabel label = new JLabel(w.getLabel() + ":");
						add(label, "label");
						
						String group = p.getConstraints().get("group");
						if (group == null) {
							// add our widget component
							JComponent component = (JComponent) w.getUI();
							components.put(w, component);
							add(component, "wrap");	
						} else {
							Widget current = w;
							boolean lastInGroup = (i + 1 >= properties.length || !group.equals(properties[i+1].getConstraints().get("group")));
							while (!lastInGroup) {
								if (current != w) {
									String labelText = current.getLabel();
									if (labelText != null && !"".equals(labelText)) {
										add(new JLabel(current.getLabel()), "split");
									}
								}
								
								// add the component w/ split
								JComponent component = (JComponent) current.getUI();
								components.put(current, component);
								
								if (current.getUnitLabel() != null) {
									add(component, "split");
									add(new JLabel(current.getUnitLabel()), "split");
								} else {
									add(component, "split");
								}
								
								// get the next one
								i++;
								current = widgets.getWidget(properties[i], readOnly);
								if (!readOnly) {
									current.addListener(PropertiesPanel.this);
								}
								lastInGroup = (i + 1 >= properties.length || !group.equals(properties[i+1].getConstraints().get("group")));
							}
							
							if (current != w) {
								String labelText = current.getLabel();
								if (labelText != null && !"".equals(labelText)) {
									add(new JLabel(current.getLabel()), "split");
								}
							}
							
							// add the component w/ wrap
							JComponent component = (JComponent) current.getUI();
							components.put(current, component);
							
							if (current.getUnitLabel() != null) {
								add(component, "split");
								add(new JLabel(current.getUnitLabel()), "wrap");
							} else {
								add(component, "wrap");
							}
						}
					}
				}
				revalidate();
				repaint();
			}
		});
	}

	protected void clearComponents() {
		for (Widget w : components.keySet()) {
			w.removeListener(this);
		}
		components.clear();
		removeAll();
	}

	/**
	 * Gets the command stack.
	 * 
	 * @return the command stack.
	 */
	protected CommandStack getCommandStack() {
		return scene.getCommandStack();
	}

	/**
	 * Gets the scene this PropertiesPanel is watching.
	 * 
	 * @return the scene.
	 */
	Scene getScene() {
		return scene;
	}

	protected boolean isReadOnly() {
		CommandStack edit = getCommandStack();
		return ((edit == null) || !edit.canExecute());
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelAdded(final Model model) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelRemoved(final Model model) {
	}

	/**
	 * {@inheritDoc}
	 */
	public void modelUpdated(final Model model) {
		if ((selection.getFirstObject() == model) && !fromWidget) {
			buildUI();
		}
		fromWidget = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(final Selection selection) {
		setSelection(selection);
	}

	/**
	 * Sets the scene this PropertiesPanel is watching.
	 * 
	 * @param scene
	 *            the scene.
	 */
	public void setScene(final Scene scene) {
		if (this.scene != null) {
			this.scene.removeSelectionListener(this);
			this.scene.getModels().removeListener(this);
		}
		this.scene = scene;
		if (this.scene != null) {
			this.scene.addSelectionListener(this);
			this.scene.getModels().addListener(this);
		}
		setSelection(scene == null ? null : scene.getSelection());
	}

	/**
	 * Sets the selection of this properties panel.
	 * 
	 * @param selection
	 *            the selection.
	 */
	public void setSelection(final Selection selection) {
		Selection old = this.selection;
		this.selection = (selection == null) ? Selection.EMPTY : selection;
		if (!old.equals(selection)) {
			Object first = ((selection != null) && !selection.isEmpty()) ? selection.getFirstObject() : null;
			if ((first == null) || !(first instanceof Adaptable)) {
				properties = null;
			} else {
				properties = ((Adaptable) selection).getAdapter(EditableProperty[].class);
			}
			buildUI();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void widgetChanged(final Widget widget) {
		if (!isReadOnly()) {
			Command command = widget.getProperty().getCommand(widget.getValue());
			if (command != null) {
				fromWidget = true;
				getCommandStack().execute(command);
			}
		}
	}
}
