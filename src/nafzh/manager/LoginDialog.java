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
    
    // الألوان المستخدمة في التصميم الداكن
    private final Color DARK_BG = new Color(44, 62, 80);       // خلفية داكنة
    private final Color INPUT_BG = new Color(255, 255, 255);   // خلفية الحقول
    private final Color ACCENT_BLUE = new Color(20, 90, 50); // لون زر الدخول
    private final Color ACCENT_GREEN = new Color(20, 90, 50);  // لون زر الإنشاء (أخضر غامق)
    private final Color TEXT_COLOR = Color.WHITE;              // لون النصوص

    public LoginDialog(Frame parent, DatabaseManager dbManager) {
        super(parent, "تسجيل الدخول - Nafzh Manager", true);
        this.dbManager = dbManager;
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
        String titleText = isFirstRun ? "إنشاء حساب المدير" : "تسجيل الدخول";
        String subTitleText = isFirstRun ? "مرحباً بك في أول تشغيل للنظام" : "مرحباً بعودتك";

        // --- اللوحة الرئيسية ---
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(DARK_BG);
        mainPanel.setBorder(new EmptyBorder(20, 40, 20, 40));

        // 1. العنوان
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

        // 2. حقل اسم المستخدم
        JPanel userPanel = createInputPanel("اسم المستخدم", false);
        userField = (JTextField) userPanel.getClientProperty("field");
        
        // --- إضافة فلتر لمنع الحروف العربية ---
        userField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                // السماح فقط بالحروف الإنجليزية، الأرقام، المسافة، وبعض الرموز الخاصة
                if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE)) {
                    e.consume(); // تجاهل الحرف المكتوب
                    getToolkit().beep(); // صوت تنبيه خفيف
                }
            }
        });
        
        mainPanel.add(userPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // 3. حقل كلمة المرور
        JPanel passPanelWrapper = createInputPanel("كلمة المرور", true);
        passField = (JPasswordField) passPanelWrapper.getClientProperty("field");
        mainPanel.add(passPanelWrapper);
        mainPanel.add(Box.createVerticalStrut(30));

        // 4. الأزرار
        JButton actionBtn = new JButton(isFirstRun ? "إنشاء الحساب" : "دخول");
        styleButton(actionBtn, isFirstRun ? ACCENT_GREEN : ACCENT_BLUE);
        
        JButton closeBtn = new JButton("خروج");
        styleButton(closeBtn, new Color(192, 57, 43)); // أحمر

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        btnPanel.setOpaque(false);
        // الترتيب حسب الاتجاه RTL: الزر الأيمن هو الخروج، الأيسر هو الدخول
        btnPanel.add(closeBtn); 
        btnPanel.add(actionBtn);
        
        mainPanel.add(btnPanel);
        add(mainPanel, BorderLayout.CENTER);

        // --- منطق زر الدخول/الإنشاء ---
        actionBtn.addActionListener(e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword()).trim();

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "يرجى ملء جميع الحقول", "تنبيه", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // التحقق من قوة كلمة السر عند الإنشاء فقط
            if (isFirstRun && pass.length() < 6) {
                JOptionPane.showMessageDialog(this, "كلمة المرور ضعيفة! يجب أن تكون 6 أحرف على الأقل.", "تنبيه أمني", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (isFirstRun) {
                if (dbManager.addUser(user, pass, "admin")) {
                    JOptionPane.showMessageDialog(this, "تم إنشاء حساب المدير بنجاح!");
                    isAuthenticated = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "حدث خطأ أثناء الإنشاء");
                }
            } else {
                if (dbManager.checkUserCredentials(user, pass)) {
                    isAuthenticated = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "اسم المستخدم أو كلمة المرور غير صحيحة", "خطأ في الدخول", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        closeBtn.addActionListener(e -> System.exit(0));
        getRootPane().setDefaultButton(actionBtn);
    }

    // --- دوال مساعدة للتصميم ---
    private JPanel createInputPanel(String labelText, boolean isPassword) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(new Dimension(400, 60));
        
        // --- التعديل: ضبط اتجاه اللوحة لليمين لضمان ظهور العنوان يميناً ---
        panel.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JLabel label = new JLabel(labelText);
        label.setFont(getCairoFont(12f));
        label.setForeground(TEXT_COLOR);
        // محاذاة النص لليمين
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        
        // حاوية الحقل
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
            
            // --- التعديل: وضع زر العين في اليمين (EAST) ---
            // بما أن الحاوية ليس لها اتجاه محدد (افتراضياً LTR)، فإن EAST يعني اليمين.
            fieldContainer.add(eyeBtn, BorderLayout.EAST); 
            field = pf;
        } else {
            field = new JTextField();
        }

        field.setBorder(null);
        field.setFont(getCairoFont(14f));
        field.setBackground(INPUT_BG);
        // --- التعديل: إجبار الكتابة من اليسار لليمين داخل الحقل ---
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
}
