package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Collections;
import javax.swing.table.JTableHeader;
import static nafzh.manager.NafzhManager.getCairoFont;
import nafzh.manager.TableActionRendererEditor.ActionType;

public class TransactionsPanel extends JPanel {
    private final DatabaseManager dbManager;
    private final NafzhManager parentFrame;
    private JTable transactionsTable;
    private DefaultTableModel tableModel;
    private List<Integer> saleIds;
    private JButton printInvoiceBtn;
    private static final String[] COLUMN_NAMES = {"ID", "تاريخ البيع", "اسم العميل", "الإجمالي", "تحديد", "الإجراءات"};

    public TransactionsPanel(DatabaseManager dbManager, NafzhManager parentFrame) {
        this.dbManager = dbManager;
        this.parentFrame = parentFrame;
        this.saleIds = Collections.emptyList();
        initializePanel();
        loadData();
    }

    // قم باستبدال دالة initializePanel الحالية في ملف TransactionsPanel.java بهذا الكود
    private void initializePanel() {
    setLayout(new BorderLayout(10, 10));
    setBackground(Color.WHITE); // تعديل الخلفية للأبيض
    // التعديل 1: توحيد الهوامش لتطابق باقي الصفحات (20 بدلاً من أي قيمة أخرى)
    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    setName("Transactions");

    // --- اللوحة العلوية (الأزرار) ---
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    topPanel.setOpaque(false);
    
    printInvoiceBtn = new JButton("طباعة الفاتورة");
    printInvoiceBtn.setBackground(new Color(33, 150, 243));
    printInvoiceBtn.setForeground(Color.BLACK);
    printInvoiceBtn.setFont(getCairoFont(12f));
    printInvoiceBtn.addActionListener(e -> handlePrintInvoiceAction());
    
    JButton refreshButton = new JButton("تحديث");
    refreshButton.setBackground(new Color(103, 58, 183));
    refreshButton.setForeground(Color.BLACK);
    refreshButton.setFont(getCairoFont(12f));
    refreshButton.addActionListener(e -> loadData());
    
    topPanel.add(printInvoiceBtn);
    topPanel.add(refreshButton);
    add(topPanel, BorderLayout.NORTH);

    // --- إعداد الجدول ---
    tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            // السماح بتعديل عمود الاختيار (4) وعمود الأزرار (5)
            return column == 5 || column == 4;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            if (column == 4) return Boolean.class; // Checkbox
            if (column == 0) return Integer.class; // ID
            if (column == 3) return Double.class;  // Total
            return super.getColumnClass(column);
        }
    };
    
    transactionsTable = new JTable(tableModel);
    
    // استدعاء دالة التخصيص الجديدة
    customizeTable(transactionsTable);

    // إعداد عمود الإجراءات (عرض)
    TableColumn actionColumn = transactionsTable.getColumnModel().getColumn(5);
    TableActionRendererEditor actionRendererEditor = new TableActionRendererEditor(
            transactionsTable,
            null, null, this::handleViewAction, ActionType.VIEW_ONLY
    );
    actionColumn.setCellRenderer(actionRendererEditor);
    actionColumn.setCellEditor(actionRendererEditor);
    actionColumn.setPreferredWidth(100);
    actionColumn.setMinWidth(100);
    actionColumn.setMaxWidth(100);
    
    transactionsTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    JScrollPane scrollPane = new JScrollPane(transactionsTable);
    // التعديل 3: جعل خلفية المنطقة الحاوية للجدول بيضاء
    scrollPane.getViewport().setBackground(Color.WHITE);
    
    add(scrollPane, BorderLayout.CENTER);
}


    public void loadData() {
        List<List<Object>> data = dbManager.getAllSaleTransactions();
        tableModel.setRowCount(0);
        saleIds = new ArrayList<>();
        for (List<Object> rowData : data) {
            Vector<Object> row = new Vector<>(rowData);
            saleIds.add((Integer) row.get(0));
            row.add(Boolean.FALSE);
            row.add("عرض");
            tableModel.addRow(row);
        }
        customizeTable(transactionsTable);
    }

    private void handlePrintInvoiceAction() {
            List<Integer> selectedSaleIds = new ArrayList<>();
            int checkboxColumnIndex = 4;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Object value = tableModel.getValueAt(i, checkboxColumnIndex);
                if (value instanceof Boolean && (Boolean) value) {
                    if (i < saleIds.size()) {
                        selectedSaleIds.add(saleIds.get(i));
                    }
                }
            }
            if (selectedSaleIds.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "يرجى تحديد حركة مبيعات واحدة على الأقل لطباعة الفاتورة.",
                        "تنبيه",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Corrected: Loop through all selected IDs and open a dialog for each
            Frame owner = JOptionPane.getFrameForComponent(this);
            for (int saleId : selectedSaleIds) {
                new PrintDialog(owner, saleId, dbManager).setVisible(true);
            }
        }

    private void handleViewAction(int row) {
        if (row >= 0 && row < saleIds.size()) {
            int saleId = saleIds.get(row);

            // التحقق: هل هذه العملية تقسيط أم كاش؟
            // نعرف ذلك ببساطة: إذا كان لها سجلات في جدول installments فهي تقسيط
            boolean isInstallment = dbManager.isInstallmentSale(saleId);

            if (isInstallment) {
                // عرض نافذة التقسيط الجديدة الفخمة
                new InstallmentDetailsDialog((Frame) SwingUtilities.getWindowAncestor(this), saleId).setVisible(true);
            } else {
                // عرض نافذة الكاش العادية (الكود القديم البسيط)
                showCashDetailsDialog(saleId);
            }
        }
    }

    private void showCashDetailsDialog(int saleId) {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "تفاصيل فاتورة البيع", true);
            dialog.setSize(800, 500); 
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());
            dialog.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

            List<List<Object>> items = dbManager.getSaleItems(saleId);
            double realTotal = dbManager.getRealSaleTotal(saleId); 

            String[] columnNames = {"م", "الصنف", "الكمية", "السعر", "الإجمالي"};
            DefaultTableModel detailsModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int r, int c) { return false; }
            };

            int seq = 1;
            double itemsSum = 0; 
            for (List<Object> itemRow : items) {
                Object name = itemRow.get(0);
                Object unit = itemRow.get(1);
                Object qty = itemRow.get(2);
                Object price = itemRow.get(3);
                Object subTotal = itemRow.get(4); 
                String qtyWithUnit = qty + " " + unit;
                itemsSum += Double.parseDouble(subTotal.toString());
                detailsModel.addRow(new Object[]{seq++, name, qtyWithUnit, price, subTotal});
            }

            double difference = realTotal - itemsSum;
            if (difference > 0) {
                detailsModel.addRow(new Object[]{seq++, "رسوم إضافية/أخرى", "-", difference, difference});
            }

            JTable detailsTable = new JTable(detailsModel);
            styleTable(detailsTable); 

            JScrollPane scrollPane = new JScrollPane(detailsTable);
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            dialog.add(scrollPane, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
            bottomPanel.setBackground(new Color(245, 245, 245));
            bottomPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            JLabel totalLabel = new JLabel("إجمالي الفاتورة: " + String.format("%.2f", realTotal) + " ج.م");
            totalLabel.setFont(getCairoFont(14f).deriveFont(Font.BOLD));
            totalLabel.setForeground(new Color(44, 62, 80));
            JButton closeBtn = new JButton("إغلاق");
            closeBtn.setFont(getCairoFont(12f));
            closeBtn.addActionListener(e -> dialog.dispose());
            bottomPanel.add(totalLabel);
            bottomPanel.add(closeBtn);
            dialog.add(bottomPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);
        }

    private void customizeTable(JTable table) {
    // استدعاء دالة التنسيق الموحدة (لضبط الألوان والخطوط والهيدر)
    styleTable(table);

    // --- تخصيصات إضافية خاصة بالجدول الرئيسي فقط ---
    
    // ضبط عروض الأعمدة
    table.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
    table.getColumnModel().getColumn(1).setPreferredWidth(150); // التاريخ
    table.getColumnModel().getColumn(2).setPreferredWidth(200); // العميل
    table.getColumnModel().getColumn(3).setPreferredWidth(120); // الإجمالي
    
    // عمود الاختيار (Checkbox)
    table.getColumnModel().getColumn(4).setPreferredWidth(60);
    table.getColumnModel().getColumn(4).setMinWidth(60);
    table.getColumnModel().getColumn(4).setMaxWidth(60);
    table.getTableHeader().getColumnModel().getColumn(4).setHeaderValue("✔");

    // عمود الإجراءات
    if (table.getColumnCount() > 5) {
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setMinWidth(100);
        table.getColumnModel().getColumn(5).setMaxWidth(100);
    }
}

    void loadTransactionsData() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
 
    private void styleTable(JTable table) {
        // 1. الإعدادات الأساسية
        table.setRowHeight(35);
        table.setFont(getCairoFont(12f));
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // 2. تفعيل الشبكة والألوان
        table.setShowGrid(true);
        table.setGridColor(new Color(189, 195, 49)); // اللون الزيتوني
        table.setBackground(Color.WHITE);

        // 3. تنسيق رأس الجدول (Header) مع إجبار ظهور الخطوط
        JTableHeader header = table.getTableHeader();
        header.setFont(getCairoFont(12f));
        header.setReorderingAllowed(false);

        // ريندر مخصص للهيدر يرسم الحدود (الخيوط) بوضوح
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setBackground(new Color(245, 245, 245)); // خلفية رمادية فاتحة
                l.setForeground(Color.BLACK);
                l.setHorizontalAlignment(JLabel.CENTER);
                l.setFont(getCairoFont(12f)); // تأكيد الخط

                // رسم حدود للخلية: أسفل ويمين (للفصل بين الأعمدة)
                l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(189, 195, 49)), // الحدود
                    BorderFactory.createEmptyBorder(5, 5, 5, 5) // هوامش داخلية
                ));
                return l;
            }
        };

        // تطبيق الريندر على جميع أعمدة الهيدر
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // 4. توسيط محتوى الخلايا
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            // لا نطبق التوسيط على الأعمدة الخاصة (مثل الـ Checkbox في الجدول الرئيسي)
            // لكن هنا سنطبقه بشكل عام، ويمكن تخصيصه لاحقاً
            if (!table.getColumnClass(i).equals(Boolean.class)) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }
    }

    // استبدل كلاس InstallmentDetailsDialog القديم بهذا الإصدار "الدقيق" والرباعي
    private class InstallmentDetailsDialog extends JDialog {

        public InstallmentDetailsDialog(Frame owner, int saleId) {
            super(owner, "تفاصيل عملية بيع بالتقسيط", true);
            setSize(1000, 700); // زيادة العرض لاستيعاب المربع الرابع
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10, 10));
            setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            getContentPane().setBackground(Color.WHITE);

            // 1. جلب البيانات والحسابات الدقيقة
            double realTotal = dbManager.getRealSaleTotal(saleId);     // إجمالي الفاتورة (60580)
            double downPayment = dbManager.getSaleDownPayment(saleId); // المقدم (4000)

            // جلب قائمة الأقساط لحساب المدفوع والمتبقي بدقة
            List<DatabaseManager.Installment> installments = dbManager.getInstallmentsBySaleId(saleId);

            double paidInstallments = 0;   // مجموع الأقساط التي تم سدادها
            double remainingInstallments = 0; // مجموع الأقساط المتبقية (الديون)

            for (DatabaseManager.Installment inst : installments) {
                if (inst.isPaid) {
                    paidInstallments += inst.amount;
                } else {
                    remainingInstallments += inst.amount;
                }
            }

            // 2. شريط الملخص العلوي (4 مربعات الآن)
            JPanel headerPanel = new JPanel(new GridLayout(1, 4, 10, 0)); // أصبح 4 أعمدة
            headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));
            headerPanel.setBackground(Color.WHITE);
            headerPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

            // المربع 1: إجمالي الفاتورة (أزرق)
            headerPanel.add(createSummaryCard("إجمالي الفاتورة", realTotal, new Color(41, 128, 185))); 

            // المربع 2: المقدم المدفوع (أخضر غامق)
            headerPanel.add(createSummaryCard("المقدم المدفوع", downPayment, new Color(22, 160, 133)));   

            // المربع 3: الأقساط المدفوعة (أخضر فاتح - جديد!)
            // هذا هو المربع الذي طلبته: يوضح كم قسط تم دفعه فعلياً
            headerPanel.add(createSummaryCard("تم سداد (أقساط)", paidInstallments, new Color(39, 174, 96)));

            // المربع 4: المتبقي (أحمر - دين حالي)
            headerPanel.add(createSummaryCard("المتبقي (أقساط)", remainingInstallments, new Color(192, 57, 43))); 

            add(headerPanel, BorderLayout.NORTH);

            // 3. المحتوى (Tabs)
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.setFont(getCairoFont(12f));
            tabbedPane.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

            // تبويب 1: الأصناف
            tabbedPane.addTab("الأصناف المباعة", createItemsPanel(saleId));

            // تبويب 2: جدول الأقساط (نمرر القائمة التي جلبناها لتوفير استعلام)
            tabbedPane.addTab("جدول الأقساط", createInstallmentsPanel(installments));

            add(tabbedPane, BorderLayout.CENTER);

            // زر إغلاق
            JButton closeBtn = new JButton("إغلاق");
            closeBtn.setFont(getCairoFont(12f));
            closeBtn.addActionListener(e -> dispose());
            JPanel btnPanel = new JPanel();
            btnPanel.setBackground(Color.WHITE);
            btnPanel.add(closeBtn);
            add(btnPanel, BorderLayout.SOUTH);
        }

        private JPanel createSummaryCard(String title, double amount, Color color) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            card.setBackground(Color.WHITE);

            JLabel titleLbl = new JLabel(title);
            titleLbl.setFont(getCairoFont(13f)); // تكبير الخط قليلاً
            titleLbl.setHorizontalAlignment(JLabel.CENTER);

            JLabel amountLbl = new JLabel(String.format("%.2f", amount));
            amountLbl.setFont(getCairoFont(20f).deriveFont(Font.BOLD)); // رقم كبير وواضح
            amountLbl.setForeground(color);
            amountLbl.setHorizontalAlignment(JLabel.CENTER);

            card.add(titleLbl, BorderLayout.NORTH);
            card.add(amountLbl, BorderLayout.CENTER);
            return card;
        }

        private JScrollPane createItemsPanel(int saleId) {
            List<List<Object>> items = dbManager.getSaleItems(saleId);
            // ... (نفس كود الأصناف السابق، لا تغيير فيه) ...
            String[] cols = {"م", "الصنف", "الكمية", "السعر", "الإجمالي"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);
            int seq = 1;
            for (List<Object> row : items) {
                 model.addRow(new Object[]{seq++, row.get(0), row.get(2) + " " + row.get(1), row.get(3), row.get(4)});
            }
            JTable table = new JTable(model);
            styleTable(table);
            JScrollPane sp = new JScrollPane(table);
            sp.getViewport().setBackground(Color.WHITE);
            return sp;
        }

        // تم تعديل الدالة لتستقبل القائمة مباشرة بدلاً من saleId
        private JScrollPane createInstallmentsPanel(List<DatabaseManager.Installment> insts) {
            String[] cols = {"م", "تاريخ الاستحقاق", "المبلغ", "تاريخ السداد", "الحالة"};
            DefaultTableModel model = new DefaultTableModel(cols, 0);

            int seq = 1;
            for (DatabaseManager.Installment inst : insts) {
                model.addRow(new Object[]{
                    seq++,
                    inst.dueDate,
                    inst.amount,
                    inst.paymentDate,
                    inst.isPaid ? "تم السداد" : "مستحق"
                });
            }

            JTable table = new JTable(model);
            styleTable(table);

            // تلوين وتوسيط الحالة (كما طلبت سابقاً)
            table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    l.setHorizontalAlignment(JLabel.CENTER); // توسيط
                    if ("تم السداد".equals(value)) {
                        l.setForeground(new Color(39, 174, 96));
                        l.setFont(l.getFont().deriveFont(Font.BOLD));
                    } else {
                        l.setForeground(new Color(192, 57, 43));
                        l.setFont(l.getFont().deriveFont(Font.BOLD));
                    }
                    return l;
                }
            });

            JScrollPane sp = new JScrollPane(table);
            sp.getViewport().setBackground(Color.WHITE);
            return sp;
        }
    }



}
