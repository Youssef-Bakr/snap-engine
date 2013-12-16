package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.grid.AutoFilterTableHeader;
import com.jidesoft.grid.HierarchicalTable;
import com.jidesoft.grid.HierarchicalTableComponentFactory;
import com.jidesoft.grid.HierarchicalTableModel;
import com.jidesoft.grid.JideTable;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.grid.TreeLikeHierarchicalPanel;
import com.jidesoft.grid.TristateCheckBoxCellEditor;
import com.jidesoft.swing.TristateCheckBox;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.DecimalTableCellRenderer;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.ArrayUtils;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpectrumChooser extends ModalDialog {

    private static final int spectrumSelectedIndex = 0;
    private static final int spectrumNameIndex = 1;
    private static final int spectrumStrokeIndex = 2;
    private static final int spectrumShapeIndex = 3;

    private static final int bandSelectedIndex = 0;
    private static final int bandNameIndex = 1;
    private static final int bandDescriptionIndex = 2;
    private static final int bandWavelengthIndex = 3;
    private static final int bandBandwidthIndex = 4;

    private static HierarchicalTable spectraTable;

    private List<DisplayableSpectrum> allSpectra;
    private SelectionAdmin selectionAdmin;
    //    private List<SelectionState> selectionStates;
//    private List<List<Boolean>> bandSelectionStates;
//    private List<Integer> numbersOfSelectedBands;
    private static boolean selectionChangeLock;

    private final Map<Integer, SortableTable> rowToBandsTable;

    public SpectrumChooser(Window parent, List<DisplayableSpectrum> allSpectra, String helpID) {
        super(parent, "Available Spectra", ModalDialog.ID_OK_CANCEL, helpID);
        if (allSpectra != null) {
            this.allSpectra = allSpectra;
        } else {
            this.allSpectra = new ArrayList<DisplayableSpectrum>();
        }
        selectionAdmin = new SelectionAdmin();
//        selectionStates = new ArrayList<SelectionState>();
//        bandSelectionStates = new ArrayList<List<Boolean>>();
//        numbersOfSelectedBands = new ArrayList<Integer>();
        selectionChangeLock = false;
        rowToBandsTable = new HashMap<Integer, SortableTable>();
        initUI();
    }

    private void initUI() {
        final JPanel content = new JPanel(new BorderLayout());
        initSpectraTable();
        JScrollPane spectraScrollPane = new JScrollPane(spectraTable);
        final Dimension preferredSize = spectraTable.getPreferredSize();
        spectraScrollPane.setPreferredSize(new Dimension(Math.max(preferredSize.width + 20, 550),
                                                         Math.max(preferredSize.height + 10, 200)));
        content.add(spectraScrollPane, BorderLayout.CENTER);
        setContent(content);
    }

    @SuppressWarnings("unchecked")
    private void initSpectraTable() {
        SpectrumTableModel spectrumTableModel = new SpectrumTableModel();
        spectraTable = new HierarchicalTable(spectrumTableModel);
        spectraTable.setComponentFactory(new SpectrumTableComponentFactory());
        AutoFilterTableHeader header = new AutoFilterTableHeader(spectraTable);
        spectraTable.setTableHeader(header);
        spectraTable.setRowHeight(21);

        final TableColumn selectionColumn = spectraTable.getColumnModel().getColumn(spectrumSelectedIndex);
        final TristateCheckBoxCellEditor tristateCheckBoxCellEditor = new TristateCheckBoxCellEditor();
        selectionColumn.setCellEditor(tristateCheckBoxCellEditor);
        selectionColumn.setCellRenderer(new TriStateRenderer());
        selectionColumn.setMinWidth(38);
        selectionColumn.setMaxWidth(38);

        final TableColumn strokeColumn = spectraTable.getColumnModel().getColumn(spectrumStrokeIndex);
        JComboBox strokeComboBox = new JComboBox(SpectrumConstants.strokeIcons);
        ImageIconComboBoxRenderer strokeComboBoxRenderer = new ImageIconComboBoxRenderer();
        strokeComboBoxRenderer.setPreferredSize(new Dimension(200, 30));
        strokeComboBox.setRenderer(strokeComboBoxRenderer);
        strokeColumn.setCellEditor(new DefaultCellEditor(strokeComboBox));

        final TableColumn shapeColumn = spectraTable.getColumnModel().getColumn(spectrumShapeIndex);
        JComboBox shapeComboBox = new JComboBox(SpectrumConstants.shapeIcons);
        ImageIconComboBoxRenderer shapeComboBoxRenderer = new ImageIconComboBoxRenderer();
        shapeComboBoxRenderer.setPreferredSize(new Dimension(200, 30));
        shapeComboBox.setRenderer(shapeComboBoxRenderer);
        shapeColumn.setCellEditor(new DefaultCellEditor(shapeComboBox));
    }

    public List<DisplayableSpectrum> getSpectra() {
        return allSpectra;
    }

    class SpectrumTableModel extends DefaultTableModel implements HierarchicalTableModel {

        private final Class[] COLUMN_CLASSES = {
                Boolean.class,
                String.class,
                ImageIcon.class,
                ImageIcon.class,
        };
        private final String[] bandColumns = new String[]{"", "Band name", "Band description", "Spectral wavelength (nm)", "Spectral bandwidth (nm)"};

        public SpectrumTableModel() {
            super(new String[]{"", "Spectrum name", "Line style", "Symbol"}, 0);
            for (DisplayableSpectrum spectrum : allSpectra) {
                addRow(spectrum);
            }
        }

        @Override
        public Object getChildValueAt(final int row) {
            DisplayableSpectrum spectrum = allSpectra.get(row);
            if (rowToBandsTable.containsKey(row)) {
                return rowToBandsTable.get(row);
            }
            final Band[] spectralBands = spectrum.getSpectralBands();
            Object[][] spectrumData = new Object[spectralBands.length][bandColumns.length];
            for (int i = 0; i < spectralBands.length; i++) {
                Band spectralBand = spectralBands[i];
                final boolean selected = spectrum.isBandSelected(i) && spectrum.isSelected();
                spectrumData[i][bandSelectedIndex] = selected;
                selectionAdmin.addBand(row, selected);
//                bandSelectionStates.get(row).add(selected);
//                if (selected) {
//                    numbersOfSelectedBands.set(row, numbersOfSelectedBands.get(row) + 1);
//                }
                spectrumData[i][bandNameIndex] = spectralBand.getName();
                spectrumData[i][bandDescriptionIndex] = spectralBand.getDescription();
                spectrumData[i][bandWavelengthIndex] = spectralBand.getSpectralWavelength();
                spectrumData[i][bandBandwidthIndex] = spectralBand.getSpectralBandwidth();
            }
            final BandTableModel bandTableModel = new BandTableModel(spectrumData, bandColumns);
            bandTableModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    e.getSource();
                    if (e.getColumn() == bandSelectedIndex) {
                        final DisplayableSpectrum spectrum = allSpectra.get(row);
                        final int bandRow = e.getFirstRow();
                        final Boolean selected = (Boolean) bandTableModel.getValueAt(bandRow, e.getColumn());
                        spectrum.setBandSelected(bandRow, selected);
                        if (!selectionChangeLock) {
                            selectionChangeLock = true;
                            selectionAdmin.updateBandSelections(row, bandRow, selected);
                            //                            int state;
//                            if (numbersOfSelectedBands.get(row) == 0) {
//                                state = TristateCheckBox.STATE_UNSELECTED;
//                            } else if (numbersOfSelectedBands.get(row) == bandSelectionStates.get(row).size()) {
//                                state = TristateCheckBox.STATE_SELECTED;
//                            } else {
//                                state = TristateCheckBox.STATE_MIXED;
//                            }
                            spectraTable.getModel().setValueAt(selectionAdmin.getState(row), row, spectrumSelectedIndex);
                            spectrum.setSelected(selectionAdmin.isSpectrumSelected(row));
                            selectionChangeLock = false;
                        }
                    }
                }
            });
            return bandTableModel;
        }

        private void addRow(DisplayableSpectrum spectrum) {
            if (spectrum.getLineStyle() == null) {
                spectrum.setLineStyle(SpectrumConstants.strokes[getRowCount() % SpectrumConstants.strokes.length]);
            }
            final ImageIcon strokeIcon = SpectrumConstants.strokeIcons[ArrayUtils.getElementIndex(spectrum.getLineStyle(), SpectrumConstants.strokes)];
            if (spectrum.getSymbol() == null) {
                spectrum.setSymbol(SpectrumConstants.shapes[getRowCount() % SpectrumConstants.shapes.length]);
            }
            final ImageIcon shapeIcon = SpectrumConstants.shapeIcons[ArrayUtils.getElementIndex(spectrum.getSymbol(), SpectrumConstants.shapes)];

            selectionAdmin.addSpectrumSelections(spectrum);
            super.addRow(new Object[]{selectionAdmin.getState(getRowCount()), spectrum.getName(), strokeIcon, shapeIcon});
//            selectionStates.bandSelectionStates.add(selected);
//            numbersOfSelectedBands.add(numberOfSelectedBands);
        }

        @Override
        public boolean hasChild(int row) {
            return true;
        }

        @Override
        public boolean isHierarchical(int row) {
            return true;
        }

        @Override
        public boolean isExpandable(int row) {
            return true;
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return COLUMN_CLASSES[columnIndex];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column != spectrumNameIndex;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == spectrumSelectedIndex && !selectionChangeLock) {
                selectionChangeLock = true;

                if ((Integer) aValue == TristateCheckBox.STATE_MIXED) {
                    if (selectionAdmin.areNoBandsSelected(row)) {
                        aValue = TristateCheckBox.STATE_UNSELECTED;
                    } else if (selectionAdmin.areAllBandsSelected(row)) {
                        aValue = TristateCheckBox.STATE_SELECTED;
//                    } else {
//                        updateBandsTable(row, new MixedSelectedCheckBoxState(bandSelectionStates.get(row)));
                    }
                }
//                if (value == TristateCheckBox.STATE_SELECTED) {
//                    updateBandsTable(row, new AllSelectedCheckBoxState());
//                } else if (value == TristateCheckBox.STATE_UNSELECTED) {
//                    updateBandsTable(row, new NoneSelectedCheckBoxState());
//                }
                selectionAdmin.updateBandSelections(row, (Integer) aValue);
                aValue = selectionAdmin.getState(row);
                updateBandsTable(row);
                allSpectra.get(row).setSelected(selectionAdmin.isSpectrumSelected(row));
                fireTableCellUpdated(row, column);
                selectionChangeLock = false;
            } else if (column == spectrumStrokeIndex) {
                allSpectra.get(row).setLineStyle(SpectrumConstants.strokes[ArrayUtils.getElementIndex(aValue, SpectrumConstants.strokeIcons)]);
                fireTableCellUpdated(row, column);
            } else if (column == spectrumShapeIndex) {
                allSpectra.get(row).setSymbol(SpectrumConstants.shapes[ArrayUtils.getElementIndex(aValue, SpectrumConstants.shapeIcons)]);
                fireTableCellUpdated(row, column);
            }
            super.setValueAt(aValue, row, column);
        }

        private void updateBandsTable(int row) {
            if (rowToBandsTable.containsKey(row)) {
                final SortableTable bandsTable = rowToBandsTable.get(row);
                final TableModel tableModel = bandsTable.getModel();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt(selectionAdmin.isBandSelected(row, i), i, bandSelectedIndex);
                }
            } else {
                for (int i = 0; i < allSpectra.get(row).getSpectralBands().length; i++) {
                    allSpectra.get(row).setBandSelected(i, selectionAdmin.isBandSelected(row, i));
                }
            }
        }
    }

    static class BandTableModel extends DefaultTableModel {

        public BandTableModel(Object[][] spectrumData, String[] bandColumns) {
            super(spectrumData, bandColumns);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == bandSelectedIndex;
        }

    }

    class SpectrumTableComponentFactory implements HierarchicalTableComponentFactory {

        @Override
        public Component createChildComponent(HierarchicalTable table, Object value, int row) {
            if (value == null) {
                return new JPanel();
            }
            TableModel model;
            if (value instanceof JideTable) {
                model = ((JideTable) value).getModel();
            } else {
                model = (TableModel) value;
            }
            SortableTable bandsTable = new SortableTable(model);
            AutoFilterTableHeader bandsHeader = new AutoFilterTableHeader(bandsTable);
            bandsTable.setTableHeader(bandsHeader);

            final TableColumn selectionColumn = bandsTable.getColumnModel().getColumn(bandSelectedIndex);
            final JCheckBox selectionCheckBox = new JCheckBox();
            selectionColumn.setCellEditor(new DefaultCellEditor(selectionCheckBox));
            selectionColumn.setMinWidth(20);
            selectionColumn.setMaxWidth(20);
            BooleanRenderer booleanRenderer = new BooleanRenderer();
            selectionColumn.setCellRenderer(booleanRenderer);

            final TableColumn wavelengthColumn = bandsTable.getColumnModel().getColumn(bandWavelengthIndex);
            wavelengthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

            final TableColumn bandwidthColumn = bandsTable.getColumnModel().getColumn(bandBandwidthIndex);
            bandwidthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

            rowToBandsTable.put(row, bandsTable);

            final JScrollPane jScrollPane = new SpectrumScrollPane(bandsTable);
            return new TreeLikeHierarchicalPanel(jScrollPane);
        }

        @Override
        public void destroyChildComponent(HierarchicalTable table, Component component, int row) {
            // do nothing
        }
    }

    private static class SpectrumScrollPane extends JScrollPane {

        public SpectrumScrollPane(JTable table) {
            super(table);
        }

        @Override
        public Dimension getPreferredSize() {
            getViewport().setPreferredSize(getViewport().getView().getPreferredSize());
            return super.getPreferredSize();
        }
    }

    class ImageIconComboBoxRenderer extends JLabel implements ListCellRenderer {

        public ImageIconComboBoxRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setIcon((ImageIcon) value);
            return this;
        }

    }

    class BooleanRenderer extends JCheckBox implements TableCellRenderer {

        public BooleanRenderer() {
            this.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            boolean selected = (Boolean) value;
            setSelected(selected);
            return this;
        }
    }

    class TriStateRenderer extends TristateCheckBox implements TableCellRenderer {

        public TriStateRenderer() {
            this.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            int state = (Integer) value;
            setState(state);
            return this;
        }
    }

    class SelectionAdmin {

        //        private List<SelectionState> selectionStates;
        private List<List<Boolean>> bandSelectionStates;
        private List<Integer> numbersOfSelectedBands;
        private List<Integer> currentStates;


        SelectionAdmin() {
//            selectionStates = new ArrayList<SelectionState>();
            bandSelectionStates = new ArrayList<List<Boolean>>();
            numbersOfSelectedBands = new ArrayList<Integer>();
            currentStates = new ArrayList<Integer>();
        }

        void addSpectrumSelections(DisplayableSpectrum spectrum) {
            List<Boolean> selected = new ArrayList<Boolean>();
            int numberOfSelectedBands = 0;
            for (int i = 0; i < spectrum.getSpectralBands().length; i++) {
                final boolean bandSelected = spectrum.isBandSelected(i);
                selected.add(bandSelected);
                if (bandSelected) {
                    numberOfSelectedBands++;
                }
            }
            bandSelectionStates.add(selected);
            numbersOfSelectedBands.add(numberOfSelectedBands);
            currentStates.add(-1);
            evaluate(bandSelectionStates.size() - 1);
        }

        void addBand(int index, boolean selected) {
            bandSelectionStates.get(index).add(selected);
            if (selected) {
                numbersOfSelectedBands.set(index, numbersOfSelectedBands.get(index) + 1);
            }
        }

        boolean areNoBandsSelected(int row) {
            return numbersOfSelectedBands.get(row) == 0;
        }

        boolean areAllBandsSelected(int row) {
            return numbersOfSelectedBands.get(row) == bandSelectionStates.get(row).size();
        }

        boolean isBandSelected(int row, int i) {
            if (currentStates.get(row) == TristateCheckBox.STATE_MIXED) {
                return bandSelectionStates.get(row).get(i);
            } else if (currentStates.get(row) == TristateCheckBox.STATE_SELECTED) {
                return true;
            } else {
                return false;
            }
        }

        void evaluate(int index) {
            final Integer numberOfBands = numbersOfSelectedBands.get(index);
            if (numberOfBands == 0) {
                currentStates.set(index, TristateCheckBox.STATE_UNSELECTED);
            } else if (numberOfBands == bandSelectionStates.get(index).size()) {
                currentStates.set(index, TristateCheckBox.STATE_SELECTED);
            } else {
                currentStates.set(index, TristateCheckBox.STATE_MIXED);
            }
        }

        public int getState(int index) {
//            evaluate(index);
            return currentStates.get(index);
        }

        public boolean isSpectrumSelected(int row) {
            return currentStates.get(row) != TristateCheckBox.STATE_UNSELECTED;
        }

        private void updateBandSelections(int row, int bandRow, boolean selected) {
            bandSelectionStates.get(row).set(bandRow, selected);
            updateNumbersOfSelectedBands(selected, row);
//            int state = currentStates.get(row);
            evaluate(row);
//            if(state != currentStates.get(row)) {
//
//            }
//            getState(row);
//            updateBandSelection(row);
        }

        private void updateBandSelections(int row, int newState) {
            if (newState == TristateCheckBox.STATE_MIXED) {
                if (selectionAdmin.areNoBandsSelected(row)) {
                    newState = TristateCheckBox.STATE_UNSELECTED;
                } else if (selectionAdmin.areAllBandsSelected(row)) {
                    newState = TristateCheckBox.STATE_SELECTED;
                }
            }
            currentStates.set(row, newState);

//            updateBandSelection(row);


//            if ((Integer) spectraTable.getModel().getValueAt(row, spectrumSelectedIndex) == TristateCheckBox.STATE_SELECTED) {
//                for (int i = 0; i < bandSelectionStates.get(row).size(); i++) {
//                    bandSelectionStates.get(row).set(i, true);
//                }
//                numbersOfSelectedBands.set(row, bandSelectionStates.get(row).size());
//            } else if ((Integer) spectraTable.getModel().getValueAt(row, spectrumSelectedIndex) == TristateCheckBox.STATE_UNSELECTED) {
//                for (int i = 0; i < bandSelectionStates.get(row).size(); i++) {
//                    bandSelectionStates.get(row).set(i, false);
//                }
//                numbersOfSelectedBands.set(row, 0);
//            }

//            bandSelectionStates.get(row).set(bandRow, selected);

//            updateNumbersOfSelectedBands(selected, row);
        }

//        private void updateBandSelection(int index) {
//            final List<Boolean> bandSelection = bandSelectionStates.get(index);
//            if(newState != TristateCheckBox.STATE_MIXED) {
//                for (int i = 0; i < bandSelectionStates.get(index).size(); i++) {
//                    bandSelection.set(i, newState == TristateCheckBox.STATE_SELECTED);
//                }
//            }
//        }

        private void updateNumbersOfSelectedBands(Boolean selected, int row) {
            if (selected) {
                numbersOfSelectedBands.set(row, numbersOfSelectedBands.get(row) + 1);
            } else {
                numbersOfSelectedBands.set(row, numbersOfSelectedBands.get(row) - 1);
            }
        }

    }

}