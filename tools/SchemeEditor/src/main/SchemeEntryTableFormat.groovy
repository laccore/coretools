import javax.swing.JOptionPane

import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.gui.AdvancedTableFormat
import ca.odell.glazedlists.gui.WritableTableFormat


class SchemeEntryTableFormat implements WritableTableFormat, AdvancedTableFormat {
	def colnames = ['name', 'code']
	def entries = []
	def stifleCodeEditWarning = false
	public SchemeEntryTableFormat(entries) { this.entries = entries }

    // extraProps: list of Strings of property names other than the standard 'name'
    // and 'code' to include in entry table so those props can be edited.
    // Added to allow editing of 'width' property for grainsize schemes.
    public SchemeEntryTableFormat(entries, extraProps) {
        this.entries = entries
        extraProps.each { colnames << it }
    }


	public int getColumnCount() { return colnames.size() }
	public Object getColumnValue(Object baseObject, int col) {
		def propName = colnames[col]
		return baseObject."$propName"
	}
	public String getColumnName(int col) { return colnames[col].substring(0,1).toUpperCase() + colnames[col].substring(1) }
	
	public boolean isEditable(Object obj, int col) { return col >= 0 }
	public Object setColumnValue(Object obj, Object newValue, int column) {
		if (column == 0) {
			obj.name = newValue
			if (!obj.code) {
				obj.code = SchemeHelper.createUniqueCode(obj.name, entries)
			}
			return obj
		} else if (column == 1) {
			def dupElt = this.entries.find { it.code?.equals(newValue) && !it.equals(obj) }
			if (dupElt) {
				JOptionPane.showMessageDialog(null, "The code '$newValue' is already used by entry '${dupElt.name}'.",
										"Duplicate Code", JOptionPane.ERROR_MESSAGE)
				return null
			}
			
			if (!stifleCodeEditWarning) {
				def msg = "If this scheme entry is used in an existing PSICAT project, changing its code will break\nthe project's association with the entry, requiring manual correction of the project files.\nDo you want to continue?"
				def choice = JOptionPane.showOptionDialog(null, msg, "Modify Existing Code?", JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE, null, ["No", "Yes", "Yes, Stop Asking Me!"] as String[], "No")
				if (choice == 0) // No
					return null
				else if (choice == 2) // "stop asking me!"
					stifleCodeEditWarning = true
			}
			obj.code = newValue
			return obj
		} else if (column >= 2) {
            obj."${colnames[column]}" = newValue
            return obj
        }
		return null
	}
	
	// AdvancedTableFormat methods - make column sorting case-insensitive
	public Class getColumnClass(int column) { return Object.class }
	public Comparator getColumnComparator(int column) { return GlazedLists.caseInsensitiveComparator() }	
}