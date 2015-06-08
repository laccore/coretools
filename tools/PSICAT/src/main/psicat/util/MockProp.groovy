package psicat.util;

import org.andrill.coretools.model.edit.Command
import org.andrill.coretools.model.edit.EditableProperty
import org.andrill.coretools.ui.widget.Widget

// brg 12/17/2014: Mock implementation of EditableProperty, needed in order to
// create SchemeEntryWidgets using SwingWidgetSet.getWidget(EditableProperty, boolean)
// for use in find and replace.
class MockProp implements EditableProperty {
	def source
	String name
	String widgetType = Widget.SCHEME_ENTRY_TYPE
	Map<String,String> widgetProperties = [:]
	Map<String,String> constraints = [:]
	def validators = []
	Command command = null

	Command getCommand(String foo) { return command }
	String getValue() {	"None" }
	boolean isValid(String newValue) { return true }
}
