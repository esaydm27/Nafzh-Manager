package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.table.JTableHeader;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import static nafzh.manager.NafzhManager.getCairoFont;

public class CustomersPanel extends JPanel {

    private final DatabaseManager dbManager;
    private JTable customersTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JCheckBox bulkStatusCheck;
    private List<Map<String, Object>> allCustomersRawData;
    private static final String[] COLUMN_NAMES = {"ID", "إسم العميل", "العنوان", "الهاتف", "الرصيد", "الحالة", "الوضع", "الإجراءات"};

    public CustomersPanel(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(Color.WHITE);
        
        applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        initializeTopPanel();
        initializeTable();
        loadData();
    }

    private void initializeTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(15, 10));
        topPanel.setBackground(Color.WHITE);
        topPanel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        leftActions.setBackground(Color.WHITE);

        bulkStatusCheck = new JCheckBox("تبديل حالة الكل (نشط/خامل)");
        bulkStatusCheck.setFont(getCairoFont(11));
        bulkStatusCheck.setBackground(Color.WHITE);
        bulkStatusCheck.addActionListener(e -> {
        toggleAllCustomersStatus();
        SwingUtilities.invokeLater(() -> bulkStatusCheck.setSelected(false));
         });

        searchField = new JTextField(20);
        searchField.setFont(getCairoFont(12));
        searchField.setPreferredSize(new Dimension(200, 35));
        searchField.setHorizontalAlignment(JTextField.RIGHT);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterData(); }
            public void removeUpdate(DocumentEvent e) { filterData(); }
            public void changedUpdate(DocumentEvent e) { filterData(); }
        });

        leftActions.add(bulkStatusCheck);
        leftActions.add(searchField);

        JButton btnAdd = new JButton("إضافة عميل");
        btnAdd.setFont(getCairoFont(12));
        btnAdd.setBackground(new Color(46, 204, 113));
        btnAdd.setForeground(Color.BLACK);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> showCustomerDialog(null));

        topPanel.add(leftActions, BorderLayout.WEST);
        topPanel.add(btnAdd, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    private void toggleAllCustomersStatus(boolean active) {
        int status = active ? 1 : 0;
        if (JOptionPane.showConfirmDialog(this, "هل تريد تغيير حالة جميع العملاء؟", "تحديث جماعي", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                for (Map<String, Object> customer : allCustomersRawData) {
                    dbManager.updateCustomerStatus((int) customer.get("id"), status);
                }
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "خطأ في التحديث: " + ex.getMessage());
            }
        }
    }

    private void initializeTable() {
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6 || column == 7;
            }
        };

        customersTable = new JTable(tableModel);
        customersTable.setRowHeight(30);
        customersTable.setFont(getCairoFont(11));
        customersTable.setShowGrid(true);
        customersTable.setGridColor(new Color(189, 195, 49));
        customersTable.setIntercellSpacing(new Dimension(0, 0));
        customersTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JTableHeader header = customersTable.getTableHeader();
        header.setFont(getCairoFont(12));
        header.setBackground(new Color(245, 245, 245));
        header.setForeground(Color.BLACK);
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                return this;
            }
        };

        for (int i = 0; i < 6; i++) {
            customersTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        customersTable.getColumnModel().getColumn(0).setMinWidth(0);
        customersTable.getColumnModel().getColumn(0).setMaxWidth(0);

        customersTable.getColumnModel().getColumn(6).setCellRenderer(new StatusPanelRenderer());
        customersTable.getColumnModel().getColumn(6).setCellEditor(new StatusPanelEditor());
        customersTable.getColumnModel().getColumn(7).setCellRenderer(new ActionPanelRenderer());
        customersTable.getColumnModel().getColumn(7).setCellEditor(new ActionPanelEditor());

        JScrollPane scrollPane = new JScrollPane(customersTable);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

        public void loadData() {
        // استدعاء الدالة الجديدة التي تجلب العملاء مع حالتهم
        allCustomersRawData = dbManager.getAllCustomersWithStatus(); 
        updateTable(allCustomersRawData);
    }


    private void updateTable(List<Map<String, Object>> data) {
        tableModel.setRowCount(0);
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        for (Map<String, Object> row : data) {
            double balance = (double) row.get("balance");
            // تأمين ضد القيم الفارغة للـ status
            int status = 1;
            if (row.get("status") != null) {
                status = (int) row.get("status");
            }
            
            tableModel.addRow(new Object[]{
                row.get("id"), row.get("name"), row.get("address"), row.get("phone"),
                df.format(balance), (balance < 0 ? "دائن" : "مدين"), status, ""
            });
        }
    }

    private void filterData() {
        String text = searchField.getText().trim();
        List<Map<String, Object>> filtered = allCustomersRawData.stream()
                .filter(c -> c.get("name").toString().contains(text))
                .collect(java.util.stream.Collectors.toList());
        updateTable(filtered);
    }

    private JPanel createClickablePanel(String text, Color textColor) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE); 
        JLabel lbl = new JLabel(text);
        lbl.setFont(getCairoFont(12));
        lbl.setForeground(textColor);   
        p.add(lbl);
        p.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return p;
    }

    class StatusPanelRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int status = (value instanceof Integer) ? (int) value : 1;
            Color textColor = (status == 1) ? new Color(27, 122, 68) : new Color(52, 152, 219);
            JPanel p = createClickablePanel(status == 1 ? "نشط" : "خامل", textColor);
            p.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            return p;
        }
    }

  
    class StatusPanelEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private int currentId;
        private int status;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentId = (int) table.getValueAt(row, 0);
            status = (value instanceof Integer) ? (int) value : 1;

            Color textColor = (status == 1) ? new Color(27, 122, 68) : new Color(231, 76, 60);
            String text = (status == 1) ? "نشط" : "خامل";
            JPanel p = createClickablePanel(text, textColor);
            p.setBackground(table.getSelectionBackground());

            p.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // الخطوة 1: إنهاء وضع التعديل فوراً
                    fireEditingStopped();

                    // الخطوة 2: تنفيذ التحديث وإعادة التحميل (تماماً مثل الزر الجماعي)
                    // نستخدم invokeLater لضمان أن تتم هذه العملية بعد انتهاء التعديل تماماً
                    SwingUtilities.invokeLater(() -> {
                        try {
                            int newStatus = (status == 1) ? 0 : 1;
                            dbManager.updateCustomerStatus(currentId, newStatus);

                            // استدعاء نفس الدالة التي تعمل بشكل فوري في التحديث الجماعي
                            loadData();

                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(CustomersPanel.this, "خطأ: " + ex.getMessage());
                        }
                    });
                }
            });
            return p;
        }

        @Override
        public Object getCellEditorValue() {
            // بما أننا سنعيد تحميل الجدول كله، يمكن إرجاع أي قيمة هنا
            return status;
        }
    }

    class ActionPanelRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel p = createDualActionPanel();
            p.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            return p;
        }
    }

    private JPanel createDualActionPanel() {
        JPanel container = new JPanel(new GridLayout(1, 2, 0, 0));
        container.setBackground(Color.WHITE);
        container.add(createClickablePanel("تعديل", new Color(52, 152, 219)));
        container.add(createClickablePanel("حذف", new Color(231, 76, 60)));
        return container;
    }

    class ActionPanelEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            int rowId = (int) table.getValueAt(row, 0);
            JPanel container = new JPanel(new GridLayout(1, 2, 0, 0));
            container.setBackground(table.getSelectionBackground());

            JPanel editPnl = createClickablePanel("تعديل", new Color(46, 204, 113));
            JPanel delPnl = createClickablePanel("حذف", new Color(231, 76, 60));
            
            editPnl.setBackground(table.getSelectionBackground());
            delPnl.setBackground(table.getSelectionBackground());

            editPnl.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    stopCellEditing();
                    Map<String, Object> data = allCustomersRawData.stream().filter(c -> (int)c.get("id") == rowId).findFirst().orElse(null);
                    showCustomerDialog(data);
                }
            });

            delPnl.addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    stopCellEditing();
                    if (JOptionPane.showConfirmDialog(null, "هل تريد حذف هذا العميل؟", "تأكيد الحذف", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        dbManager.deleteCustomer(rowId);
                        loadData();
                    }
                }
            });

            container.add(editPnl);
            container.add(delPnl);
            return container;
        }
        @Override public Object getCellEditorValue() { return ""; }
    }


    private void showCustomerDialog(Map<String, Object> existingData) {
    boolean isEdit = (existingData != null);
    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "تعديل عميل" : "إضافة عميل", true);
    dialog.setLayout(new GridBagLayout());
    dialog.setSize(400, 280); // حجم محدث لمظهر أفضل
    dialog.setResizable(false);
    dialog.setLocationRelativeTo(this);
    dialog.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    // استخدام لوحة رئيسية مع حدود لتحسين التنسيق
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(new EmptyBorder(15, 15, 15, 15));
    panel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    dialog.add(panel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);

    // --- عمود العناوين (اليمين) ---
    gbc.gridx = 0; // العمود الأول (على اليمين)
    gbc.weightx = 0.0; // لا يتمدد
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.LINE_START; // محاذاة لليمين

    JLabel lblName = new JLabel("إسم العميل:");
    lblName.setFont(getCairoFont(11));
    gbc.gridy = 0;
    panel.add(lblName, gbc);

    JLabel lblAddress = new JLabel("العنوان:");
    lblAddress.setFont(getCairoFont(11));
    gbc.gridy = 1;
    panel.add(lblAddress, gbc);

    JLabel lblPhone = new JLabel("رقم الهاتف:");
    lblPhone.setFont(getCairoFont(11));
    gbc.gridy = 2;
    panel.add(lblPhone, gbc);

    JLabel lblBalance = new JLabel("الرصيد الافتتاحي:");
    lblBalance.setFont(getCairoFont(11));
    gbc.gridy = 3;
    panel.add(lblBalance, gbc);

    // --- عمود حقول الإدخال (اليسار) ---
    gbc.gridx = 1; // العمود الثاني (على اليسار)
    gbc.weightx = 1.0; // يتمدد ليملأ المساحة
    gbc.fill = GridBagConstraints.HORIZONTAL; // يملأ العرض

    JTextField txtName = new JTextField(isEdit ? existingData.get("name").toString() : "", 15);
    txtName.setFont(getCairoFont(11));
    gbc.gridy = 0;
    panel.add(txtName, gbc);

    JTextField txtAddress = new JTextField(isEdit ? existingData.get("address").toString() : "", 15);
    txtAddress.setFont(getCairoFont(11));
    gbc.gridy = 1;
    panel.add(txtAddress, gbc);

    JTextField txtPhone = new JTextField(isEdit ? existingData.get("phone").toString() : "", 15);
    txtPhone.setFont(getCairoFont(11));
    gbc.gridy = 2;
    panel.add(txtPhone, gbc);

    JTextField txtBalance = new JTextField(isEdit ? (existingData.get("balance") != null ? existingData.get("balance").toString() : "0") : "0", 15);
    txtBalance.setFont(getCairoFont(11));
    gbc.gridy = 3;
    panel.add(txtBalance, gbc);

    // --- زر الحفظ ---
    JButton btnSave = new JButton(isEdit ? "تعديل البيانات" : "إضافة العميل");
    btnSave.setFont(getCairoFont(12));
    btnSave.setBackground(new Color(41, 128, 185));
    btnSave.setForeground(Color.WHITE);
    btnSave.setFocusPainted(false);

    gbc.gridy = 4;
    gbc.gridx = 0;
    gbc.gridwidth = 2; // يمتد على كلا العمودين
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets = new Insets(15, 5, 5, 5); // مسافة إضافية فوق الزر
    panel.add(btnSave, gbc);

    btnSave.addActionListener(e -> {
        try {
            if (isEdit) {
                dbManager.updateCustomer((int) existingData.get("id"), txtName.getText(), txtAddress.getText(), txtPhone.getText(), Double.parseDouble(txtBalance.getText()));
            } else {
                dbManager.addCustomer(txtName.getText(), txtAddress.getText(), txtPhone.getText(), Double.parseDouble(txtBalance.getText()), 1);
            }
            loadData();
            dialog.dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog, "يرجى التأكد من صحة البيانات المدخلة");
        }
    });

    dialog.setVisible(true);
}
    
    private void toggleAllCustomersStatus() {
    int confirm = JOptionPane.showConfirmDialog(this, 
        "هل تريد عكس حالة جميع العملاء؟\n(النشط سيصبح خامل، والخامل سيصبح نشط)", 
        "تحديث جماعي", 
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );
    
    if (confirm == JOptionPane.YES_OPTION) {
        try {
            // استدعاء دالة قاعدة البيانات الجديدة
            if (dbManager.toggleAllCustomerStatuses()) {
                loadData(); // إعادة تحميل البيانات لإظهار التغييرات
            } else {
                JOptionPane.showMessageDialog(this, "فشل تحديث حالة العملاء.", "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ في التحديث: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }
}

    public void loadCustomerData() {
        loadData();
    }

    private void addFormField(JDialog dialog, String label, JTextField field, GridBagConstraints gbc, int y) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(getCairoFont(11));
        gbc.gridy = y; gbc.gridx = 1; dialog.add(lbl, gbc);
        gbc.gridx = 0; dialog.add(field, gbc);
    }
}