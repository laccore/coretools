package psicat.ui

import java.awt.*
import java.awt.event.*
import java.util.Vector
import javax.swing.*
import javax.swing.table.*

import net.miginfocom.swing.MigLayout

import org.andrill.coretools.geology.ui.Scale

import psicat.util.Dialogs
import psicat.util.SchemeUtils

public class GrainSizeDialog extends JDialog {
    public GrainSizeDialog(Frame owner, String title, Scale scale) {
        super(owner, title, true); // modal
        loadScale(scale)
        setupUI()
        setLocationRelativeTo(owner)
    }

    private double grainSizeOffset = 0.2
    private Vector<GrainSize> grainSizes = new Vector<GrainSize>()

    private JTextField defaultWidthField
    private JTable gsTable
    private GrainSizeTableModel gsTableModel
    private JButton okButton, cancelButton, loadButton, addButton, deleteButton
    private Font helpFont
    public boolean okPressed = false

    public String getGrainSizeCode() {
        String gscode = "${grainSizeOffset}:0" // minimum bound is always 0
        grainSizes.sort { it.width }.each {
            gscode += "<${it.name}<${it.width}"
        }
        return gscode
    }

    private void loadScale(Scale scale) {
        grainSizes = GrainSize.scaleToGrainSizes(scale)
        grainSizeOffset = scale.offset        
    }

    private void deriveFromGrainSizeScheme() {
        def gsentries = SchemeUtils.getGrainSizeSchemeEntries()
        if (gsentries.size() == 0) {
            Dialogs.showMessageDialog("No Grain Sizes", "No Grain Size scheme was found in this project.\nUse the Project > Choose Schemes... menu item to add one.")
            return
        }

        Vector<GrainSize> newGrainSizes = new Vector<GrainSize>()
        gsentries.each { gsentry ->
            newGrainSizes.add(new GrainSize(gsentry.name, gsentry.width as Double))
        }

        grainSizes = newGrainSizes
        gsTable.setModel(new GrainSizeTableModel(newGrainSizes))
        selectRow(0)
    }

    private void loadGrainSizes() {
        JPanel loadGrainSizesOptionsPanel = new JPanel(new MigLayout("wrap, insets 5"))
        JRadioButton csdfRadio = new JRadioButton("CSD Facility: for projects using CSD Facility grain sizes.", true) // default selection
        JRadioButton gsSchemeRadio = new JRadioButton("Project Grain Sizes: use the current project's custom grain sizes.")
        JRadioButton phiScaleRadio = new JRadioButton("Phi Scale: for legacy projects that use the Andrill Lithology column.")
        ButtonGroup radioGroup = new ButtonGroup()
        radioGroup.add(csdfRadio)
        radioGroup.add(gsSchemeRadio)
        radioGroup.add(phiScaleRadio)
        loadGrainSizesOptionsPanel.add(csdfRadio)
        loadGrainSizesOptionsPanel.add(gsSchemeRadio)
        loadGrainSizesOptionsPanel.add(phiScaleRadio)
        boolean confirmed = Dialogs.showCustomDialog("Load Grain Sizes", loadGrainSizesOptionsPanel, null, false)
        if (confirmed) {
            if (csdfRadio.isSelected() || phiScaleRadio.isSelected()) {
                loadScale(new Scale(csdfRadio.isSelected() ? Scale.CSDF_DEFAULT : Scale.DEFAULT))
                gsTable.setModel(new GrainSizeTableModel(grainSizes))
                selectRow(0)
            } else {
                deriveFromGrainSizeScheme()
            }
        }
    }

    private void selectRow(int row) {
        final rowCount = gsTable.getModel().getRowCount()
        if (rowCount > 0) {
            while (row >= rowCount) { row-- }
            gsTable.setRowSelectionInterval(row, row)
        }
    }

    private void onOk() {
        if (gsTable.getModel().size() == 0) {
            Dialogs.showErrorDialog("Error", "At least one grain size is required.")
            return
        }
        try {
            def offset = defaultWidthField.getText() as Double
            if (offset <= 0) {
                Dialogs.showErrorDialog("Invalid Width", "Minimum Interval Display Width must be greater than 0%.")
                return
            }
            if (offset > 100) {
                Dialogs.showErrorDialog("Invalid Width", "Minimum Interval Display Width cannot exceed 100%.")
                return
            }

            // detect and warn on duplicate widths
            HashSet widthSet = new HashSet()
            def duplicates = []
            grainSizes.each {
                if (!widthSet.add(it.width)) {
                    duplicates.add(it.width)
                }
            }
            if (duplicates.size() > 0) {
                String dupsStr = String.join(", ", duplicates.collect { it as String })
                String msg = "Duplicate grain size width values detected: ${dupsStr}.\nThis will result in overlapping labels in the grain size header.\nContinue?"
                def confirmed = Dialogs.showConfirmDialog("Duplicate Widths", msg)
                if (!confirmed) {
                    return
                }
            }
            grainSizeOffset = offset / 100.0
        } catch (NumberFormatException e) {
            Dialogs.showErrorDialog("Invalid Number", "Minimum Interval Display Width '${defaultWidthField.getText()}' is not a number.")
            return
        }
        okPressed = true
        setVisible(false)
        dispose()
    }

    private void setupUI() {
        Font defaultFont = UIManager.getDefaults().getFont("Label.font")
        helpFont = defaultFont.deriveFont((float)(defaultFont.getSize() - 2.0))

        setLayout(new MigLayout("fillx, wrap", "[]", "[]20[][grow]"))

        JPanel defaultWidthPanel = new JPanel(new MigLayout("fill, wrap, insets 0"))
        defaultWidthField = new JTextField()
        defaultWidthField.setText("${grainSizeOffset * 100.0}") // display as percentage
        defaultWidthPanel.add(new JLabel("Minimum Interval Display Width"), "split 3")
        defaultWidthPanel.add(defaultWidthField, "wmin 100")
        defaultWidthPanel.add(new JLabel("%"))
        final String widthDesc = "Lithology intervals will fill at least this % of the available column width."
        JLabel widthDescLabel = new JLabel(widthDesc)
        widthDescLabel.setFont(helpFont)
        defaultWidthPanel.add(widthDescLabel)

        gsTableModel = new GrainSizeTableModel(grainSizes)
        gsTable = new JTable(gsTableModel)
        gsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        selectRow(0)
        gsTable.setShowGrid(true)
        gsTable.setGridColor(Color.black)
        JScrollPane gsTableScrollPane = new JScrollPane(gsTable)

        addButton = new JButton("Add Grain Size")
        addButton.addActionListener({ event ->
            GrainSizeTableModel model = (GrainSizeTableModel)gsTable.getModel()
            model.addGrainSize(new GrainSize("New Grain Size", 999))
        } as ActionListener)

        deleteButton = new JButton("Delete Grain Size")
        deleteButton.addActionListener({ event ->
            int sel = gsTable.getSelectedRow()
            if (sel != -1) {
                GrainSizeTableModel model = (GrainSizeTableModel)gsTable.getModel()
                model.deleteGrainSize(sel)
                selectRow(sel)
            }
        } as ActionListener)

        loadButton = new JButton("Load Grain Sizes...")
        loadButton.addActionListener({ event ->
            loadGrainSizes()
        } as ActionListener)
        JLabel tableHelpLabel = new JLabel("Double-click cells to edit.")
        tableHelpLabel.setFont(helpFont)

        okButton = new JButton("OK")
        okButton.addActionListener({ event ->
            onOk()
        } as ActionListener)
        cancelButton = new JButton("Cancel")
        cancelButton.addActionListener({ dispose() } as ActionListener)

        add(defaultWidthPanel)

        add(addButton, "split 3")
        add(deleteButton)
        add(loadButton, "gapleft push")

        add(gsTableScrollPane, "grow")
        add(tableHelpLabel)

        add(cancelButton, "split 2, align right")
        add(okButton)

        addWindowListener([
            windowClosing: { dispose() }
        ] as WindowAdapter)

        pack()
    }
}

class GrainSizeTableModel extends AbstractTableModel {
    Vector<GrainSize> grainSizes
    public GrainSizeTableModel(Vector<GrainSize> grainSizes) {
        super()
        this.grainSizes = grainSizes
    }

    public int getColumnCount() { return 2; }
    public Class getColumnClass(int col) { return String.class }
    public String getColumnName(int col) { return col == 0 ? "Grain Size Name" : "Width"; }
    public int getRowCount() { return grainSizes.size(); }
    public Object getValueAt(int row, int col) {
        final GrainSize gs = grainSizes.get(row)
        if (col == 0) {
            return gs.name
         } else {
            // display width as integer if it has no decimal portion
            return BigDecimal.valueOf(gs.width).stripTrailingZeros().toPlainString()
         }
    }
    public void setValueAt(Object value, int row, int col) {
        GrainSize gs = grainSizes.get(row)
        if (col == 0) {
            String v = value
            if (v.indexOf('<') != -1) {
                Dialogs.showErrorDialog("Invalid Name", "Grain size names cannot include '<'.")
                return
            }
            gs.name = (String)value
        } else {
            try {
                def newWidth = value as Double
                if (newWidth <= 0) {
                    Dialogs.showErrorDialog("Invalid Width", "Widths must be greater than zero.")
                    return
                }
                gs.width = newWidth
            } catch (NumberFormatException e) {
                Dialogs.showErrorDialog("Invalid Number", "'${value}' is not a number.")
            }
        }
    }
    public boolean isCellEditable(int row, int col) { return true; }
    public void addGrainSize(GrainSize gs) {
        grainSizes.add(gs)
        fireTableRowsInserted(grainSizes.size()-1, grainSizes.size()-1)
    }
    public void deleteGrainSize(int index) {
        grainSizes.remove(index)
        fireTableRowsDeleted(index, index)
    }
}

class GrainSize {
    public String name
    public double width
    public GrainSize(String name, double width) {
        this.name = name
        this.width = width
    }

    public static Vector<GrainSize> scaleToGrainSizes(Scale scale) {
        Vector<GrainSize> grainSizes = new Vector<GrainSize>()
        for (int i = 0; i < scale.labels.size(); i++) {
            GrainSize gs = new GrainSize(scale.labels[i], scale.values[i+1]); // use the size's upper bound for width
            grainSizes.add(gs)
        }
        return grainSizes
    }
}
