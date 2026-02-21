package nafzh.manager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import static nafzh.manager.NafzhManager.getCairoFont;

public class LoginDialog extends JDialog {
    private final DatabaseManager dbManager;
    private JTextField userField;
    private JPasswordField passField;
    private boolean isAuthenticated = false;
    private NafzhManager parentManager;

    private final Color DARK_BG = new Color(44, 62, 80);
    private final Color INPUT_BG = new Color(255, 255, 255);
    private final Color ACCENT_BLUE = new Color(20, 90, 50);
    private final Color ACCENT_GREEN = new Color(20, 90, 50);
    private final Color TEXT_COLOR = Color.WHITE;

    public LoginDialog(Frame parent, DatabaseManager dbManager) {
        super(parent, "تسجيل الدخول - Nafzh Manager", true);
        this.dbManager = dbManager;
        if (parent instanceof NafzhManager) {
            this.parentManager = (NafzhManager) parent;
        }
        initializeUI();
    }

    private void initializeUI() {
        setSize(400, 370);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        boolean isFirstRun = dbManager.isUsersTableEmpty();
        String titleText = isFirstRun ? "إنشاء حساب السوبر أدمن" : "تسجيل الدخول";
        String subTitleText = isFirstRun ? "مرحباً بك في أول تشغيل للنظام" : "مرحباً بعودتك";

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(DARK_BG);
        mainPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(getCairoFont(20f).deriveFont(Font.BOLD));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subTitleLabel = new JLabel(subTitleText);
        subTitleLabel.setFont(getCairoFont(12f));
        subTitleLabel.setForeground(new Color(200, 200, 200));
        subTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(subTitleLabel);
        mainPanel.add(Box.createVerticalStrut(30));

        JPanel userPanel = createInputPanel("اسم المستخدم", false);
        userField = (JTextField) userPanel.getClientProperty("field");
        userField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                      (c >= '0' && c <= '9') || c == '_' || c == '.' ||
                      c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE)) {
                    e.consume();
                    getToolkit().beep();
                }
            }
        });
        mainPanel.add(userPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        JPanel passPanelWrapper = createInputPanel("كلمة المرور", true);
        passField = (JPasswordField) passPanelWrapper.getClientProperty("field");
        mainPanel.add(passPanelWrapper);
        mainPanel.add(Box.createVerticalStrut(30));

        JButton actionBtn = new JButton(isFirstRun ? "إنشاء الحساب" : "دخول");
        styleButton(actionBtn, isFirstRun ? ACCENT_GREEN : ACCENT_BLUE);

        JButton closeBtn = new JButton("خروج");
        styleButton(closeBtn, new Color(192, 57, 43));

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(closeBtn);
        btnPanel.add(actionBtn);

        mainPanel.add(btnPanel);
        add(mainPanel, BorderLayout.CENTER);

        actionBtn.addActionListener(e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword()).trim();

            if (user.isEmpty() || pass.isEmpty()) {
                if (parentManager != null) parentManager.showToast("يرجى ملء كافة الحقول", true);
                return;
            }

            if (isFirstRun) {
                if (pass.length() < 6) {
                    if (parentManager != null) parentManager.showToast("كلمة المرور يجب أن تكون 6 أحرف فأكثر", true);
                    return;
                }
                
                if (dbManager.addUser(user, pass, "super_admin")) {
                    DatabaseManager.User superAdminUser = dbManager.checkUserCredentials(user, pass);
                    if (superAdminUser != null) {
                        NafzhManager.setCurrentUser(superAdminUser.id, superAdminUser.username, superAdminUser.role);
                        isAuthenticated = true;
                        dispose();
                    } else {
                        if (parentManager != null) parentManager.showToast("حدث خطأ أثناء تسجيل الدخول التلقائي", true);
                    }
                } else {
                    if (parentManager != null) parentManager.showToast("فشل إنشاء حساب السوبر أدمن", true);
                }
            } else {
                DatabaseManager.User authenticatedUser = dbManager.checkUserCredentials(user, pass);
                if (authenticatedUser != null) {
                    NafzhManager.setCurrentUser(authenticatedUser.id, authenticatedUser.username, authenticatedUser.role);
                    isAuthenticated = true;
                    dispose();
                } else {
                    if (parentManager != null) parentManager.showToast("بيانات الدخول غير صحيحة", true);
                }
            }
        });

        closeBtn.addActionListener(e -> System.exit(0));
        getRootPane().setDefaultButton(actionBtn);
    }

    private JPanel createInputPanel(String labelText, boolean isPassword) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(new Dimension(400, 60));
        panel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel label = new JLabel(labelText);
        label.setFont(getCairoFont(12f));
        label.setForeground(TEXT_COLOR);
        label.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel fieldContainer = new JPanel(new BorderLayout());
        fieldContainer.setBackground(INPUT_BG);
        fieldContainer.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        JTextField field;
        if (isPassword) {
            JPasswordField pf = new JPasswordField();
            pf.setEchoChar('•');
            JToggleButton eyeBtn = new JToggleButton("👁");
            eyeBtn.setContentAreaFilled(false);
            eyeBtn.setBorderPainted(false);
            eyeBtn.setFocusPainted(false);
            eyeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            eyeBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            eyeBtn.addActionListener(e -> {
                if (eyeBtn.isSelected()) pf.setEchoChar((char) 0);
                else pf.setEchoChar('•');
            });
            fieldContainer.add(eyeBtn, BorderLayout.EAST);
            field = pf;
        } else {
            field = new JTextField();
        }

        field.setBorder(null);
        field.setFont(getCairoFont(14f));
        field.setBackground(INPUT_BG);
        field.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        fieldContainer.add(field, BorderLayout.CENTER);
        panel.add(label, BorderLayout.NORTH);
        panel.add(fieldContainer, BorderLayout.CENTER);
        panel.putClientProperty("field", field);
        return panel;
    }

    private void styleButton(JButton btn, Color bgColor) {
        btn.setFont(getCairoFont(14f).deriveFont(Font.BOLD));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bgColor.brighter()); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(bgColor); }
        });
    }

    public boolean isAuthenticated() { return isAuthenticated; }
    public String getUser() { return userField.getText().trim(); }
}