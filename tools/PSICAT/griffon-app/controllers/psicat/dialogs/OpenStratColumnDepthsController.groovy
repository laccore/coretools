package psicat.dialogs

import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel
import javax.swing.JOptionPane

import psicat.util.CustomFileFilter
import psicat.util.Dialogs
import psicat.util.GeoUtils

import psicat.stratcol.*

class SectionMapping extends AbstractTableModel {
	private colmap = [0:'metadataSection', 1:'section', 2:'top', 3:'base']
	private colnames = [0:'Metadata Section', 1:'Project Section', 2:'Top', 3:'Base']
	private secmap
	public SectionMapping(secmap) { this.secmap = secmap }
	public int getColumnCount() { return 2; }
	public int getRowCount() { return secmap.size() }
	public Object getValueAt(int row, int col) { return secmap[row][colmap[col]] }
	public String getColumnName(int col) { return colnames[col] }
}

class OpenStratColumnDepthsController {
	def model
	def view
	
	void mvcGroupInit(Map args) {
		model.project = args.project
		model.metadataPath = args.metadataPath
	}
	
	def show() {
		if (model.metadataPath) {
			parse(model.metadataPath)
		}
	}
	void errbox(message) { Dialogs.showErrorDialog("Strat Column Metadata Error", message, view.openSCMD) }
	def updateStatus(text) { edt { model.statusText = text } }
	
	def parse(filepath) {
		def scMetadata = null
		try {
			scMetadata = StratColumnMetadataFactory.create(filepath)
		} catch (Exception e) {
			errbox("${e.message}")
			return
		}
		if (scMetadata) {
			def sectionMap = null
			try {
				if (scMetadata instanceof SpliceIntervalMetadata) {
					final msg = "<html>Enter the character used to separate section name components.<br><br> \
					Examples:<br> \
					IODP and CSD Facility section names (e.g. GLAD4-HST03-1C-2H-1) use a hyphen, which is the default.<br> \
					ICDP section names (e.g. 5066_1_A_017Q_1) use an underscore.<br><br></html>"
					final delimiter = JOptionPane.showInputDialog(view.root, msg, "-")
					if (delimiter == null) {
						return
					} else {
						scMetadata.setSectionNameDelimiter(delimiter)
					}
				}
				sectionMap = scMetadata.mapSections(model.project)
			} catch (Exception e) {
				errbox("${e.message}")
				e.printStackTrace()
				return
			}

			model.metadataPath = filepath
			model.stratColumnMetadata = scMetadata

			view.sectionMapTable.model = new SectionMapping(sectionMap)
			def mdCount = sectionMap.size()
			def matchCount = sectionMap.findAll { it['section'] != null }.size()
			updateStatus("Found $matchCount matching project sections for $mdCount metadata sections.")
			model.fileTypeText = "${scMetadata.typeName} file detected."
		} else {
			errbox("The selected file is not valid strat column metadata.")
		}
	}
	
	def actions = [
		'browse': { evt = null ->
			def file = StratColumnMetadataUtils.chooseMetadataFile(view.root)
			if (file) {
				parse(file.absolutePath)
			}
		},
		'okAction': { evt = null ->
			model.confirmed = true
			view.openSCMD.setVisible(false)
		},
		'cancelAction': { evt = null ->
			model.confirmed = false
			view.openSCMD.setVisible(false)
		}
	]
}