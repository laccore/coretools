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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.*;

import org.andrill.coretools.model.edit.EditableProperty;
import org.andrill.coretools.model.scheme.Scheme;
import org.andrill.coretools.model.scheme.SchemeEntry;
import org.andrill.coretools.model.scheme.SchemeManager;
import org.andrill.coretools.ui.widget.AbstractWidget;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

import net.miginfocom.swing.MigLayout;

/**
 * A widget for selecting {@link SchemeEntry}s.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class SchemeEntryWidget extends AbstractWidget implements ActionListener {
	public class EntryFormat extends Format {
		private static final long serialVersionUID = 1L;

		@Override
		public StringBuffer format(final Object obj, final StringBuffer toAppendTo, final FieldPosition pos) {
			if (obj != null) {
				toAppendTo.append(((SchemeEntry) obj).getName());
			}
			return toAppendTo;
		}

		@Override
		public Object parseObject(final String source, final ParsePosition pos) {
			return combo.getSelectedItem();
		}
	}

	protected static class EntryRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(final JList list, final Object value, final int index,
		        final boolean isSelected, final boolean cellHasFocus) {
			JLabel result = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			SchemeEntry e = (SchemeEntry) value;
			if (e != null) {
				result.setText(e.getName());
			}
			return result;
		}
	}

	private static SchemeEntry NONE = new SchemeEntry(null, "None", null, null);
	protected JPanel panel = null;
	protected JLabel icon = null;
	protected JComboBox combo = null;
	protected EventList<SchemeEntry> entries = GlazedLists.eventList(new ArrayList<SchemeEntry>());
	protected final SchemeManager schemes;
	protected final boolean required;
	private static HashMap<String, RecentSchemeEntries> recentEntries = new HashMap<String, RecentSchemeEntries>();
	private static HashMap<String, JPanel> recentEntryPanels = new HashMap<String, JPanel>();
	private static int RECENT_ENTRIES_CAPACITY = 10;

	/**
	 * Create a new SchemeEntryWidget.
	 * 
	 * @param p
	 *            the
	 * @param readOnly
	 */
	public SchemeEntryWidget(final EditableProperty p, final boolean readOnly, final SchemeManager schemes) {
		super(p, readOnly);
		this.schemes = schemes;
		String nullable = property.getConstraints().get("nullable");
		required = (nullable == null || !"true".equalsIgnoreCase(nullable));
	}

	public void actionPerformed(final ActionEvent e) {
		SchemeEntry selected = (SchemeEntry) combo.getSelectedItem();
		if (selected != null) {
			icon.setIcon(selected.getIcon());
			fireChange();
			addRecentEntry(selected, this.getSchemeType(), combo);
		}
	}

	@Override
	protected Object getEditableUI() {
		if (panel == null) {
			if (!required) {
				entries.add(NONE);
			}
			// get our list of entries
			String type = this.getSchemeType();
			for (Scheme s : schemes.getSchemes()) {
				if ((type == null) || type.equalsIgnoreCase(s.getType())) {
					entries.addAll(s.getEntries());
				}
			}

 			// 7/29/2024: sort Grain Size entries by width ascending. If further per-scheme-type sorting
			// is required, some refactoring is probably in order (e.g. SchemeEntrySortingProvider/Method).
			if (type != null && type.equals("grainsize")) {
				Collections.sort(entries, new Comparator<SchemeEntry>() {
					public int compare(final SchemeEntry o1, final SchemeEntry o2) {
						// The NONE SchemeEntry will not have a width property: default
						// to 999 so it appears at the end of the list.
						final Integer width1 = new Integer(o1.getProperty("width", "999"));
						final Integer width2 = new Integer(o2.getProperty("width", "999"));
						return width1.compareTo(width2);
					}
				});
			} else { // sort alphabetically
				Collections.sort(entries, new Comparator<SchemeEntry>() {
					public int compare(final SchemeEntry o1, final SchemeEntry o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
			}

			// build our icon label
			icon = new JLabel();

			// build our combobox with auto complete support
			combo = new JComboBox(entries.toArray());
			TextFilterator<SchemeEntry> filterator = GlazedLists.textFilterator(new String[] { "name", "code" });
			AutoCompleteSupport<SchemeEntry> autocomplete = AutoCompleteSupport.install(combo, GlazedLists
			        .eventList(entries), filterator, new EntryFormat());
			autocomplete.setStrict(true);
			SchemeEntry e = getEntry(property.getValue());
			if (e == null && required) {
				e = entries.get(0);
			}
			if (e == null) {
				combo.setSelectedItem(NONE);
				icon.setIcon(null);	
			} else {
				combo.setSelectedItem(e);
				icon.setIcon(e.getIcon());
			}
			combo.setRenderer(new EntryRenderer());
			combo.addActionListener(this);
			combo.setBorder(BorderFactory.createEmptyBorder());

			panel = new JPanel();
			panel.setLayout(new MigLayout("insets 0, fillx", "[]", "[][grow,fill]"));
			panel.add(icon, "split 2");
			panel.add(combo, "wrap");
			updateRecentEntryPanel(this.getSchemeType(), combo);
			panel.add(getRecentEntryPanel(this.getSchemeType()), "grow, wmin 400, hmin 60");
		}
		return panel;
	}

	protected SchemeEntry getEntry(final String value) {
		SchemeEntry e = null;
		if (value != null) {
			String[] split = value.split(":");
			if (split.length == 2) {
				Scheme scheme = schemes.getScheme(split[0]);
				if (scheme != null) {
					e = scheme.getEntry(split[1]);
				}
			}
		}
		return e;
	}

	@Override
	protected Object getReadOnlyUI() {
		String value = property.getValue();
		SchemeEntry entry = getEntry(value);
		if (entry == null) {
			return new JLabel("");
		} else {
			return new JLabel(entry.getName(), entry.getIcon(), SwingConstants.LEFT);
		}
	}

	@Override
	protected String getWidgetValue() {
		String value = null;
		if (combo != null) {
			SchemeEntry e = (SchemeEntry) combo.getSelectedItem();
			if (e == null && required) {
				e = entries.get(0);
			}
			if (e != null && e.getScheme() != null) {
				value = e.getScheme().getId() + ":" + e.getCode();
			}
		}
		return value;
	}

	public void setValue(final String value) {
		if (combo != null) {
			SchemeEntry e = getEntry(value);
			if (e == null && required) {
				e = entries.get(0);
			}
			if (e == null) {
				combo.setSelectedIndex(-1);
			} else {
				combo.setSelectedItem(e);
			}
		}
	}

	protected String getSchemeType() {
		return property.getWidgetProperties().get("schemeType");
	}

	// recent scheme entries logic and UI handling
	public static class RecentSchemeEntries {
		private ArrayList<SchemeEntry> entries = null;
		private int capacity;

		public RecentSchemeEntries(int capacity) {
			this.capacity = capacity;
			entries = new ArrayList<SchemeEntry>(capacity);
		}

		// if unique, add entry at index 0, popping last entry if already at capacity
		public void add(SchemeEntry entry) {
			if (this.contains(entry)) {
				return;
			}
			if (entries.size() == capacity) {
				entries.remove(capacity - 1);
			}
			entries.add(0, entry);
		}

		public boolean contains(SchemeEntry entry) { return entries.contains(entry); }
		public ArrayList<SchemeEntry> getEntries() { return entries; }
	}

	public static void clearRecentEntries() {
		recentEntries = new HashMap<String, RecentSchemeEntries>();
		recentEntryPanels = new HashMap<String, JPanel>();
	}

	private static void addRecentEntry(SchemeEntry entry, String schemeType, final JComboBox widgetCombo) {
		if (recentEntries.containsKey(schemeType)) {
			recentEntries.get(schemeType).add(entry);
		} else {
			RecentSchemeEntries recentEntriesForType = new RecentSchemeEntries(RECENT_ENTRIES_CAPACITY);
			recentEntriesForType.add(entry);
			recentEntries.put(schemeType, recentEntriesForType);
		}
		updateRecentEntryPanel(schemeType, widgetCombo);
	}

	private static JPanel getRecentEntryPanel(String schemeType) {
		JPanel rePanel = recentEntryPanels.get(schemeType);
		if (rePanel == null) {
			rePanel = new JPanel();
			rePanel.setLayout(new MigLayout("insets 0 5 0 5, fill, wrap 5"));
			rePanel.setBorder(BorderFactory.createTitledBorder(null, "Recent Selections"));
			rePanel.setMinimumSize(new java.awt.Dimension(200, 50));
			recentEntryPanels.put(schemeType, rePanel);
		}
		return rePanel;
	}

	private static void updateRecentEntryPanel(String schemeType, final JComboBox widgetCombo) {
		JPanel rePanel = getRecentEntryPanel(schemeType);
		rePanel.removeAll();
		if (recentEntries.get(schemeType) != null) {
			for (final SchemeEntry e : recentEntries.get(schemeType).getEntries()) {
				JButton iconButton = new JButton(e.getName(), e.getIcon());
				iconButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent event) {
						widgetCombo.setSelectedItem(e);
					}
				});
				rePanel.add(iconButton);
			}
			rePanel.repaint();
			rePanel.revalidate();
		} else {
			rePanel.add(new JLabel("[Recently selected entries will appear here]"));
		}
	}	
}
