package nafzh.manager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import static nafzh.manager.NafzhManager.getCairoFont;

public class UserManagementDialog extends JDialog {
    private final DatabaseManager dbManager;
    private DefaultTableModel tableModel;
    private JTable usersTable;

    // نفس ألوان نافذة الدخول لتوحيد التصميم
    private final Color DARK_BG = new Color(44, 62, 80);
    private final Color INPUT_BG = new Color(255, 255, 255);
    private final Color HEADER_BG = new Color(52, 73, 94); // لون رأس الجدول
    private final Color ACCENT_GREEN = new Color(39, 174, 96);
    private final Color ACCENT_RED = new Color(192, 57, 43);
    private final Color TEXT_COLOR = Color.WHITE;

    public UserManagementDialog(Frame parent, DatabaseManager dbManager) {
        super(parent, "إدارة مستخدمي النظام", true);
        this.dbManager = dbManager;
        initializeUI();
        loadUsers();
    }

    private void initializeUI() {
        setSize(600, 500);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // تعيين خلفية النافذة
        getContentPane().setBackground(DARK_BG);
        


        // --- 1. الجدول (في المنتصف) ---
        String[] cols = {"ID", "اسم المستخدم", "الصلاحية"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        
        usersTable = new JTable(tableModel);
        usersTable.setRowHeight(35);
        usersTable.setFont(getCairoFont(13f));
        usersTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        usersTable.setGridColor(new Color(200, 200, 200));
        
        // تنسيق رأس الجدول (Header)
        JTableHeader header = usersTable.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(HEADER_BG);
                setForeground(Color.WHITE);
                setFont(getCairoFont(13f).deriveFont(Font.BOLD));
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY));
                return this;
            }
        });

        // تنسيق الخلايا (توسيط)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for(int i=0; i<usersTable.getColumnCount(); i++) {
            usersTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(usersTable);
        scrollPane.getViewport().setBackground(Color.WHITE); // خلفية منطقة البيانات بيضاء
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // هامش حول الجدول
        scrollPane.setBackground(DARK_BG); // خلفية الإطار الخارجي داكنة
        
        add(scrollPane, BorderLayout.CENTER);

        // --- 2. قسم الإضافة (في الأسفل) ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(DARK_BG);
        bottomPanel.setBorder(new EmptyBorder(10, 20, 20, 20)); // هوامش خارجية

        // عنوان القسم
        JLabel titleLabel = new JLabel("إضافة مستخدم جديد");
        titleLabel.setFont(getCairoFont(14f).deriveFont(Font.BOLD));
        titleLabel.setForeground(new Color(52, 152, 219)); // أزرق فاتح
        titleLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        bottomPanel.add(titleLabel, BorderLayout.NORTH);

        // لوحة الحقول
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        fieldsPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // الصف الأول: اسم المستخدم
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
        JLabel userLabel = new JLabel("اسم المستخدم:");
        userLabel.setFont(getCairoFont(13f));
        userLabel.setForeground(TEXT_COLOR);
        fieldsPanel.add(userLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.9;
        JTextField userField = createStyledTextField();
        fieldsPanel.add(userField, gbc);

        // الصف الثاني: كلمة المرور
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.1;
        JLabel passLabel = new JLabel("كلمة المرور:");
        passLabel.setFont(getCairoFont(13f));
        passLabel.setForeground(TEXT_COLOR);
        fieldsPanel.add(passLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.9;
        JPasswordField passField = new JPasswordField();
        styleTextField(passField); // تطبيق نفس الستايل
        fieldsPanel.add(passField, gbc);

        // الصف الثالث: الأزرار
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.insets = new Insets(15, 5, 0, 5);
        
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false);
        
        JButton deleteBtn = new JButton("حذف المحدد");
        styleButton(deleteBtn, ACCENT_RED);
        
        JButton addBtn = new JButton("إضافة");
        styleButton(addBtn, ACCENT_GREEN);

        btnPanel.add(deleteBtn); // يمين (بسبب RTL)
        btnPanel.add(addBtn);    // يسار

        fieldsPanel.add(btnPanel, gbc);

        bottomPanel.add(fieldsPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- برمجة الأزرار ---
        addBtn.addActionListener(e -> {
            String u = userField.getText();
            String p = new String(passField.getPassword());
            if(!u.isEmpty() && !p.isEmpty()) {
                if (p.length() < 6) {
                    JOptionPane.showMessageDialog(this, "كلمة المرور ضعيفة (أقل من 6 أحرف)", "تنبيه", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if(dbManager.addUser(u, p, "user")) {
                    loadUsers();
                    userField.setText("");
                    passField.setText("");
                    JOptionPane.showMessageDialog(this, "تمت الإضافة بنجاح");
                } else {
                    JOptionPane.showMessageDialog(this, "اسم المستخدم موجود مسبقاً", "خطأ", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "يرجى ملء البيانات", "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if(row != -1) {
                int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
                String role = tableModel.getValueAt(row, 2).toString();
                
                if ("admin".equals(role)) {
                    JOptionPane.showMessageDialog(this, "لا يمكن حذف حساب المدير الرئيسي!", "خطأ", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (JOptionPane.showConfirmDialog(this, "هل أنت متأكد من الحذف؟", "تأكيد", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if(dbManager.deleteUser(id)) loadUsers();
                }
            } else {
                JOptionPane.showMessageDialog(this, "يرجى تحديد مستخدم من الجدول", "تنبيه", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    // --- دوال مساعدة للتصميم ---
    
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        styleTextField(field);
        return field;
    }

    private void styleTextField(JTextField field) {
        field.setFont(getCairoFont(14f));
        field.setPreferredSize(new Dimension(0, 35));
        field.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        field.setBackground(INPUT_BG);
        // لغة الكتابة يسار لليمين (انجليزي/أرقام)
        field.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    }

    private void styleButton(JButton btn, Color bgColor) {
        btn.setFont(getCairoFont(13f).deriveFont(Font.BOLD));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 40));
        
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.brighter()); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
    }

    private void loadUsers() {
        tableModel.setRowCount(0);
        List<String[]> users = dbManager.getAllUsers();
        for(String[] u : users) tableModel.addRow(u);
    }
}
