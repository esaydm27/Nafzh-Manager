package nafzh.manager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
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

    private static final int ACTION_COL_INDEX = 7;
    // توحيد لون الشبكة (الزيتوني)
    private static final Color GRID_COLOR = new Color(189, 195, 49); 

    // --- أيقونة السهم للأسفل (v) عند التوسيع ---
    private static final Icon ARROW_DOWN = new Icon() {
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(80, 80, 80)); 
            
            // رسم حرف v
            int w = 10;
            int h = 6;
            int xStart = x + (getIconWidth() - w)/2;
            int yStart = y + (getIconHeight() - h)/2;
            
            g2.drawLine(xStart, yStart, xStart + w/2, yStart + h);
            g2.drawLine(xStart + w/2, yStart + h, xStart + w, yStart);
            g2.dispose();
        }
        @Override public int getIconWidth() { return 20; }
        @Override public int getIconHeight() { return 20; }
    };

    // --- أيقونة السهم لليسار (<) عند الطي ---
    private static final Icon ARROW_LEFT = new Icon() {
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(80, 80, 80));
            
            // رسم علامة <
            int w = 6;
            int h = 10;
            int xStart = x + (getIconWidth() - w)/2;
            int yStart = y + (getIconHeight() - h)/2;
            
            g2.drawLine(xStart + w, yStart, xStart, yStart + h/2);
            g2.drawLine(xStart, yStart + h/2, xStart + w, yStart + h);
            g2.dispose();
        }
        @Override public int getIconWidth() { return 20; }
        @Override public int getIconHeight() { return 20; }
    };

    public InventoryPanel(DatabaseManager dbManager, NafzhManager parentFrame) {
        this.dbManager = dbManager;
        this.parentFrame = parentFrame;
        loadInventoryData();
        initializePanel();
    }

    private void initializePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setName("Inventory");

        // --- العنوان والزر ---
        JLabel titleLabel = new JLabel("إدارة المخزون والأصناف", SwingConstants.RIGHT);
        titleLabel.setFont(getCairoFont(21f));
        titleLabel.setForeground(new Color(50, 50, 50));

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setOpaque(false);
        northPanel.add(titleLabel, BorderLayout.EAST);

        JButton addButton = new JButton("إضافة صنف");
        addButton.setFont(getCairoFont(12f));
        addButton.setBackground(new Color(76, 175, 80));
        addButton.setForeground(Color.BLACK);
        addButton.setFocusPainted(false);
        addButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        addButton.addActionListener(e -> showProductDialog(null));

        northPanel.add(addButton, BorderLayout.WEST);
        add(northPanel, BorderLayout.NORTH);

        // --- الجدول ---
        inventoryTable = new JTable(tableModel);
        customizeTable(inventoryTable);
        setupActionColumn();
        
        // --- تفعيل النقر على السهم/الفئة للطي والتوسيع ---
        inventoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = inventoryTable.rowAtPoint(e.getPoint());
                int col = inventoryTable.columnAtPoint(e.getPoint());
                
                // النقر على عمود الفئة (العمود 0)
                if (row >= 0 && col == 0) {
                    String category = (String) tableModel.getValueAt(row, 0);
                    // نتأكد أنه الصف الأول في المجموعة (رأس الفئة)
                    String prevCategory = (row > 0) ? (String) tableModel.getValueAt(row - 1, 0) : "";
                    
                    if (!category.equals(prevCategory)) {
                        tableModel.toggleExpandState(category);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(inventoryTable);
        // جعل إطار الجدول بنفس لون الشبكة
        scrollPane.setBorder(BorderFactory.createLineBorder(GRID_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void loadInventoryData() {
        productsData = dbManager.getAllProducts();
        // ترتيب البيانات حسب الفئة
        productsData.sort((p1, p2) -> {
            String c1 = (String) p1.get("category");
            String c2 = (String) p2.get("category");
            return c1.compareTo(c2);
        });

        tableModel = new ExpandableInventoryTableModel(productsData);
        if (inventoryTable != null) {
            inventoryTable.setModel(tableModel);
            customizeTable(inventoryTable);
            setupActionColumn();
        }
        parentFrame.updateDashboardData();
    }

    private void customizeTable(JTable table) {
        table.setRowHeight(45); 
        table.setFont(getCairoFont(12f));
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.setShowGrid(false); // سنرسم نحن الحدود يدوياً
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);

        // --- تنسيق الهيدر ---
        JTableHeader header = table.getTableHeader();
        header.setFont(getCairoFont(12f).deriveFont(Font.BOLD));
        header.setBackground(new Color(240, 240, 240));
        header.setForeground(new Color(50, 50, 50));
        header.setReorderingAllowed(false);
        
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setBackground(new Color(240, 240, 240));
                l.setHorizontalAlignment(JLabel.CENTER);
                // استخدام GRID_COLOR للحدود
                l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, GRID_COLOR),
                    BorderFactory.createEmptyBorder(10, 5, 10, 5)
                ));
                return l;
            }
        };

        // --- الريندر المجمع (Group Renderer) ---
        DefaultTableCellRenderer groupingRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                String currentCategory = (String) table.getValueAt(row, 0);
                String prevCategory = (row > 0) ? (String) table.getValueAt(row - 1, 0) : "";
                String nextCategory = (row < table.getRowCount() - 1) ? (String) table.getValueAt(row + 1, 0) : "";
                
                boolean isStartOfGroup = !currentCategory.equals(prevCategory);
                boolean isEndOfGroup = !currentCategory.equals(nextCategory);

                // --- ضبط الحدود: أسفل ويمين دائماً ---
                // نستخدم GRID_COLOR الزيتوني الواضح
                
                if (column == 0) { // عمود الفئة
                    l.setHorizontalAlignment(JLabel.RIGHT); 
                    l.setHorizontalTextPosition(SwingConstants.LEFT);
                    
                    l.setFont(getCairoFont(13f).deriveFont(Font.BOLD));
                    l.setForeground(new Color(41, 128, 185)); // لون أزرق للفئة
                    
                    if (isStartOfGroup) {
                        l.setText(currentCategory);
                        if (tableModel.hasMoreProducts(currentCategory)) {
                            l.setIcon(tableModel.isExpanded(currentCategory) ? ARROW_DOWN : ARROW_LEFT);
                        } else {
                            l.setIcon(null);
                        }
                        l.setBackground(new Color(250, 250, 250));
                        l.setOpaque(true);
                        
                        // حدود الفئة:
                        // أعلى: إذا كانت بداية مجموعة، ارسم خط علوي أيضاً لتأكيد الفصل
                        // أسفل: إذا كانت نهاية مجموعة، ارسم خط سفلي
                        // يمين: دائماً خط يمين
                        l.setBorder(BorderFactory.createCompoundBorder(
                             BorderFactory.createMatteBorder(1, 0, isEndOfGroup ? 1 : 0, 1, GRID_COLOR),
                             BorderFactory.createEmptyBorder(0, 10, 0, 0)
                        ));
                    } else {
                        // تفريغ الخلية
                        l.setText("");
                        l.setIcon(null);
                        l.setBackground(Color.WHITE);
                        l.setOpaque(true);
                        // حدود للخلايا الفارغة: يمين فقط، وأسفل إذا كانت نهاية المجموعة
                        l.setBorder(BorderFactory.createMatteBorder(0, 0, isEndOfGroup ? 1 : 0, 1, GRID_COLOR));
                    }
                } else { // باقي الأعمدة
                    l.setHorizontalAlignment(JLabel.CENTER);
                    l.setForeground(Color.BLACK);
                    l.setIcon(null);
                    l.setFont(getCairoFont(12f));
                    l.setBackground(isSelected ? new Color(235, 245, 255) : Color.WHITE);
                    // رسم حدود واضحة لكل خلية (أسفل ويمين)
                    l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, GRID_COLOR));
                }
                return l;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            if (i != ACTION_COL_INDEX) {
                table.getColumnModel().getColumn(i).setCellRenderer(groupingRenderer);
            }
        }
        
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(ACTION_COL_INDEX).setPreferredWidth(140);
        table.getColumnModel().getColumn(ACTION_COL_INDEX).setMinWidth(140);
    }

    private void setupActionColumn() {
        TableColumn actionColumn = inventoryTable.getColumnModel().getColumn(ACTION_COL_INDEX);
        // تأكد من استخدام TableActionRendererEditor التي تستخدم GRID_COLOR أيضاً داخلياً
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
        Map<String, Object> productData = tableModel.getProductDataAt(rowIndex);
        if (productData != null) showProductDialog(productData);
    }

    private void handleDeleteAction(int rowIndex) {
        Integer productId = tableModel.getProductIdAt(rowIndex);
        if (productId != null) {
            if (JOptionPane.showConfirmDialog(this, "هل أنت متأكد من حذف هذا الصنف؟", "تأكيد الحذف", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                if (dbManager.deleteProduct(productId)) {
                    loadInventoryData();
                }
            }
        }
    }

    private void showProductDialog(Map<String, Object> existingData) {
        boolean isEdit = (existingData != null);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "تعديل صنف" : "إضافة صنف جديد", true);
        dialog.setSize(400, 650); 
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        panel.setBackground(Color.WHITE);
        panel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        String nameVal = isEdit ? (String) existingData.get("name") : "";
        String catVal = isEdit ? (String) existingData.get("category") : "";
        String unitVal = isEdit && existingData.containsKey("unit") ? (String) existingData.get("unit") : "قطعة";
        String qtyVal = isEdit ? String.valueOf(existingData.get("current_quantity")) : "0";
        String purchVal = isEdit ? String.valueOf(existingData.get("purchase_price")) : "0.0";
        String saleVal = isEdit ? String.valueOf(existingData.get("sale_price")) : "0.0";

        MaterialTextField txtName = new MaterialTextField("اسم الصنف", nameVal);
        JPanel categoryPanel = createComboPanelWithButton("الفئة", dbManager.getAllCategories(), catVal, dialog, true);
        JPanel unitPanel = createComboPanelWithButton("الوحدة", dbManager.getAllUnits(), unitVal, dialog, false);
        MaterialTextField txtQty = new MaterialTextField("الكمية الحالية", qtyVal);
        MaterialTextField txtPurch = new MaterialTextField("سعر الشراء", purchVal);
        MaterialTextField txtSale = new MaterialTextField("سعر البيع", saleVal);

        panel.add(txtName); panel.add(Box.createVerticalStrut(15));
        panel.add(categoryPanel); panel.add(Box.createVerticalStrut(15));
        panel.add(unitPanel); panel.add(Box.createVerticalStrut(15));
        panel.add(txtQty); panel.add(Box.createVerticalStrut(15));
        panel.add(txtPurch); panel.add(Box.createVerticalStrut(15));
        panel.add(txtSale); panel.add(Box.createVerticalStrut(25));

        JButton btnSave = new JButton("حفظ الصنف");
        btnSave.setFont(getCairoFont(14));
        btnSave.setBackground(new Color(39, 174, 96));
        btnSave.setForeground(Color.BLACK);
        btnSave.setFocusPainted(false);
        btnSave.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        btnSave.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                if (name.isEmpty()) { JOptionPane.showMessageDialog(dialog, "الاسم مطلوب"); return; }
                
                JComboBox<String> catCombo = (JComboBox<String>) ((JPanel) categoryPanel.getComponent(1)).getComponent(0);
                String cat = (String) catCombo.getSelectedItem();
                JComboBox<String> unitCombo = (JComboBox<String>) ((JPanel) unitPanel.getComponent(1)).getComponent(0);
                String unit = (String) unitCombo.getSelectedItem();
                int qty = Integer.parseInt(txtQty.getText().trim());
                double pPrice = Double.parseDouble(txtPurch.getText().trim());
                double sPrice = Double.parseDouble(txtSale.getText().trim());

                if (isEdit) {
                    dbManager.updateProduct((int) existingData.get("id"), name, cat, unit, qty, pPrice, sPrice);
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

    private JPanel createComboPanelWithButton(String labelText, java.util.Vector<String> items, String selectedItem, JDialog parentDialog, boolean hasAddButton) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        JLabel label = new JLabel(labelText);
        label.setFont(getCairoFont(11));
        label.setForeground(Color.GRAY);
        label.setHorizontalAlignment(JLabel.RIGHT);
        
        JPanel comboContainer = new JPanel(new BorderLayout(5, 0)); 
        comboContainer.setBackground(Color.WHITE);
        
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setEditable(true); 
        comboBox.setFont(getCairoFont(13));
        if (selectedItem != null) comboBox.setSelectedItem(selectedItem);
        
        Component editorComponent = comboBox.getEditor().getEditorComponent();
        if (editorComponent instanceof JTextField) {
            ((JTextField) editorComponent).setHorizontalAlignment(JTextField.RIGHT);
        }
        
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                lbl.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                return lbl;
            }
        });

        comboContainer.add(comboBox, BorderLayout.CENTER);

        if (hasAddButton) {
            JButton btnAdd = new JButton("+");
            btnAdd.setFont(new Font("Arial", Font.BOLD, 14));
            btnAdd.setBackground(new Color(52, 152, 219));
            btnAdd.setForeground(Color.WHITE);
            btnAdd.addActionListener(e -> {
                String newItem = JOptionPane.showInputDialog(parentDialog, "أدخل اسم " + labelText + " الجديدة:");
                if (newItem != null && !newItem.trim().isEmpty()) {
                    boolean success = labelText.equals("الفئة") ? dbManager.addCategory(newItem.trim()) : true;
                    if (success) {
                        comboBox.addItem(newItem.trim());
                        comboBox.setSelectedItem(newItem.trim());
                    }
                }
            });
            comboContainer.add(btnAdd, BorderLayout.WEST); 
        }
        
        comboContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        panel.add(label, BorderLayout.NORTH);
        panel.add(comboContainer, BorderLayout.CENTER);
        return panel;
    }

    class MaterialTextField extends JPanel {
        private final JTextField textField;
        public MaterialTextField(String labelText, String initialText) {
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            JLabel label = new JLabel(labelText);
            label.setFont(getCairoFont(11));
            label.setForeground(Color.GRAY);
            label.setHorizontalAlignment(JLabel.RIGHT);
            textField = new JTextField(initialText);
            textField.setFont(getCairoFont(13));
            textField.setHorizontalAlignment(JTextField.RIGHT);
            textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            textField.setBackground(Color.WHITE);
            add(label, BorderLayout.NORTH);
            add(textField, BorderLayout.CENTER);
        }
        public String getText() { return textField.getText(); }
    }
}
