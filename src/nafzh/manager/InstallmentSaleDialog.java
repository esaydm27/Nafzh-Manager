package nafzh.manager;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import static nafzh.manager.NafzhManager.getCairoFont;

public class InstallmentSaleDialog extends JDialog {

    private final DatabaseManager dbManager;
    private final double totalAmount;
    private final Map<Integer, Integer> cartItems;

    // أزرار أنواع الأقساط (تم حذف btnDeferred)
    private JButton btnEqual, btnReducing, btnBalloon, btnPeriodic;
    private final int BASE_WIDTH = 1200;
    // حاوية البانلات
    private JPanel cardsPanel;
    private CardLayout cardLayout;

    // أسماء البانلات (تم حذف PANEL_DEFERRED)
    private static final String PANEL_EQUAL = "EqualPanel";
    private static final String PANEL_REDUCING = "ReducingPanel";
    private static final String PANEL_BALLOON = "BalloonPanel";
    private static final String PANEL_PERIODIC = "PeriodicPanel";

    public InstallmentSaleDialog(Frame owner, DatabaseManager dbManager, double totalAmount, Map<Integer, Integer> cartItems) {
        super(owner, "إعداد تفاصيل البيع بالتقسيط", true);
        this.dbManager = dbManager;
        this.totalAmount = totalAmount;
        this.cartItems = cartItems;

        initializeUI();
    }

        private void initializeUI() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.85);
        int height = (int) (screenSize.height * 0.80);
        setSize(width, height);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // --- 1. الهيدر (الأزرار العلوية) ---
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setPreferredSize(new Dimension(0, 80));
        headerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 20));
        headerPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        btnEqual = createHeaderButton("الأقساط المتساوية");
        btnReducing = createHeaderButton("القسط المتناقص");
        btnBalloon = createHeaderButton("نظام الكباري");
        btnPeriodic = createHeaderButton("الأقساط الدورية");

        // --- 2. منطقة المحتوى (CardLayout) ---
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setOpaque(false);
        
        // 1. إضافة بانل الأقساط المتساوية (والتي تشمل المؤجل أيضاً)
        EqualInstallmentsPanel equalPanel = new EqualInstallmentsPanel(dbManager, totalAmount, cartItems, this);
        cardsPanel.add(equalPanel, PANEL_EQUAL);
        
        // 2. إضافة بانل القسط المتناقص
        ReducingInstallmentsPanel reducingPanel = new ReducingInstallmentsPanel(dbManager, totalAmount, cartItems, this);
        cardsPanel.add(reducingPanel, PANEL_REDUCING);

        // 3. إضافة بانل نظام الكباري
        BalloonInstallmentsPanel balloonPanel = new BalloonInstallmentsPanel(dbManager, totalAmount, cartItems, this);
        cardsPanel.add(balloonPanel, PANEL_BALLOON);
        
        // 4. إضافة بانل الأقساط الدورية (الجديدة)
        PeriodicInstallmentsPanel periodicPanel = new PeriodicInstallmentsPanel(dbManager, totalAmount, cartItems, this);
        cardsPanel.add(periodicPanel, PANEL_PERIODIC);

        // --- برمجة الأزرار للتبديل بين الصفحات ---
        btnEqual.addActionListener(e -> {
            setActiveMethod(btnEqual);
            cardLayout.show(cardsPanel, PANEL_EQUAL);
            
        });

                // ... (بعد سطر cardLayout.show(cardsPanel, PANEL_EQUAL);) ...

        // --- تفعيل تغيير الخط الديناميكي ---
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                performDynamicFontScaling();
            }
        });
        
        // استدعاء أولي لضبط الحجم عند الفتح
        SwingUtilities.invokeLater(this::performDynamicFontScaling);
    

        btnReducing.addActionListener(e -> {
            setActiveMethod(btnReducing);
            cardLayout.show(cardsPanel, PANEL_REDUCING);
        });

        btnBalloon.addActionListener(e -> {
            setActiveMethod(btnBalloon);
            cardLayout.show(cardsPanel, PANEL_BALLOON);
        });
        
        btnPeriodic.addActionListener(e -> {
            setActiveMethod(btnPeriodic);
            cardLayout.show(cardsPanel, PANEL_PERIODIC);
        });

        // إضافة الأزرار للهيدر
        headerPanel.add(btnEqual);
        headerPanel.add(btnReducing);
        headerPanel.add(btnBalloon);
        headerPanel.add(btnPeriodic);

        add(headerPanel, BorderLayout.NORTH);
        add(cardsPanel, BorderLayout.CENTER);

        // تفعيل الزر الأول والبانال الخاصة به عند البدء افتراضياً
        setActiveMethod(btnEqual);
        cardLayout.show(cardsPanel, PANEL_EQUAL);
    }


    private JButton createHeaderButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(getCairoFont(12f).deriveFont(Font.BOLD));
        btn.setBackground(new Color(255, 255, 255));
        btn.setForeground(new Color(41, 128, 185));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.WHITE, 1),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // --- تم إصلاح هذه الدالة لحذف btnDeferred ---
    private void setActiveMethod(JButton activeButton) {
        // إعادة تعيين الأزرار الموجودة فقط
        resetButtonStyle(btnEqual);
        resetButtonStyle(btnReducing);
        resetButtonStyle(btnBalloon);
        resetButtonStyle(btnPeriodic);
        
        // تلوين الزر النشط
        styleActiveButton(activeButton);
    }

    private void styleActiveButton(JButton btn) {
        if (btn != null) {
            btn.setBackground(new Color(241, 196, 15));
            btn.setForeground(Color.BLACK);
        }
    }

    private void resetButtonStyle(JButton btn) {
        if (btn != null) {
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(41, 128, 185));
        }
    }
    
   

    private void performDynamicFontScaling() {
        int currentWidth = getWidth();
        // حساب نسبة التغيير
        float scaleFactor = (float) currentWidth / BASE_WIDTH;
        // وضع حدود للتكبير والتصغير (بين 70% و 140%)
        scaleFactor = Math.max(0.70f, Math.min(scaleFactor, 1.4f));

        updateComponentHierarchy(this.getContentPane(), scaleFactor);
    }

    private void updateComponentHierarchy(Container container, float scaleFactor) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JComponent) {
                JComponent jc = (JComponent) comp;
                
                // 1. التعامل مع الخطوط
                Font originalFont = (Font) jc.getClientProperty("original_font");
                if (originalFont == null) {
                    originalFont = jc.getFont();
                    jc.putClientProperty("original_font", originalFont);
                }
                if (originalFont != null) {
                    float newSize = originalFont.getSize() * scaleFactor;
                    if (originalFont.getSize() > 18) newSize = originalFont.getSize() * (scaleFactor * 0.9f); // تقليل تكبير العناوين الضخمة
                    jc.setFont(originalFont.deriveFont(newSize));
                }
                
                // 2. التعامل مع ارتفاع صفوف الجداول
                if (comp instanceof JTable) {
                    JTable table = (JTable) comp;
                    Integer originalRowHeight = (Integer) table.getClientProperty("original_row_height");
                    if (originalRowHeight == null) {
                        originalRowHeight = table.getRowHeight();
                        table.putClientProperty("original_row_height", originalRowHeight);
                    }
                    int newRowHeight = (int) (originalRowHeight * scaleFactor);
                    table.setRowHeight(Math.max(20, newRowHeight)); // لا يقل عن 20
                }
            }
            
            // الاستمرار في البحث داخل الحاويات
            if (comp instanceof Container) {
                updateComponentHierarchy((Container) comp, scaleFactor);
            }
        }
    }

}
