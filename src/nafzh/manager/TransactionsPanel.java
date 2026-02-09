package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Collections;
import static nafzh.manager.NafzhManager.getCairoFont;
import nafzh.manager.TableActionRendererEditor.ActionType;

public class TransactionsPanel extends JPanel {
    private final DatabaseManager dbManager;
    private final NafzhManager parentFrame;
    private JTable transactionsTable;
    private DefaultTableModel tableModel;
    private List<Integer> saleIds;
    private JButton printInvoiceBtn;
    private static final String[] COLUMN_NAMES = {"ID", "تاريخ البيع", "اسم العميل", "الإجمالي", "تحديد", "الإجراءات"};

    public TransactionsPanel(DatabaseManager dbManager, NafzhManager parentFrame) {
        this.dbManager = dbManager;
        this.parentFrame = parentFrame;
        this.saleIds = Collections.emptyList();
        initializePanel();
        loadData();
    }

    private void initializePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setName("Transactions");
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topPanel.setOpaque(false);
        printInvoiceBtn = new JButton("طباعة الفاتورة");
        printInvoiceBtn.setBackground(new Color(33, 150, 243));
        printInvoiceBtn.setForeground(Color.BLACK);
        printInvoiceBtn.setFont(getCairoFont(12f));
        printInvoiceBtn.addActionListener(e -> handlePrintInvoiceAction());
        JButton refreshButton = new JButton("تحديث");
        refreshButton.setBackground(new Color(103, 58, 183));
        refreshButton.setForeground(Color.BLACK);
        refreshButton.setFont(getCairoFont(12f));
        refreshButton.addActionListener(e -> loadData());
        topPanel.add(printInvoiceBtn);
        topPanel.add(refreshButton);
        add(topPanel, BorderLayout.NORTH);
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5 || column == 4;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 4) {
                    return Boolean.class;
                }
                if (column == 0) {
                    return Integer.class;
                }
                if (column == 3) {
                    return Double.class;
                }
                return super.getColumnClass(column);
            }
        };
        transactionsTable = new JTable(tableModel);
        customizeTable(transactionsTable);
        TableColumn actionColumn = transactionsTable.getColumnModel().getColumn(5);
        TableActionRendererEditor actionRendererEditor = new TableActionRendererEditor(
                transactionsTable,
                null,
                null,
                this::handleViewAction,
                ActionType.VIEW_ONLY
        );
        actionColumn.setCellRenderer(actionRendererEditor);
        actionColumn.setCellEditor(actionRendererEditor);
        actionColumn.setPreferredWidth(100);
        actionColumn.setMinWidth(100);
        actionColumn.setMaxWidth(100);
        transactionsTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        add(new JScrollPane(transactionsTable), BorderLayout.CENTER);
    }

    public void loadData() {
        List<List<Object>> data = dbManager.getAllSaleTransactions();
        tableModel.setRowCount(0);
        saleIds = new ArrayList<>();
        for (List<Object> rowData : data) {
            Vector<Object> row = new Vector<>(rowData);
            saleIds.add((Integer) row.get(0));
            row.add(Boolean.FALSE);
            row.add("عرض");
            tableModel.addRow(row);
        }
        customizeTable(transactionsTable);
    }

    private void handlePrintInvoiceAction() {
        List<Integer> selectedSaleIds = new ArrayList<>();
        int checkboxColumnIndex = 4;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object value = tableModel.getValueAt(i, checkboxColumnIndex);
            if (value instanceof Boolean && (Boolean) value) {
                if (i < saleIds.size()) {
                    selectedSaleIds.add(saleIds.get(i));
                }
            }
        }
        if (selectedSaleIds.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "يرجى تحديد حركة مبيعات واحدة على الأقل لطباعة الفاتورة.",
                    "تنبيه",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Corrected: Loop through all selected IDs and open a dialog for each
        Frame owner = JOptionPane.getFrameForComponent(this);
        for (int saleId : selectedSaleIds) {
            new PrintDialog(owner, saleId, dbManager).setVisible(true);
        }
    }

    private void handleViewAction(int row) {
        if (row >= 0 && row < saleIds.size()) {
            int saleId = saleIds.get(row);
            parentFrame.showSaleDetailsDialog(saleId);
        }
    }

    private void customizeTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(getCairoFont(12f));
        table.setShowGrid(true);
        table.setGridColor(new Color(200, 200, 200));
        table.setFont(getCairoFont(10f));
        table.getTableHeader().setBackground(new Color(220, 220, 220));
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setVerticalAlignment(JLabel.CENTER);
        DefaultTableCellRenderer centerHeaderRenderer = new DefaultTableCellRenderer();
        centerHeaderRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerHeaderRenderer.setVerticalAlignment(JLabel.CENTER);
        centerHeaderRenderer.setFont(table.getTableHeader().getFont());
        centerHeaderRenderer.setBackground(new Color(230, 230, 230));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getTableHeader().getColumnModel().getColumn(i).setHeaderRenderer(centerHeaderRenderer);
            if (i != 4) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setMinWidth(60);
        table.getColumnModel().getColumn(4).setMaxWidth(60);
        table.getTableHeader().getColumnModel().getColumn(4).setHeaderValue("✔");
        if (table.getColumnCount() > 5) {
            table.getColumnModel().getColumn(5).setPreferredWidth(100);
            table.getColumnModel().getColumn(5).setMinWidth(100);
            table.getColumnModel().getColumn(5).setMaxWidth(100);
        }
    }

    void loadTransactionsData() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
