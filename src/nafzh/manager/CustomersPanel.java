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
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import javax.swing.event.DocumentListener;
import static nafzh.manager.NafzhManager.getCairoFont;

public class CustomersPanel extends JPanel {
    
    

    // افترضت وجود هذه الكائنات بناءً على سياق الكود السابق
    
    private Runnable loadDataCallback; 
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
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), isEdit ? "تعديل بيانات العميل" : "إضافة عميل جديد", true);

        // تصميم طولي وأنيق (بما أن الحقول أصبحت تحت بعضها)
        dialog.setSize(350, 480); 
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // ترتيب عمودي
        panel.setBorder(new EmptyBorder(30, 25, 30, 25)); // هوامش مريحة
        panel.setBackground(Color.WHITE);
        panel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // --- إنشاء الحقول المودرن ---
        MaterialTextField txtName = new MaterialTextField("اسم العميل", isEdit ? existingData.get("name").toString() : "");
        MaterialTextField txtAddress = new MaterialTextField("العنوان", isEdit ? existingData.get("address").toString() : "");
        MaterialTextField txtPhone = new MaterialTextField("رقم الهاتف", isEdit ? existingData.get("phone").toString() : "");
        String balanceStr = isEdit && existingData.get("balance") != null ? existingData.get("balance").toString() : "0.0";
        MaterialTextField txtBalance = new MaterialTextField("الرصيد الافتتاحي", balanceStr);

        // إضافة مسافات بين الحقول
        panel.add(txtName);
        panel.add(Box.createVerticalStrut(15));
        panel.add(txtAddress);
        panel.add(Box.createVerticalStrut(15));
        panel.add(txtPhone);
        panel.add(Box.createVerticalStrut(15));
        panel.add(txtBalance);
        panel.add(Box.createVerticalStrut(25)); // مسافة قبل الزر

        // --- زر الحفظ ---
        JButton btnSave = new JButton("حفظ البيانات");
        btnSave.setFont(getCairoFont(14));
        btnSave.setBackground(new Color(39, 174, 96));
        btnSave.setForeground(Color.BLACK);
        btnSave.setFocusPainted(false);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // جعل الزر بعرض كامل
        btnSave.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45)); 

        btnSave.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "اسم العميل مطلوب", "تنبيه", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                double balance = Double.parseDouble(txtBalance.getText().trim());
                if (isEdit) {
                    dbManager.updateCustomer((int) existingData.get("id"), name, txtAddress.getText(), txtPhone.getText(), balance);
                } else {
                    dbManager.addCustomer(name, txtAddress.getText(), txtPhone.getText(), balance, 0);
                }
                loadData();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "الرصيد يجب أن يكون رقماً صحيحاً", "خطأ", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "حدث خطأ: " + ex.getMessage());
            }
        });

        panel.add(btnSave);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    class FloatingTextField extends JPanel {
        private JTextField textField;
        private JLabel label;
        private String placeholder;
        private boolean isFloating = false;

        public FloatingTextField(String placeholder) {
            this.placeholder = placeholder;
            setLayout(null);
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(250, 50));

            label = new JLabel(placeholder);
            label.setFont(new Font("Cairo", Font.PLAIN, 14));
            label.setForeground(Color.GRAY);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setBounds(5, 15, 230, 20);
            add(label);

            textField = new JTextField();
            textField.setFont(new Font("Cairo", Font.PLAIN, 14));
            textField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
            textField.setBackground(new Color(0, 0, 0, 0));
            textField.setOpaque(false);
            textField.setBounds(5, 15, 230, 30);
            textField.setHorizontalAlignment(JTextField.RIGHT);
            textField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            add(textField);

            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    floatLabel(true);
                    textField.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(39, 174, 96)));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (textField.getText().isEmpty()) {
                        floatLabel(false);
                    }
                    textField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
                }
            });
        }

        private void floatLabel(boolean floating) {
            if (floating && !isFloating) {
                label.setFont(new Font("Cairo", Font.BOLD, 11));
                label.setForeground(new Color(39, 174, 96));
                label.setBounds(5, 0, 230, 15);
                isFloating = true;
            } else if (!floating && isFloating) {
                label.setFont(new Font("Cairo", Font.PLAIN, 14));
                label.setForeground(Color.GRAY);
                label.setBounds(5, 15, 230, 20);
                isFloating = false;
            }
            repaint();
        }

        public String getText() { return textField.getText(); }
        public void setText(String text) { 
            textField.setText(text);
            if (!text.isEmpty()) floatLabel(true);
        }
    }


    private JTextField createStyledTextField(String text) {
     JTextField field = new JTextField(text); // إزالة عدد الأعمدة هنا ليعتمد على Dimension
     field.setFont(getCairoFont(13)); // خط أوضح قليلاً
     field.setHorizontalAlignment(JTextField.RIGHT);
     field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
     // الارتفاع المناسب هو 35 بكسل، والعرض سيأخذه من الـ GridBagLayout
     field.setPreferredSize(new Dimension(220, 35)); 
     return field;
 }

    private void addFormRow(JPanel panel, String labelText, JComponent field, GridBagConstraints gbc, int y) {
     gbc.gridy = y;

     // التسمية (Label) - تأخذ مساحة ثابتة
     gbc.gridx = 0;
     gbc.weightx = 0.0; // لا تتمدد
     JLabel label = new JLabel(labelText);
     label.setFont(getCairoFont(12));
     label.setPreferredSize(new Dimension(100, 30)); // عرض ثابت للتسميات لضمان المحاذاة
     panel.add(label, gbc);

     // الحقل (Field) - يتمدد ليملأ الباقي
     gbc.gridx = 1;
     gbc.weightx = 1.0; // يتمدد
     panel.add(field, gbc);
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

    class MaterialTextField extends JPanel {
    private final JTextField textField;
    private final JLabel label;
    private final Color activeColor = new Color(39, 174, 96); // أخضر عند التركيز
    private final Color inactiveColor = Color.GRAY;
    
    public MaterialTextField(String labelText, String initialText) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0)); // مسافات خارجية
        
        // العنوان (Label) الذي سيظهر بالأعلى
        label = new JLabel(labelText);
        label.setFont(getCairoFont(11));
        label.setForeground(inactiveColor);
        label.setHorizontalAlignment(JLabel.RIGHT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 5, 2, 5));
        
        // حقل النص
        textField = new JTextField(initialText);
        textField.setFont(getCairoFont(13));
        textField.setHorizontalAlignment(JTextField.RIGHT);
        textField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        // إزالة الحدود الافتراضية واستبدالها بخط سفلي فقط (ستايل أندرويد)
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, inactiveColor),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        textField.setBackground(Color.WHITE);
        
        // إضافة مستمع للأحداث لتغيير اللون عند التركيز
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                label.setForeground(activeColor);
                textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, activeColor), // خط سميك وملون
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                label.setForeground(inactiveColor);
                textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, inactiveColor), // عودة للخط الرفيع
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
                ));
            }
        });
        
        add(label, BorderLayout.NORTH);
        add(textField, BorderLayout.CENTER);
    }
    
    public String getText() {
        return textField.getText();
    }
    
    public void setText(String t) {
        textField.setText(t);
    }
}

}