package nafzh.manager;

import javax.swing.*;
import java.awt.*;
import java.awt.Font;
import java.awt.image.BufferedImage;
import javax.swing.table.DefaultTableCellRenderer;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;
import java.util.function.Consumer;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.awt.FileDialog;
import java.nio.file.StandardCopyOption;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class NafzhManager extends JFrame {
    private JPanel mainContentPanel;
    private JButton dashboardButton;
    private JButton inventoryButton;
    private JButton transactionsButton;
    private JButton posButton;
    private JButton customersButton;
    private JButton installmentsButton;
    private DatabaseManager dbManager;
    private InventoryPanel inventoryPanelInstance;
    private TransactionsPanel transactionsPanelInstance;
    private DashboardPanel dashboardPanelInstance;
    private POSPanel posPanelInstance;
    private CustomersPanel customersPanelInstance;
    private String currentPanelName = DASHBOARD_PANEL;
    private InstallmentsPanel installmentsPanelInstance; // اللوحة الجديدة
    private boolean sidebarVisible = true;
    private static final String DASHBOARD_PANEL = "Dashboard";
    private static final String INVENTORY_PANEL = "Inventory";
    private static final String TRANSACTIONS_PANEL = "Transactions";
    private static final String POS_PANEL = "POS";
    private static final String CUSTOMERS_PANEL = "Customers";
    private static final String INSTALLMENTS_PANEL = "Installments"; // اسم اللوحة الجديد
    private final int BASE_WIDTH = 1300;
    
    public NafzhManager() {
        setTitle("Nafzh Manager | نظام إدارة المؤسسات المتكامل");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.90);
        int height = (int) (screenSize.height * 0.85);
        setSize(width, height);
        setLocationRelativeTo(null);
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        try {
            java.net.URL iconUrl = getClass().getResource("/resources/image/logo.png");
            if (iconUrl != null) {
                Image icon = ImageIO.read(iconUrl);
                setIconImage(icon);
            }
        } catch (Exception e) {
            System.err.println("فشل تحميل أيقونة التطبيق: " + e.getMessage());
        }
        dbManager = new DatabaseManager();
        dbManager.createNewTables();
        initializeUI();
        showPanel(DASHBOARD_PANEL);
        setVisible(true);
    }

    private void initializeUI() {
        // --- 1. إعداد القائمة الجانبية (Sidebar) ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(230, getHeight())); // عرض مناسب
        sidebar.setBackground(new Color(45, 62, 80));
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 5, 15, 5));

        // جلب بيانات المؤسسة
        DatabaseManager.BusinessInfo info = dbManager.getBusinessInfo();
        String companyName = (info != null && info.name != null && !info.name.isEmpty()) ? info.name : "اسم المؤسسة";
        String slogan = (info != null && info.slogan != null) ? info.slogan : "";

        // =========================================================
        // === منطقة الهوية (الاسم واللوجو) - إصلاح التوسيط ===
        // =========================================================

        // أ. حاوية الاسم (لضمان التوسيط الأفقي التام)
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        namePanel.setOpaque(false);
        namePanel.setMaximumSize(new Dimension(230, 70)); // ارتفاع ثابت للاسم

        JLabel titleLabel = new JLabel("<html><div style='text-align: center; width: 200px;'>" + companyName + "</div></html>");
        titleLabel.setFont(getCairoFont(20f).deriveFont(Font.BOLD));
        titleLabel.setForeground(Color.WHITE);
        namePanel.add(titleLabel);
        sidebar.add(namePanel);

        // ب. حاوية الشعار (Slogan)
        if (!slogan.isEmpty()) {
            JPanel sloganPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            sloganPanel.setOpaque(false);
            sloganPanel.setMaximumSize(new Dimension(230, 30));

            JLabel sloganLabel = new JLabel(slogan);
            sloganLabel.setFont(getCairoFont(11f));
            sloganLabel.setForeground(new Color(200, 200, 200));
            sloganPanel.add(sloganLabel);
            sidebar.add(sloganPanel);
        }

        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));

        // ج. حاوية اللوجو
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        logoPanel.setOpaque(false);
        logoPanel.setMaximumSize(new Dimension(230, 120)); // مساحة للصورة

        if (info != null && info.logoBytes != null) {
            ImageIcon originalIcon = new ImageIcon(info.logoBytes);
            ImageIcon scaledIcon = getScaledIcon(originalIcon, 140, 100);
            if (scaledIcon != null) {
                JLabel logoLabel = new JLabel(scaledIcon);
                logoPanel.add(logoLabel);
            }
        } else {
            // اللوجو الافتراضي
            try {
                java.net.URL logoUrl = getClass().getResource("/resources/image/logo.png");
                if (logoUrl != null) {
                    ImageIcon originalIcon = new ImageIcon(logoUrl);
                    ImageIcon scaledIcon = getScaledIcon(originalIcon, 140, 100);
                    if (scaledIcon != null) {
                        JLabel logoLabel = new JLabel(scaledIcon);
                        logoPanel.add(logoLabel);
                    }
                }
            } catch (Exception e) {}
        }
        sidebar.add(logoPanel);
        // =========================================================

        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

        // --- 2. أزرار القائمة ---
        dashboardButton = createSidebarButton("لــوحة التقارير", DASHBOARD_PANEL);
        inventoryButton = createSidebarButton("إدارة المخــزون", INVENTORY_PANEL);
        transactionsButton = createSidebarButton("سجـــل المبيعات", TRANSACTIONS_PANEL);
        posButton = createSidebarButton("عملية بيع  جديدة", POS_PANEL);
        customersButton = createSidebarButton("عمـلاء المؤسسة", CUSTOMERS_PANEL);
        installmentsButton = createSidebarButton("إدارة الأقساط", INSTALLMENTS_PANEL);

        // إضافة الأزرار مع الفواصل
        sidebar.add(dashboardButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(inventoryButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(posButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(transactionsButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(customersButton);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        sidebar.add(installmentsButton);

        // مساحة مرنة تدفع التذييل للأسفل
        sidebar.add(Box.createVerticalGlue());

        // --- 3. تذييل القائمة (لوجو المطور) ---
        JPanel developerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        developerPanel.setOpaque(false);
        developerPanel.setMaximumSize(new Dimension(230, 80));

        try {
            java.net.URL iconUrl = getClass().getResource("/resources/image/logomo.png");
            if (iconUrl != null) {
                ImageIcon scaledIcon = getScaledIcon(new ImageIcon(iconUrl), 40, 40, 0.5f);
                if (scaledIcon != null) {
                    JLabel devIconLabel = new JLabel(scaledIcon);
                    // وضع النص تحت الأيقونة باستخدام HTML
                    devIconLabel.setText("<html><center><br>تصميم: م / محمد السيد</center></html>");
                    devIconLabel.setHorizontalTextPosition(JLabel.CENTER);
                    devIconLabel.setVerticalTextPosition(JLabel.BOTTOM);
                    devIconLabel.setFont(getCairoFont(10f));
                    devIconLabel.setForeground(new Color(150, 160, 170));
                    developerPanel.add(devIconLabel);
                }
            }
        } catch (Exception e) {}
        
        sidebar.add(developerPanel);

        // --- 4. تهيئة منطقة المحتوى (Panels) ---
        mainContentPanel = new JPanel(new CardLayout());

        dashboardPanelInstance = new DashboardPanel(dbManager);
        inventoryPanelInstance = new InventoryPanel(dbManager, this);
        transactionsPanelInstance = new TransactionsPanel(dbManager, this);
        posPanelInstance = new POSPanel(dbManager, this);
        customersPanelInstance = new CustomersPanel(dbManager);
        // تمرير 'this' لتمكين التنقل من التنبيهات
        installmentsPanelInstance = new InstallmentsPanel(dbManager, this); 

        mainContentPanel.add(dashboardPanelInstance, DASHBOARD_PANEL);
        mainContentPanel.add(inventoryPanelInstance, INVENTORY_PANEL);
        mainContentPanel.add(transactionsPanelInstance, TRANSACTIONS_PANEL);
        mainContentPanel.add(posPanelInstance, POS_PANEL);
        mainContentPanel.add(customersPanelInstance, CUSTOMERS_PANEL);
        mainContentPanel.add(installmentsPanelInstance, INSTALLMENTS_PANEL);

        // إعداد المنيو بار
        initializeMenuBar();

        // --- 5. تجميع الإطار الرئيسي ---
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(sidebar, BorderLayout.EAST);

        // شريط العنوان العلوي وزر القائمة
        JButton toggleSidebarBtn = new JButton("☰");
        toggleSidebarBtn.setFont(getCairoFont(18f));
        toggleSidebarBtn.setForeground(new Color(45, 62, 80));
        toggleSidebarBtn.setFocusPainted(false);
        toggleSidebarBtn.setBorderPainted(false);
        toggleSidebarBtn.setContentAreaFilled(false);
        toggleSidebarBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel centerWrapper = new JPanel(new BorderLayout());
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        togglePanel.setBackground(new Color(245, 245, 245));
        togglePanel.add(toggleSidebarBtn);
        togglePanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        
        centerWrapper.add(togglePanel, BorderLayout.NORTH);
        centerWrapper.add(mainContentPanel, BorderLayout.CENTER);
        getContentPane().add(centerWrapper, BorderLayout.CENTER);

        // --- 6. الأحداث (Listeners) ---
        toggleSidebarBtn.addActionListener(e -> {
            sidebarVisible = !sidebarVisible;
            sidebar.setVisible(sidebarVisible);
            revalidate();
            repaint();
        });

        dashboardButton.addActionListener(e -> showPanel(DASHBOARD_PANEL));
        inventoryButton.addActionListener(e -> showPanel(INVENTORY_PANEL));
        transactionsButton.addActionListener(e -> showPanel(TRANSACTIONS_PANEL));
        posButton.addActionListener(e -> showPanel(POS_PANEL));
        customersButton.addActionListener(e -> showPanel(CUSTOMERS_PANEL));
        installmentsButton.addActionListener(e -> showPanel(INSTALLMENTS_PANEL));
        
        // تفعيل تغيير حجم الخط التفاعلي
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                performDynamicFontScaling();
            }
        });
        
        SwingUtilities.invokeLater(this::performDynamicFontScaling);
    }

    private ImageIcon getScaledIcon(ImageIcon srcIcon, int width, int height, float alpha) {
        if (srcIcon == null || srcIcon.getImage() == null) {
            System.err.println("  [getScaledIcon] [خطأ] الأيقونة المصدر فارغة (null).");
            return null;
    }

    // 1. إنشاء صورة جديدة فارغة بالحجم المطلوب
    BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = resizedImage.createGraphics();

    // 2. تفعيل تنعيم الحواف للحصول على جودة أفضل
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    
    // 3. تطبيق الشفافية
    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    
    // 4. رسم الصورة الأصلية مباشرة على الصورة الجديدة مع تغيير حجمها في نفس الوقت
    // هذه الطريقة أكثر موثوقية من getScaledInstance
    g2d.drawImage(srcIcon.getImage(), 0, 0, width, height, null);
    
    g2d.dispose();
    
    return new ImageIcon(resizedImage);
    }

    private ImageIcon getScaledIcon(ImageIcon srcIcon, int width, int height) {
        return getScaledIcon(srcIcon, width, height, 1.0f); 
    }

    public static Font getCairoFont(float size) {
        try {
            InputStream is = NafzhManager.class.getResourceAsStream("/resources/fonts/Cairo-Medium.ttf");
            if (is == null) {
                return new Font("Arial", Font.BOLD, (int) size);
            }
            Font cairoFont = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(cairoFont);
            return cairoFont.deriveFont(size);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Arial", Font.BOLD, (int) size);
        }
    }

    private void initializeMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        Font menuFont = getCairoFont(13f);

        // ============================================
        // 1. قائمة ملف (File Menu)
        // ============================================
        JMenu fileMenu = new JMenu("ملف");
        JMenuItem backupItem = new JMenuItem("نسخ احتياطي لقاعدة البيانات");
        JMenuItem restoreItem = new JMenuItem("استعادة نسخة قديمة");
        JMenuItem exitItem = new JMenuItem("خروج من النظام");

        backupItem.addActionListener(e -> performBackup());
        restoreItem.addActionListener(e -> performRestore());
        exitItem.addActionListener(e -> {
            dispose();
            System.exit(0);
        });

        fileMenu.add(backupItem);
        fileMenu.add(restoreItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // ============================================
        // 2. قائمة تحرير (Edit Menu)
        // ============================================
        JMenu editMenu = new JMenu("تحرير");
        
        // زر إدارة المستخدمين (لإضافة كاشير أو مسؤول)
        JMenuItem usersItem = new JMenuItem("إدارة المستخدمين والصلاحيات");
        usersItem.addActionListener(e -> new UserManagementDialog(this, dbManager).setVisible(true));
        
        // زر إعدادات المؤسسة (الاسم والشعار)
        JMenuItem businessInfoItem = new JMenuItem("بيانات المؤسسة (الاسم والشعار)");
        businessInfoItem.addActionListener(e -> {
            BusinessSettingsDialog dialog = new BusinessSettingsDialog(this, dbManager);
            dialog.setVisible(true);
            if (dialog.isDataSaved()) {
                JOptionPane.showMessageDialog(this, "سيتم إعادة تشغيل التطبيق لتطبيق التغييرات.");
                dispose();
                // إعادة التشغيل بفتح الدالة الرئيسية من جديد
                main(new String[0]); 
            }
        });

        JMenuItem clearCacheItem = new JMenuItem("تحديث وتنشيط الذاكرة");
        clearCacheItem.addActionListener(e -> {
            refreshCurrentPanel();
            JOptionPane.showMessageDialog(this, "تم تحديث البيانات بنجاح.");
        });

        editMenu.add(businessInfoItem);
        editMenu.add(usersItem);
        editMenu.addSeparator();
        editMenu.add(clearCacheItem);

        // ============================================
        // 3. قائمة عرض (View Menu)
        // ============================================
        JMenu viewMenu = new JMenu("عرض");
        JMenuItem toggleSidebarItem = new JMenuItem("إظهار / إخفاء القائمة الجانبية");
        JMenuItem refreshItem = new JMenuItem("تحديث بيانات الصفحة الحالية");

        toggleSidebarItem.addActionListener(e -> {
            JPanel sidebar = null;
            Container contentPane = getContentPane();
            LayoutManager layout = contentPane.getLayout();
            if (layout instanceof BorderLayout) {
                Component eastComponent = ((BorderLayout) layout).getLayoutComponent(BorderLayout.EAST);
                if (eastComponent instanceof JPanel) sidebar = (JPanel) eastComponent;
            }
            if (sidebar != null) {
                sidebarVisible = !sidebar.isVisible();
                sidebar.setVisible(sidebarVisible);
                revalidate();
                repaint();
            }
        });

        refreshItem.addActionListener(e -> {
            if (currentPanelName.equals(INVENTORY_PANEL) && inventoryPanelInstance != null) {
                inventoryPanelInstance.loadInventoryData();
            } else if (currentPanelName.equals(TRANSACTIONS_PANEL) && transactionsPanelInstance != null) {
                transactionsPanelInstance.loadData();
            } else if (currentPanelName.equals(INSTALLMENTS_PANEL) && installmentsPanelInstance != null) {
                installmentsPanelInstance.refreshData();
            } else if (currentPanelName.equals(CUSTOMERS_PANEL) && customersPanelInstance != null) {
                customersPanelInstance.loadData();
            } else if (currentPanelName.equals(DASHBOARD_PANEL) && dashboardPanelInstance != null) {
                dashboardPanelInstance.loadDashboardData();
            }
            revalidate();
            repaint();
        });

        // قائمة فرعية لالتقاط الشاشة
        JMenu captureScreenshotMenu = new JMenu("التقاط صورة للشاشة");
        String[] panelNames = {"لوحة التقارير", "إدارة المخزون", "عملية بيع", "سجل المبيعات", "العملاء", "الأقساط"};
        JComponent[] panelComponents = {dashboardPanelInstance, inventoryPanelInstance, posPanelInstance, transactionsPanelInstance, customersPanelInstance, installmentsPanelInstance};

        for (int i = 0; i < panelNames.length; i++) {
            String name = panelNames[i];
            JComponent component = panelComponents[i];
            JMenuItem subItem = new JMenuItem(name);
            subItem.addActionListener(e -> {
                if (component != null) {
                    boolean success = ComponentScreenshot.captureAndSaveToClipboard(component, name);
                    if (success) {
                        JOptionPane.showMessageDialog(this, "تم نسخ صورة \"" + name + "\" للحافظة (Clipboard).", "نجاح", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "فشل التقاط الصورة.", "خطأ", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            captureScreenshotMenu.add(subItem);
        }

        viewMenu.add(toggleSidebarItem);
        viewMenu.add(refreshItem);
        viewMenu.addSeparator();
        viewMenu.add(captureScreenshotMenu);
        viewMenu.addSeparator();

        // اختصارات سريعة للتنقل
        JMenuItem dashboardViewItem = new JMenuItem("الانتقال إلى: لوحة التقارير");
        JMenuItem inventoryViewItem = new JMenuItem("الانتقال إلى: المخزون");
        JMenuItem posViewItem = new JMenuItem("الانتقال إلى: نقطة البيع");
        JMenuItem transactionsViewItem = new JMenuItem("الانتقال إلى: المبيعات");
        JMenuItem customersViewItem = new JMenuItem("الانتقال إلى: العملاء");
        JMenuItem installmentsViewItem = new JMenuItem("الانتقال إلى: الأقساط");

        dashboardViewItem.addActionListener(e -> showPanel(DASHBOARD_PANEL));
        inventoryViewItem.addActionListener(e -> showPanel(INVENTORY_PANEL));
        posViewItem.addActionListener(e -> showPanel(POS_PANEL));
        transactionsViewItem.addActionListener(e -> showPanel(TRANSACTIONS_PANEL));
        customersViewItem.addActionListener(e -> showPanel(CUSTOMERS_PANEL));
        installmentsViewItem.addActionListener(e -> {
            showPanel(INSTALLMENTS_PANEL);
            installmentsPanelInstance.refreshData();
        });

        viewMenu.add(dashboardViewItem);
        viewMenu.add(inventoryViewItem);
        viewMenu.add(posViewItem);
        viewMenu.add(transactionsViewItem);
        viewMenu.add(customersViewItem);
        viewMenu.add(installmentsViewItem);

        // ============================================
        // 4. قائمة مساعدة (Help Menu)
        // ============================================
        JMenu helpMenu = new JMenu("مساعدة");
        JMenuItem aboutItem = new JMenuItem("حول Nafzh Manager");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this, 
                "برنامج: Nafzh Manager\n" +
                "الإصدار: 1.0 (Enterprise Edition)\n" +
                "تطوير: Nafzh Software Solutions\n" +
                "تصميم: مهندس محمد السيد\n" +
                "للشراء: تواصل مع المصمم على الأرقام هذه\n" +
                "01148538352|01500080403\n" +
                "جميع الحقوق محفوظة © 2026", 
                "حول البرنامج", JOptionPane.INFORMATION_MESSAGE));

        
        helpMenu.add(aboutItem);

        // إضافة القوائم للشريط
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        // تطبيق اتجاه اليمين لليسار (RTL)
        menuBar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        applyRTLLayout(menuBar, menuFont);

        setJMenuBar(menuBar);
    }

        // استبدل دالة performBackup القديمة بهذه:
    private void performBackup() {
        // استخدام FileDialog بدلاً من JFileChooser للحصول على شكل الويندوز الأصلي
        FileDialog fileDialog = new FileDialog(this, "حفظ نسخة احتياطية", FileDialog.SAVE);
        fileDialog.setFile("backup_inventory_" + java.time.LocalDate.now() + ".db");
        
        // محاولة فلترة الملفات (تعمل حسب نظام التشغيل)
        fileDialog.setFilenameFilter((dir, name) -> name.endsWith(".db"));
        
        fileDialog.setVisible(true); // إظهار النافذة

        // التحقق هل قام المستخدم باختيار ملف أم ضغط Cancel
        if (fileDialog.getFile() != null) {
            File source = new File("inventory.db");
            File dest = new File(fileDialog.getDirectory() + fileDialog.getFile());

            // التأكد من الامتداد
            if (!dest.getName().toLowerCase().endsWith(".db")) {
                dest = new File(dest.getAbsolutePath() + ".db");
            }

            try {
                dbManager.close(); 
                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                dbManager.connect(); 
                JOptionPane.showMessageDialog(this, "تم إنشاء النسخة الاحتياطية بنجاح!");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "فشل النسخ الاحتياطي: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
                dbManager.connect();
            }
        }
    }

    // استبدل دالة performRestore القديمة بهذه:
    private void performRestore() {
        FileDialog fileDialog = new FileDialog(this, "اختر ملف النسخة الاحتياطية", FileDialog.LOAD);
        fileDialog.setFile("*.db"); // تلميح للويندوز لعرض ملفات db
        fileDialog.setVisible(true);

        if (fileDialog.getFile() != null) {
            File source = new File(fileDialog.getDirectory() + fileDialog.getFile());
            File dest = new File("inventory.db");

            // حماية إضافية: التأكد أنه ملف قاعدة بيانات فعلاً
            if (!source.getName().endsWith(".db")) {
                JOptionPane.showMessageDialog(this, "الرجاء اختيار ملف قاعدة بيانات صحيح (.db)", "ملف غير صالح", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this, 
                "تحذير: استعادة البيانات ستحذف جميع البيانات الحالية.\nهل أنت متأكد؟", 
                "تأكيد الاستعادة", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    dbManager.close();
                    Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    dbManager.connect();
                    
                    JOptionPane.showMessageDialog(this, "تمت استعادة البيانات بنجاح! سيتم تحديث الواجهة.");
                    
                    updateDashboardData();
                    if(inventoryPanelInstance != null) inventoryPanelInstance.loadInventoryData();
                    if(transactionsPanelInstance != null) transactionsPanelInstance.loadData();
                    if(customersPanelInstance != null) customersPanelInstance.loadData();
                    if(installmentsPanelInstance != null) installmentsPanelInstance.refreshData();
                    if(posPanelInstance != null) posPanelInstance.clearCart();
                    
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "فشل الاستعادة: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
                    dbManager.connect();
                }
            }
        }
    }


    /**
 * دالة محدثة لتطبيق الخط والاتجاه RTL بشكل تكراري (Recursive)
 * لضمان وصول التنسيق لجميع العناصر بما فيها القوائم الفرعية العميقة.
 */
private void applyRTLLayout(JMenuBar menuBar, Font font) {
    // 1. المرور على القوائم الرئيسية (ملف، تحرير، عرض...)
    for (int i = 0; i < menuBar.getMenuCount(); i++) {
        JMenu menu = menuBar.getMenu(i);
        // تنسيق القائمة الرئيسية نفسها
        menu.setFont(font);
        menu.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        // 2. استدعاء الدالة المساعدة للدخول في محتويات القائمة
        applyStyleRecursively(menu.getPopupMenu(), font);
    }
}

    // --- دالة مساعدة جديدة للدخول في عمق القوائم ---
    private void applyStyleRecursively(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            // التحقق مما إذا كان العنصر عنصر قائمة (سواء كان عنصراً عادياً أو قائمة فرعية)
            if (comp instanceof JMenuItem) {
                comp.setFont(font);
                comp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

                // النقطة الحاسمة: إذا كان العنصر هو نفسه قائمة فرعية (JMenu)
                if (comp instanceof JMenu) {
                    // نستدعي نفس الدالة مرة أخرى للدخول إلى محتويات هذه القائمة الفرعية
                    applyStyleRecursively(((JMenu) comp).getPopupMenu(), font);
                }
            }
        }
    }


    public void showPanel(String panelName) {
        this.currentPanelName = panelName;
        CardLayout cl = (CardLayout) (mainContentPanel.getLayout());
        cl.show(mainContentPanel, panelName);
        if (panelName.equals(INVENTORY_PANEL)) {
            inventoryPanelInstance.loadInventoryData();
        } else if (panelName.equals(DASHBOARD_PANEL)) {
            dashboardPanelInstance.loadDashboardData();
        } else if (panelName.equals(TRANSACTIONS_PANEL)) {
            transactionsPanelInstance.loadData();
        } else if (panelName.equals(CUSTOMERS_PANEL)) {
            customersPanelInstance.loadData();
        } else if (panelName.equals(POS_PANEL)) {
            posPanelInstance.resetPanel();
        }
        
       else if (panelName.equals(INSTALLMENTS_PANEL)) {
           if (installmentsPanelInstance != null) {
               installmentsPanelInstance.refreshData() ;
           }
       }

        Color activeColor = new Color(75, 110, 140);
        Color defaultColor = new Color(45, 62, 80);
        dashboardButton.setBackground(panelName.equals(DASHBOARD_PANEL) ? activeColor : defaultColor);
        inventoryButton.setBackground(panelName.equals(INVENTORY_PANEL) ? activeColor : defaultColor);
        transactionsButton.setBackground(panelName.equals(TRANSACTIONS_PANEL) ? activeColor : defaultColor);
        posButton.setBackground(panelName.equals(POS_PANEL) ? activeColor : defaultColor);
        customersButton.setBackground(panelName.equals(CUSTOMERS_PANEL) ? activeColor : defaultColor);
    
        installmentsButton.setBackground(panelName.equals(INSTALLMENTS_PANEL) ? activeColor : defaultColor);

    }

    private JButton createSidebarButton(String text, String actionCommand) {
        JButton button = new JButton("<html><div dir='rtl' style='text-align: right;'>" + text + "</div></html>");
        button.setActionCommand(actionCommand);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(45, 62, 80));
        button.setFont(getCairoFont(16f));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        return button;
    }

    public void updateDashboardData() {
        if (dashboardPanelInstance != null) {
            dashboardPanelInstance.loadDashboardData();
        }
    }

    public void refreshCurrentPanel() {
        switch (currentPanelName) {
            case DASHBOARD_PANEL:
                if (dashboardPanelInstance != null) dashboardPanelInstance.loadDashboardData();
                break;
            case INVENTORY_PANEL:
                if (inventoryPanelInstance != null) inventoryPanelInstance.loadInventoryData();
                break;
            case POS_PANEL:
                if (posPanelInstance != null) posPanelInstance.resetPanel();
                break;
            case TRANSACTIONS_PANEL:
                if (transactionsPanelInstance != null) transactionsPanelInstance.loadData();
                break;
            case CUSTOMERS_PANEL:
                if (customersPanelInstance != null) customersPanelInstance.loadData();
                break;
            default:
                System.out.println("No specific refresh logic for panel: " + currentPanelName);
                break;
        }
        revalidate();
        repaint();
    }
    
    public void updateInventoryDisplay() {
        if (inventoryPanelInstance != null) {
            inventoryPanelInstance.loadInventoryData();
        }
    }

    public void showProductDialog(Runnable onProductChange) {
        showProductDialog(-1, "", "", 0, 0.0, 0.0, onProductChange);
    }

    public void showProductDialog(int id, String name, String category, int quantity, double purchasePrice, double salePrice, Runnable onProductChange) {
        boolean isEditing = id != -1;
        String dialogTitle = isEditing ? "تعديل الصنف: " + name : "إضافة صنف جديد";
        JDialog dialog = new JDialog(this, dialogTitle, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);
        dialog.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        Vector<String> categories = dbManager.getAllCategories();
        
        JPanel fieldsPanel = new JPanel(new GridLayout(8, 2, 10, 10));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        fieldsPanel.add(new JLabel("اسم الصنف:"));
        JTextField nameField = new JTextField(name);
        nameField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        fieldsPanel.add(nameField);
        
        fieldsPanel.add(new JLabel("الفئة:"));
        JComboBox<String> categoryComboBox = new JComboBox<>(categories);
        categoryComboBox.setSelectedItem(category);
        categoryComboBox.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        fieldsPanel.add(categoryComboBox);
        
        fieldsPanel.add(new JLabel("وحدة القياس:"));
        JTextField unitField = new JTextField(isEditing ? dbManager.getProductUnit(id) : ""); 
        unitField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        fieldsPanel.add(unitField);

        fieldsPanel.add(new JLabel("سعر الشراء:"));
        JTextField purchasePriceField = new JTextField(String.valueOf(purchasePrice));
        purchasePriceField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        fieldsPanel.add(purchasePriceField);
        
        fieldsPanel.add(new JLabel("سعر البيع:"));
        JTextField salePriceField = new JTextField(String.valueOf(salePrice));
        salePriceField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        fieldsPanel.add(salePriceField);
        
        fieldsPanel.add(new JLabel("الكمية:"));
        JTextField quantityField = new JTextField(String.valueOf(quantity));
        quantityField.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        fieldsPanel.add(quantityField);
        
        JButton manageCategoryButton = new JButton("إدارة الفئات");
        manageCategoryButton.addActionListener(e -> {
            showCategoryManagerDialog((updatedCategories) -> {
                categoryComboBox.setModel(new DefaultComboBoxModel<>(updatedCategories));
                if (isEditing && updatedCategories.contains(category)) {
                    categoryComboBox.setSelectedItem(category);
                }
            });
        });
        fieldsPanel.add(new JLabel());
        fieldsPanel.add(manageCategoryButton);
        dialog.add(fieldsPanel, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton saveButton = new JButton(isEditing ? "حفظ التعديلات" : "إضافة");
        saveButton.addActionListener(e -> {
            try {
                String newName = nameField.getText().trim();
                String newCategory = (String) categoryComboBox.getSelectedItem();
                String newUnit = unitField.getText().trim();
                double newPurchasePrice = Double.parseDouble(purchasePriceField.getText().trim());
                double newSalePrice = Double.parseDouble(salePriceField.getText().trim());
                int newQuantity = Integer.parseInt(quantityField.getText().trim());
                
                if (newName.isEmpty() || newCategory == null || newCategory.isEmpty() || newUnit.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "الرجاء إدخال جميع الحقول المطلوبة.", "خطأ", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                boolean success = isEditing ?
                    dbManager.updateProduct(id, newName, newCategory, newUnit, newQuantity, newPurchasePrice, newSalePrice) :
                    dbManager.addProduct(newName, newCategory, newUnit, newQuantity, newPurchasePrice, newSalePrice);
                
                if (success) {
                    onProductChange.run();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "فشل في حفظ الصنف.", "خطأ", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "الرجاء إدخال أرقام صحيحة للأسعار والكمية.", "خطأ في الإدخال", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        JButton cancelButton = new JButton("إلغاء");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    public void showCategoryManagerDialog(Consumer<Vector<String>> onUpdate) {
        JDialog categoryDialog = new JDialog(this, "إدارة فئات الأصناف", true);
        categoryDialog.setLayout(new BorderLayout(10, 10));
        categoryDialog.setSize(400, 400);
        categoryDialog.setLocationRelativeTo(this);
        Vector<String> categories = dbManager.getAllCategories();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        categories.forEach(listModel::addElement);
        JList<String> categoryList = new JList<>(listModel);
        
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        JTextField categoryField = new JTextField();
        JButton addButton = new JButton("إضافة");
        addButton.addActionListener(e -> {
            String newCat = categoryField.getText().trim();
            if (!newCat.isEmpty() && dbManager.addCategory(newCat)) {
                listModel.addElement(newCat);
                categoryField.setText("");
            }
        });
        inputPanel.add(categoryField, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);

        JButton deleteButton = new JButton("حذف المحدد");
        deleteButton.addActionListener(e -> {
            String selectedCat = categoryList.getSelectedValue();
            if (selectedCat != null && dbManager.deleteCategory(selectedCat)) {
                listModel.removeElement(selectedCat);
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.add(inputPanel, BorderLayout.NORTH);
        topPanel.add(deleteButton, BorderLayout.SOUTH);

        categoryDialog.add(topPanel, BorderLayout.NORTH);
        categoryDialog.add(new JScrollPane(categoryList), BorderLayout.CENTER);
        
        JButton closeButton = new JButton("إغلاق");
        closeButton.addActionListener(e -> {
            onUpdate.accept(dbManager.getAllCategories());
            categoryDialog.dispose();
        });
        categoryDialog.add(closeButton, BorderLayout.SOUTH);

        categoryDialog.setVisible(true);
        }

    public void showSaleDetailsDialog(int saleId) {
        JDialog detailsDialog = new JDialog(this, "تفاصيل فاتورة البيع رقم: " + saleId, true);
        detailsDialog.setLayout(new BorderLayout(10, 10));
        detailsDialog.setSize(700, 500);
        detailsDialog.setLocationRelativeTo(this);
        detailsDialog.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // 1. تحديد أسماء الأعمدة الصحيحة
        String[] columnNames = {"اسم الصنف", "الوحدة", "الكمية", "سعر الوحدة", "الإجمالي الفرعي"};
        DefaultTableModel detailsModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // جعل كل الخلايا غير قابلة للتعديل
            }
        };

        JTable detailsTable = new JTable(detailsModel);
        detailsTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        detailsTable.setRowHeight(30);
        detailsTable.setFont(getCairoFont(14f)); // يمكنك تعديل حجم الخط حسب الرغبة

        // =================================================================
        // ==== بدء التعديل: جزء تنسيق الجدول ====
        // =================================================================

        // 2. إنشاء Renderer للتوسيط
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // 3. تطبيق الـ Renderer على جميع خلايا الجدول
        for (int i = 0; i < detailsTable.getColumnCount(); i++) {
            detailsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 4. تنسيق رأس الجدول (Header)
        javax.swing.table.JTableHeader header = detailsTable.getTableHeader();
        header.setFont(getCairoFont(16f));
        // التأكد من أن رأس الجدول يستخدم الـ Renderer الخاص به مع التوسيط
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        // =================================================================
        // ==== نهاية التعديل ====
        // =================================================================

        // 5. جلب البيانات من قاعدة البيانات
        List<List<Object>> saleDetails = dbManager.getSaleItems(saleId);
        double grandTotal = 0.0;

        for (List<Object> rowData : saleDetails) {
            // تأكد من أن البيانات متوافقة مع الأعمدة
            Vector<Object> row = new Vector<>();
            row.add(rowData.get(0)); // اسم الصنف
            row.add(rowData.get(1)); // الوحدة
            row.add(rowData.get(2)); // الكمية
            row.add(rowData.get(3)); // سعر الوحدة
            row.add(rowData.get(4)); // الإجمالي الفرعي

            detailsModel.addRow(row);

            // التأكد من أن الإجمالي الفرعي هو العنصر الخامس (index 4)
            if (rowData.size() > 4 && rowData.get(4) instanceof Number) {
                grandTotal += ((Number) rowData.get(4)).doubleValue();
            }
        }

        JScrollPane scrollPane = new JScrollPane(detailsTable);
        detailsDialog.add(scrollPane, BorderLayout.CENTER);

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel totalLabel = new JLabel(String.format("الإجمالي الكلي للفاتورة: %.2f", grandTotal));
        totalLabel.setFont(getCairoFont(18f));
        totalPanel.add(totalLabel);
        detailsDialog.add(totalPanel, BorderLayout.SOUTH);

        detailsDialog.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> {
            // 1. نظام الترخيص (License)
            LicenseManager licenseManager = new LicenseManager();
            LicenseManager.LicenseStatus status = licenseManager.checkLicense();

            if (status != LicenseManager.LicenseStatus.ACTIVATED) {
                LicenseDialog licenseDialog = new LicenseDialog(null, licenseManager, status);
                licenseDialog.setVisible(true);

                if (!licenseDialog.isActivatedOrTrial()) {
                    System.exit(0);
                }
            }

            // 2. الاتصال بقاعدة البيانات وإنشاء الجداول
            DatabaseManager db = new DatabaseManager();
            db.createNewTables();

            // 3. شاشة تسجيل الدخول (Login)
            LoginDialog loginDialog = new LoginDialog(null, db);
            loginDialog.setVisible(true);

            // 4. إذا نجح الدخول، نبدأ التطبيق
            if (loginDialog.isAuthenticated()) {
                
                // --- الإضافة الجديدة: التحقق من بيانات المؤسسة ---
                DatabaseManager.BusinessInfo info = db.getBusinessInfo();
                
                // إذا لم تكن البيانات موجودة (أول مرة)، نفتح نافذة الإعدادات
                if (info == null) {
                    // نفتح نافذة مخفية مؤقتاً لتكون أباً للـ Dialog
                    JFrame dummyFrame = new JFrame();
                    BusinessSettingsDialog settingsDialog = new BusinessSettingsDialog(dummyFrame, db);
                    settingsDialog.setVisible(true);
                    dummyFrame.dispose();
                    
                    // إذا أغلق المستخدم النافذة دون حفظ، نغلق البرنامج (لإجباره على الإدخال)
                    if (!settingsDialog.isDataSaved()) {
                        System.exit(0);
                    }
                }
                // ------------------------------------------------

                // تشغيل التطبيق الرئيسي
                new NafzhManager();
                
            } else {
                System.exit(0);
            }
        });
    }
  
    private void performDynamicFontScaling() {
        int currentWidth = getWidth();
        
        // حساب نسبة التغيير (Scale Factor)
        float scaleFactor = (float) currentWidth / BASE_WIDTH;
        
        // وضع حدود (مثلاً لا يصغر عن 75% ولا يكبر عن 130% للحفاظ على الشكل)
        scaleFactor = Math.max(0.75f, Math.min(scaleFactor, 1.3f));

        // بدء التحديث التكراري لكل المكونات
        updateComponentHierarchy(this, scaleFactor);
    }

    private void updateComponentHierarchy(Container container, float scaleFactor) {
        for (Component comp : container.getComponents()) {
            
            // التعامل مع المكونات
            if (comp instanceof JComponent) {
                JComponent jc = (JComponent) comp;
                
                // أ. محاولة جلب الخط الأصلي المحفوظ سابقاً
                Font originalFont = (Font) jc.getClientProperty("original_font");
                
                // ب. إذا لم يكن محفوظاً (أول مرة)، نقوم بحفظه
                if (originalFont == null) {
                    originalFont = jc.getFont();
                    jc.putClientProperty("original_font", originalFont);
                }

                // ج. تطبيق الحجم الجديد بناءً على الحجم الأصلي
                if (originalFont != null) {
                    float newSize = originalFont.getSize() * scaleFactor;
                    
                    // تحسين: إذا كان المكون عنواناً كبيراً، نقلل نسبة التكبير قليلاً
                    if (originalFont.getSize() > 20) newSize = originalFont.getSize() * (scaleFactor * 0.9f);
                    
                    jc.setFont(originalFont.deriveFont(newSize));
                }
                
                // د. تحسين خاص للجداول (تغيير ارتفاع الصف مع الخط)
                if (comp instanceof JTable) {
                    JTable table = (JTable) comp;
                    // حفظ ارتفاع الصف الأصلي
                    Integer originalRowHeight = (Integer) table.getClientProperty("original_row_height");
                    if (originalRowHeight == null) {
                        originalRowHeight = table.getRowHeight();
                        table.putClientProperty("original_row_height", originalRowHeight);
                    }
                    table.setRowHeight((int) (originalRowHeight * scaleFactor));
                }
            }
            
            // هـ. الاستمرار في البحث داخل الحاويات (Panels)
            if (comp instanceof Container) {
                updateComponentHierarchy((Container) comp, scaleFactor);
            }
        }
    }


}
