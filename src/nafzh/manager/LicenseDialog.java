package nafzh.manager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import static nafzh.manager.NafzhManager.getCairoFont;

public class LicenseDialog extends JDialog {
    
    private final LicenseManager licenseManager;
    private boolean isActivated = false;
    private boolean allowTrial = false;

    public LicenseDialog(Frame parent, LicenseManager licenseManager, LicenseManager.LicenseStatus status) {
        super(parent, "Nafzh Manager | نظام إدارة المؤسسات المتكامل", true);
        this.licenseManager = licenseManager;
        initializeUI(status);
    }

    private void initializeUI(LicenseManager.LicenseStatus status) {
        setSize(500, 450);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout());
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        Color DARK_BG = new Color(44, 62, 80);
        Color TEXT_COLOR = Color.WHITE;

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(DARK_BG);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 1. حالة الترخيص
        String msg = "";
        Color statusColor = Color.YELLOW;
        
        switch (status) {
            case TRIAL:
                long days = licenseManager.getTrialDaysRemaining();
                msg = "نسخة تجريبية: متبقى " + days + " أيام.";
                allowTrial = true;
                statusColor = new Color(241, 196, 15);
                break;
            case EXPIRED:
                msg = "انتهت الفترة التجريبية. يرجى الشراء للمتابعة.";
                allowTrial = false;
                statusColor = new Color(231, 76, 60);
                break;
            case TAMPERED:
                msg = "خطأ في النظام: تم التلاعب بتاريخ الجهاز.";
                allowTrial = false;
                statusColor = Color.RED;
                break;
            default:
                break;
        }

        JLabel statusLabel = new JLabel(msg);
        statusLabel.setFont(getCairoFont(16f).deriveFont(Font.BOLD));
        statusLabel.setForeground(statusColor);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(30));

        // 2. كود الطلب (Request Code)
        JLabel reqLabel = new JLabel("كود الطلب (أرسل هذا الكود للمطور):");
        reqLabel.setFont(getCairoFont(13f));
        reqLabel.setForeground(TEXT_COLOR);
        reqLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField reqField = new JTextField(licenseManager.getRequestCode());
        reqField.setFont(new Font("Consolas", Font.BOLD, 16));
        reqField.setHorizontalAlignment(JTextField.CENTER);
        reqField.setEditable(false);
        reqField.setMaximumSize(new Dimension(400, 40));
        
        JButton copyBtn = new JButton("نسخ الكود");
        copyBtn.setFont(getCairoFont(12f));
        copyBtn.setBackground(new Color(52, 152, 219));
        copyBtn.setForeground(Color.WHITE);
        copyBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new StringSelection(reqField.getText()), null);
            JOptionPane.showMessageDialog(this, "تم نسخ الكود");
        });

        mainPanel.add(reqLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(reqField);
        mainPanel.add(Box.createVerticalStrut(5));
        
        JPanel copyPanel = new JPanel(); 
        copyPanel.setOpaque(false); 
        copyPanel.add(copyBtn);
        mainPanel.add(copyPanel);
        
        mainPanel.add(Box.createVerticalStrut(30));

        // 3. كود التفعيل (Activation Key)
        JLabel actLabel = new JLabel("أدخل كود التفعيل:");
        actLabel.setFont(getCairoFont(13f));
        actLabel.setForeground(TEXT_COLOR);
        actLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JTextField actField = new JTextField();
        actField.setFont(new Font("Consolas", Font.BOLD, 16));
        actField.setHorizontalAlignment(JTextField.CENTER);
        actField.setMaximumSize(new Dimension(400, 40));

        mainPanel.add(actLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(actField);
        mainPanel.add(Box.createVerticalStrut(30));

        // 4. الأزرار
        JButton activateBtn = new JButton("تفعيل الآن");
        activateBtn.setFont(getCairoFont(14f));
        activateBtn.setBackground(new Color(46, 204, 113));
        activateBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        activateBtn.setForeground(Color.BLACK);
        
        JButton trialBtn = new JButton("متابعة التجربة");
        trialBtn.setFont(getCairoFont(14f));
        trialBtn.setBackground(new Color(149, 165, 166));
        trialBtn.setForeground(Color.BLACK);
        trialBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        trialBtn.setEnabled(allowTrial); // تعطيل الزر إذا انتهت الفترة

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(trialBtn);
        btnPanel.add(activateBtn);
        
        mainPanel.add(btnPanel);
        add(mainPanel, BorderLayout.CENTER);

        // --- الأحداث ---
        activateBtn.addActionListener(e -> {
            if (licenseManager.activate(actField.getText())) {
                JOptionPane.showMessageDialog(this, "تم التفعيل بنجاح! شكراً لشرائك النسخة.");
                isActivated = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "كود التفعيل غير صحيح", "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        });

        trialBtn.addActionListener(e -> {
            if (allowTrial) {
                dispose();
            }
        });
        
        // إجبار المستخدم على اتخاذ قرار (منع الإغلاق من X إذا كانت الفترة منتهية)
        if (!allowTrial) {
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    System.exit(0); // إغلاق البرنامج بالكامل
                }
            });
        }
    }

    public boolean isActivatedOrTrial() {
        return isActivated || allowTrial;
    }
}
