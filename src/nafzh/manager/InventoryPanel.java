package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import static nafzh.manager.NafzhManager.getCairoFont;
import nafzh.manager.TableActionRendererEditor.ActionType;

public class InventoryPanel extends JPanel {

    private final DatabaseManager dbManager;
    private final NafzhManager parentFrame;
    private JTable inventoryTable;
    private ExpandableInventoryTableModel tableModel;
    private List<Map<String, Object>> productsData;
    
    private static final int CATEGORY_COL_INDEX = 0;
    private static final int ACTION_COL_INDEX = 7; 

    public InventoryPanel(DatabaseManager dbManager, NafzhManager parentFrame) {
        this.dbManager = dbManager;
        this.parentFrame = parentFrame;
        productsData = dbManager.getAllProducts();
        tableModel = new ExpandableInventoryTableModel(productsData);
        initializePanel();
        addCategoryCollapseListener();
    }

    private void initializePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setName("Inventory");

        JLabel titleLabel = new JLabel("إدارة المخزون والأصناف", SwingConstants.RIGHT);
        titleLabel.setFont(getCairoFont(21f));
        titleLabel.setForeground(new Color(50, 50, 50));

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setOpaque(false);
        northPanel.add(titleLabel, BorderLayout.EAST);

        JButton addButton = new JButton("إضافة صنف");
        addButton.setFont(getCairoFont(11f));
        addButton.setBackground(new Color(76, 175, 80));
        addButton.setForeground(Color.BLACK);
        addButton.setFocusPainted(false);
        addButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        addButton.addActionListener(e -> parentFrame.showProductDialog(this::loadInventoryData));
        northPanel.add(addButton, BorderLayout.WEST);

        add(northPanel, BorderLayout.NORTH);

        inventoryTable = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == ACTION_COL_INDEX;
            }
        };

        customizeTable(inventoryTable);
        setupActionColumn();

        TableColumn categoryColumn = inventoryTable.getColumnModel().getColumn(CATEGORY_COL_INDEX);
        categoryColumn.setCellRenderer(new CategoryTreeCellRenderer(tableModel));

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        // إضافة إطار أسود حول منطقة التمرير ليتطابق مع الصورة المطلوبة
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadInventoryData() {
        productsData = dbManager.getAllProducts();
        tableModel = new ExpandableInventoryTableModel(productsData);
        inventoryTable.setModel(tableModel);
        customizeTable(inventoryTable);
        setupActionColumn();
        TableColumn categoryColumn = inventoryTable.getColumnModel().getColumn(CATEGORY_COL_INDEX);
        categoryColumn.setCellRenderer(new CategoryTreeCellRenderer(tableModel));
        inventoryTable.revalidate();
        inventoryTable.repaint();
        parentFrame.updateDashboardData();
    }

    private void setupActionColumn() {
        TableColumn actionColumn = inventoryTable.getColumnModel().getColumn(ACTION_COL_INDEX);
        TableActionRendererEditor actionRendererEditor = new TableActionRendererEditor(
                inventoryTable,
                this::handleEditAction,
                this::handleDeleteAction,
                null,
                ActionType.EDIT_DELETE
        );
        actionColumn.setCellRenderer(actionRendererEditor);
        actionColumn.setCellEditor(actionRendererEditor);
    }

    private void handleEditAction(int rowIndex) {
        if (tableModel.isCategoryRow(rowIndex)) return;
        Map<String, Object> product = tableModel.getProductDataAt(rowIndex);
        if (product != null) {
            parentFrame.showProductDialog(
                    (int) product.get("id"),
                    (String) product.get("name"),
                    (String) product.get("category"),
                    (int) product.get("current_quantity"),
                    (double) product.get("purchase_price"),
                    (double) product.get("sale_price"),
                    this::loadInventoryData
            );
        }
    }

    private void handleDeleteAction(int rowIndex) {
        if (tableModel.isCategoryRow(rowIndex)) return;
        Integer productId = tableModel.getProductIdAt(rowIndex);
        if (productId != null) {
            int confirm = JOptionPane.showConfirmDialog(this, "هل أنت متأكد من الحذف؟", "تأكيد", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (dbManager.deleteProduct(productId)) {
                    loadInventoryData();
                }
            }
        }
    }

    private void addCategoryCollapseListener() {
        inventoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = inventoryTable.rowAtPoint(e.getPoint());
                int col = inventoryTable.columnAtPoint(e.getPoint());
                if (col == CATEGORY_COL_INDEX && row != -1) {
                    if (tableModel.isCategoryRow(row)) {
                        String category = (String) inventoryTable.getValueAt(row, CATEGORY_COL_INDEX);
                        tableModel.toggleExpandState(category);
                    }
                }
            }
        });
    }

    private void customizeTable(JTable table) {
        // --- التعديلات الجوهرية للرسم ---
        table.setShowGrid(true); // إظهار الخطوط
        table.setGridColor(Color.BLACK); // جعل لون الخطوط أسود
        table.setRowHeight(35);
        table.setFont(getCairoFont(12f));
        table.getTableHeader().setFont(getCairoFont(14f));
        table.getTableHeader().setBackground(new Color(230, 230, 230));
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // Renderer مخصص للرؤوس لضمان وجود حدود سوداء
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createLineBorder(Color.BLACK));
                setHorizontalAlignment(JLabel.CENTER);
                setBackground(new Color(220, 220, 220));
                return this;
            }
        };

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                return this;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getTableHeader().getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            // استثناء عمود الأفعال من الـ Renderer المركزي إذا لزم الأمر، لكننا سنطبقه للتماثل
            if (i != ACTION_COL_INDEX) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        table.getColumnModel().getColumn(1).setPreferredWidth(50); // ID
        table.getColumnModel().getColumn(ACTION_COL_INDEX).setPreferredWidth(120);
    }
}