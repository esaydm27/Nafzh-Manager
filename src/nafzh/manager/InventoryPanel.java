package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import javax.swing.border.EmptyBorder;
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

    // 1. استبدل handleEditAction الحالية بهذا الكود
    private void handleEditAction(int rowIndex) {
    if (tableModel.isCategoryRow(rowIndex)) return;
    
    // جلب بيانات المنتج كـ Map
    Map<String, Object> productData = tableModel.getProductDataAt(rowIndex);
    
    if (productData != null) {
        // استدعاء دالتنا المحلية الجديدة
        showProductDialog(productData);
    }
}

    // استبدل دالة showProductDialog الحالية في ملف InventoryPanel.java بهذا الكود المطور
    private void showProductDialog(Map<String, Object> existingData) {
        boolean isEdit = (existingData != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "تعديل صنف" : "إضافة صنف جديد", true);

        // تصميم مدمج وأنيق (عرض 400 وارتفاع مناسب)
        dialog.setSize(400, 650); 
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        panel.setBackground(Color.WHITE);
        panel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // تجهيز البيانات
        String nameVal = isEdit ? (String) existingData.get("name") : "";
        String catVal = isEdit ? (String) existingData.get("category") : "";
        String unitVal = isEdit && existingData.containsKey("unit") ? (String) existingData.get("unit") : "قطعة";
        String qtyVal = isEdit ? String.valueOf(existingData.get("current_quantity")) : "0";
        String purchVal = isEdit ? String.valueOf(existingData.get("purchase_price")) : "0.0";
        String saleVal = isEdit ? String.valueOf(existingData.get("sale_price")) : "0.0";

        // --- 1. حقل الاسم ---
        MaterialTextField txtName = new MaterialTextField("اسم الصنف", nameVal);

        // --- 2. قسم الفئة (Combobox + Button) بتصميم Material ---
        JPanel categoryPanel = createComboPanelWithButton("الفئة", dbManager.getAllCategories(), catVal, dialog, true);

        // --- 3. قسم الوحدة (Combobox قابل للكتابة) ---
        JPanel unitPanel = createComboPanelWithButton("الوحدة", dbManager.getAllUnits(), unitVal, dialog, false);

        // --- 4. باقي الحقول الرقمية ---
        MaterialTextField txtQty = new MaterialTextField("الكمية الحالية", qtyVal);
        MaterialTextField txtPurch = new MaterialTextField("سعر الشراء", purchVal);
        MaterialTextField txtSale = new MaterialTextField("سعر البيع", saleVal);

        // --- إضافة العناصر للوحة بترتيب جميل ---
        panel.add(txtName);
        panel.add(Box.createVerticalStrut(15));
        panel.add(categoryPanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(unitPanel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(txtQty);
        panel.add(Box.createVerticalStrut(15));
        panel.add(txtPurch);
        panel.add(Box.createVerticalStrut(15));
        panel.add(txtSale);
        panel.add(Box.createVerticalStrut(25));

        // --- زر الحفظ ---
        JButton btnSave = new JButton("حفظ الصنف");
        btnSave.setFont(getCairoFont(14));
        btnSave.setBackground(new Color(39, 174, 96));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        btnSave.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                if (name.isEmpty()) { JOptionPane.showMessageDialog(dialog, "الاسم مطلوب", "تنبيه", JOptionPane.WARNING_MESSAGE); return; }

                // استخراج القيم من الـ ComboBoxes
                JComboBox<String> catCombo = (JComboBox<String>) ((JPanel) categoryPanel.getComponent(1)).getComponent(0);
                String cat = (String) catCombo.getSelectedItem();

                JComboBox<String> unitCombo = (JComboBox<String>) ((JPanel) unitPanel.getComponent(1)).getComponent(0);
                String unit = (String) unitCombo.getSelectedItem();

                int qty = Integer.parseInt(txtQty.getText().trim());
                double pPrice = Double.parseDouble(txtPurch.getText().trim());
                double sPrice = Double.parseDouble(txtSale.getText().trim());

                if (isEdit) {
                    int id = (int) existingData.get("id");
                    dbManager.updateProduct(id, name, cat, unit, qty, pPrice, sPrice);
                } else {
                    dbManager.addProduct(name, cat, unit, qty, pPrice, sPrice);
                }
                loadInventoryData();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "يرجى إدخال أرقام صحيحة");
            }
        });

        panel.add(btnSave);
        dialog.add(panel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // دالة مساعدة لإنشاء قسم القائمة المنسدلة (لتفادي تكرار الكود)
    private JPanel createComboPanelWithButton(String labelText, java.util.Vector<String> items, String selectedItem, JDialog parentDialog, boolean hasAddButton) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JLabel label = new JLabel(labelText);
        label.setFont(getCairoFont(11));
        label.setForeground(Color.GRAY);
        label.setHorizontalAlignment(JLabel.RIGHT); // محاذاة لليمين
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 5));

        JPanel comboContainer = new JPanel(new BorderLayout(5, 0)); 
        comboContainer.setBackground(Color.WHITE);

        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setEditable(true); // قابلة للكتابة
        comboBox.setFont(getCairoFont(13));
        if (selectedItem != null) comboBox.setSelectedItem(selectedItem);

        // محاذاة النص داخل الـ ComboBox Editor لليمين
        Component editorComponent = comboBox.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextField) {
            ((JTextField) editorComponent).setHorizontalAlignment(JTextField.RIGHT);
        }

        comboContainer.add(comboBox, BorderLayout.CENTER);

        if (hasAddButton) {
            JButton btnAdd = new JButton("+");
            btnAdd.setFont(new Font("Arial", Font.BOLD, 14));
            btnAdd.setBackground(new Color(52, 152, 219));
            btnAdd.setForeground(Color.WHITE);
            btnAdd.setMargin(new Insets(2, 8, 2, 8));
            btnAdd.setFocusPainted(false);
            btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btnAdd.addActionListener(e -> {
                String newItem = JOptionPane.showInputDialog(parentDialog, "أدخل اسم " + labelText + " الجديدة:");
                if (newItem != null && !newItem.trim().isEmpty()) {
                    boolean success = false;
                    if (labelText.equals("الفئة")) success = dbManager.addCategory(newItem.trim());
                    // يمكن إضافة شرط للوحدات هنا أيضاً لو لزم الأمر

                    if (success) {
                        comboBox.addItem(newItem.trim());
                        comboBox.setSelectedItem(newItem.trim());
                    } else {
                        JOptionPane.showMessageDialog(parentDialog, "موجود بالفعل أو حدث خطأ");
                    }
                }
            });
            comboContainer.add(btnAdd, BorderLayout.WEST); 
        }

        // تصميم الحافة السفلية (Material Style)
        comboContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        panel.add(label, BorderLayout.NORTH);
        panel.add(comboContainer, BorderLayout.CENTER);

        return panel;
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
    
    // أضف هذا في نهاية ملف InventoryPanel.java
    class MaterialTextField extends JPanel {
        private final JTextField textField;
        private final JLabel label;
        private final Color activeColor = new Color(39, 174, 96);
        private final Color inactiveColor = Color.GRAY;

        public MaterialTextField(String labelText, String initialText) {
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

            label = new JLabel(labelText);
            label.setFont(getCairoFont(11));
            label.setForeground(inactiveColor);
            label.setHorizontalAlignment(JLabel.RIGHT); // محاذاة لليمين
            label.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 5));

            textField = new JTextField(initialText);
            textField.setFont(getCairoFont(13));
            textField.setHorizontalAlignment(JTextField.RIGHT);
            textField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

            textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, inactiveColor),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            textField.setBackground(Color.WHITE);

            textField.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent evt) {
                    label.setForeground(activeColor);
                    textField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, activeColor),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    ));
                }
                public void focusLost(java.awt.event.FocusEvent evt) {
                    label.setForeground(inactiveColor);
                    textField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, inactiveColor),
                        BorderFactory.createEmptyBorder(5, 5, 5, 5)
                    ));
                }
            });

            add(label, BorderLayout.NORTH);
            add(textField, BorderLayout.CENTER);
        }

        public String getText() { return textField.getText(); }
        public void setText(String t) { textField.setText(t); }
    }

}