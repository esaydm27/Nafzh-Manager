package nafzh.manager;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;
import javax.imageio.ImageIO;
import static nafzh.manager.NafzhManager.getCairoFont;

// --- كلاس البيانات ---
class SaleInvoiceData {
    public int saleId;
    public String invoiceDate;
    public String customerName;
    public int customerId;
    public String salesmanName;
    public double totalAmount;
    public double paidAmount;
    public double remainingOnInvoice;
    public double priorBalance;
    public double currentBalance;
    public List<List<Object>> items;

    public SaleInvoiceData(int saleId) {
        this.saleId = saleId;
        this.items = new Vector<>();
    }
}

public class InvoicePanel extends JPanel {

    private final DatabaseManager dbManager;
    private final SaleInvoiceData invoiceData;
    private static final DecimalFormat DF = new DecimalFormat("0.00");
    private final DatabaseManager.BusinessInfo businessInfo;
    private Image watermarkImage;

    public InvoicePanel(int saleId, DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.invoiceData = new SaleInvoiceData(saleId);
        this.businessInfo = dbManager.getBusinessInfo();
        
        prepareWatermark();
        loadInvoiceData();
        initializePanel();
    }
    
    private void prepareWatermark() {
        if (businessInfo != null && businessInfo.logoBytes != null) {
            try {
                watermarkImage = ImageIO.read(new ByteArrayInputStream(businessInfo.logoBytes));
            } catch (Exception e) {}
        } else {
            try {
                java.net.URL url = getClass().getResource("/resources/image/logo.png");
                if (url != null) watermarkImage = ImageIO.read(url);
            } catch (Exception e) {}
        }
    }

    private void loadInvoiceData() {
        List<Object> saleDetails = dbManager.getSaleDetails(invoiceData.saleId);
        if (saleDetails != null && saleDetails.size() >= 7) {
            invoiceData.saleId = (int) saleDetails.get(0);
            invoiceData.invoiceDate = (String) saleDetails.get(1);
            invoiceData.customerName = (String) saleDetails.get(2);
            invoiceData.customerId = (int) saleDetails.get(3);
            invoiceData.totalAmount = (double) saleDetails.get(4);
            invoiceData.salesmanName = (String) saleDetails.get(5);
            invoiceData.paidAmount = (double) saleDetails.get(6);
        }
        invoiceData.remainingOnInvoice = invoiceData.totalAmount - invoiceData.paidAmount;
        if (invoiceData.customerId != 0) {
            double balanceInDB = dbManager.getCustomerPriorBalance(invoiceData.customerId);
            invoiceData.currentBalance = balanceInDB;
            invoiceData.priorBalance = invoiceData.currentBalance - invoiceData.remainingOnInvoice;
        }
        invoiceData.items = dbManager.getSaleItems(invoiceData.saleId);
    }

    private void initializePanel() {
        setLayout(new BorderLayout(0, 15)); 
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createItemsTablePanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // 1. معلومات الشركة (اسم + شعار + هواتف)
        String companyName = (businessInfo != null && businessInfo.name != null) ? businessInfo.name : "اسم المؤسسة";
        String slogan = (businessInfo != null && businessInfo.slogan != null) ? businessInfo.slogan : "";
        String phone1 = (businessInfo != null && businessInfo.phone1 != null) ? businessInfo.phone1 : "";
        String phone2 = (businessInfo != null && businessInfo.phone2 != null) ? businessInfo.phone2 : "";
        String salesmanName = (businessInfo != null && businessInfo.salesman != null) ? businessInfo.salesman : "المندوب";

        JLabel nameLabel = new JLabel(companyName);
        nameLabel.setFont(getCairoFont(30f).deriveFont(Font.BOLD));
        nameLabel.setForeground(Color.BLACK);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel sloganLabel = new JLabel(slogan);
        sloganLabel.setFont(getCairoFont(13f));
        sloganLabel.setForeground(Color.BLACK);
        sloganLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel phonesPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        phonesPanel.setBackground(Color.WHITE);
        if (!phone1.isEmpty()) {
            JLabel p1 = new JLabel(phone1 + " \u260E");
            p1.setFont(getCairoFont(12f));
            p1.setForeground(Color.BLACK);
            phonesPanel.add(p1);
        }
        if (!phone2.isEmpty()) {
            JLabel p2 = new JLabel(phone2 + " \uD83D\uDCDE");
            p2.setFont(getCairoFont(12f));
            p2.setForeground(Color.BLACK);
            phonesPanel.add(p2);
        }
        
        headerPanel.add(nameLabel);
        if (!slogan.isEmpty()) headerPanel.add(sloganLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(phonesPanel);
        headerPanel.add(Box.createVerticalStrut(20)); // مسافة قبل البيانات

        // 2. بيانات الفاتورة (Grid)
        JPanel infoGrid = new JPanel(new GridLayout(2, 2, 20, 5)); // مسافة أفقية 20 وعمودية 5
        infoGrid.setBackground(Color.WHITE);
        infoGrid.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // الصف الأول
        // يمين: العميل
        infoGrid.add(createSingleLabelRow("المطلوب من السيد:", invoiceData.customerName));
        // يسار: رقم الفاتورة
        infoGrid.add(createSingleLabelRow("رقم الفاتورة:", "#" + invoiceData.saleId));

        // الصف الثاني
        // يمين: المندوب
        infoGrid.add(createSingleLabelRow("المندوب:", salesmanName));
        // يسار: التاريخ
        infoGrid.add(createSingleLabelRow("تحرير في:", invoiceData.invoiceDate));

        headerPanel.add(infoGrid);
        headerPanel.add(Box.createVerticalStrut(10));

        return headerPanel;
    }

    // --- دالة جديدة لإنشاء سطر بيانات (دمج العنوان والقيمة في نص واحد) ---
    private JLabel createSingleLabelRow(String label, String value) {
        // دمج النص: "العنوان: القيمة"
        JLabel lbl = new JLabel(label + "  " + value);
        lbl.setFont(getCairoFont(13f).deriveFont(Font.BOLD));
        lbl.setForeground(Color.BLACK); // أسود بالكامل كما طلبت
        // محاذاة لليمين دائماً (لأننا في Grid RTL)
        lbl.setHorizontalAlignment(SwingConstants.RIGHT); 
        return lbl;
    }

    // --- الجدول ---
    private JPanel createItemsTablePanel() {
        String[] columns = {"م", "البيان", "الوحدة", "العدد", "السعر", "الإجمالي"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        int idx = 1;
        for (List<Object> item : invoiceData.items) {
            model.addRow(new Object[]{
                idx++, item.get(0), item.get(1), item.get(2), 
                DF.format(item.get(3)), DF.format(item.get(4))
            });
        }

        JTable table = new JTable(model) {
            @Override
            protected void paintComponent(Graphics g) {
                if (watermarkImage != null) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f)); 
                    int imgWidth = 200; 
                    int imgHeight = 200;
                    int x = (getWidth() - imgWidth) / 2;
                    int y = (getHeight() - imgHeight) / 2;
                    g2d.drawImage(watermarkImage, x, y, imgWidth, imgHeight, null);
                    g2d.dispose();
                }
                super.paintComponent(g);
            }
        };

        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.setRowHeight(28);
        table.setFont(getCairoFont(12f));
        table.setForeground(Color.BLACK);
        
        table.setOpaque(false);
        ((JComponent)table.getDefaultRenderer(Object.class)).setOpaque(false);
        table.setShowGrid(true);
        table.setGridColor(Color.GRAY);
        table.setIntercellSpacing(new Dimension(1, 1));

        JTableHeader header = table.getTableHeader();
        header.setFont(getCairoFont(12f).deriveFont(Font.BOLD));
        header.setBackground(new Color(245, 245, 245));
        header.setForeground(Color.BLACK);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        centerRenderer.setOpaque(false);
        centerRenderer.setForeground(Color.BLACK);
        
        for (int i = 0; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);

        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);

        int totalHeight = table.getTableHeader().getPreferredSize().height + (table.getRowHeight() * model.getRowCount());
        table.setPreferredScrollableViewportSize(new Dimension(table.getPreferredScrollableViewportSize().width, totalHeight));

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(Color.WHITE);
        tablePanel.add(header, BorderLayout.NORTH);
        tablePanel.add(table, BorderLayout.CENTER);
        tablePanel.setBorder(new MatteBorder(1, 1, 1, 1, Color.BLACK));
        
        return tablePanel;
    }

    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new GridBagLayout());
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 1. المجاميع
        JPanel balancePanel = new JPanel(new GridLayout(1, 5));
        balancePanel.setBorder(new MatteBorder(1, 1, 1, 1, Color.BLACK));
        balancePanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // منطق الحالة والألوان
        String statusText;
        Color statusColor; // هذا المتغير الوحيد الملون

        if (invoiceData.currentBalance < 0) {
            statusText = "دائن (له)";
            statusColor = new Color(0, 128, 0); // أخضر
        } else if (invoiceData.currentBalance > 0) {
            statusText = "مدين (عليه)";
            statusColor = Color.RED; // أحمر
        } else {
            statusText = "خالص";
            statusColor = Color.BLACK;
        }
        
        // جميع الأرقام والعناوين سوداء
        balancePanel.add(createBalanceCell("رصيد سابق", DF.format(Math.abs(invoiceData.priorBalance)), Color.BLACK));
        balancePanel.add(createBalanceCell("باقي الفاتورة", DF.format(invoiceData.remainingOnInvoice), Color.BLACK));
        balancePanel.add(createBalanceCell("المدفوع", DF.format(invoiceData.paidAmount), Color.BLACK));
        balancePanel.add(createBalanceCell("الرصيد الحالي", DF.format(Math.abs(invoiceData.currentBalance)), Color.BLACK));
        
        // خانة الحالة فقط هي الملونة
        balancePanel.add(createBalanceCell("الحالة", statusText, statusColor)); 

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0;
        footerPanel.add(balancePanel, gbc);

        // 2. الإجمالي
        JPanel totalsPanel = new JPanel(new BorderLayout());
        totalsPanel.setOpaque(false);
        
        JLabel totalLabel = new JLabel(" الإجمالي: " + DF.format(invoiceData.totalAmount) + " ج.م ", SwingConstants.LEFT);
        totalLabel.setFont(getCairoFont(14f).deriveFont(Font.BOLD));
        totalLabel.setForeground(Color.BLACK);
        totalLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        String tafqeet = "";
        try { tafqeet = Tafqeet.convertToWords(BigDecimal.valueOf(invoiceData.totalAmount), "جنيه", "قرش"); } catch (Exception e) {}
        JLabel tafqeetLabel = new JLabel(tafqeet + " ", SwingConstants.RIGHT);
        tafqeetLabel.setFont(getCairoFont(12f));
        tafqeetLabel.setForeground(Color.BLACK);

        totalsPanel.add(totalLabel, BorderLayout.WEST);
        totalsPanel.add(tafqeetLabel, BorderLayout.CENTER);

        gbc.gridy = 1;
        footerPanel.add(totalsPanel, gbc);

        // 3. التوقيعات
        JPanel signaturePanel = new JPanel(new GridLayout(1, 2, 100, 0));
        signaturePanel.setBackground(Color.WHITE);
        signaturePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 5, 20));
        signaturePanel.add(createSignatureArea("المستلم"));
        signaturePanel.add(createSignatureArea("إدارة المعرض"));

        gbc.gridy = 2;
        footerPanel.add(signaturePanel, gbc);

        return footerPanel;
    }

    // دالة الخلية (لون افتراضي أسود)
    private JPanel createBalanceCell(String title, String value) {
        return createBalanceCell(title, value, Color.BLACK);
    }

    // دالة الخلية (لون مخصص)
    private JPanel createBalanceCell(String title, String value, Color valueColor) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBackground(Color.WHITE);
        cell.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.BLACK));
        
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(getCairoFont(11f));
        t.setForeground(Color.BLACK); 
        t.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
        
        JLabel v = new JLabel(value, SwingConstants.CENTER);
        v.setFont(getCairoFont(12f).deriveFont(Font.BOLD));
        v.setForeground(valueColor); // هنا يتم تطبيق اللون
        
        cell.add(t, BorderLayout.NORTH);
        cell.add(v, BorderLayout.CENTER);
        return cell;
    }

    private JPanel createSignatureArea(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel("....................", SwingConstants.CENTER);
        l.setForeground(Color.BLACK);
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(getCairoFont(12f).deriveFont(Font.BOLD));
        t.setForeground(Color.BLACK);
        p.add(l, BorderLayout.CENTER);
        p.add(t, BorderLayout.SOUTH);
        return p;
    }
}
