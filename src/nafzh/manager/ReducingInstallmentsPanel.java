package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static nafzh.manager.NafzhManager.getCairoFont;

public class ReducingInstallmentsPanel extends JPanel {

    private final DatabaseManager dbManager;
    private final double totalAmount;
    private final Map<Integer, Integer> cartItems;
    private final Window parentWindow;

    private JTable installmentsTable;
    private DefaultTableModel tableModel;
    private JTextField dateField;
    private JComboBox<DatabaseManager.Customer> customerCombo;
    private JTextField downPaymentField;
    private JTextField monthsField;
    private JTextField profitRateField;
    private JLabel infoLabel;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ReducingInstallmentsPanel(DatabaseManager dbManager, double totalAmount, Map<Integer, Integer> cartItems, Window parentWindow) {
        this.dbManager = dbManager;
        this.totalAmount = totalAmount;
        this.cartItems = cartItems;
        this.parentWindow = parentWindow;

        initializeUI();
    }

        private void initializeUI() {
        setLayout(new BorderLayout(20, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- لوحة المدخلات (يمين) ---
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setOpaque(false);
        inputPanel.setPreferredSize(new Dimension(300, 0));

        customerCombo = new JComboBox<>();
        customerCombo.setFont(getCairoFont(16f));
        loadCustomers();

        dateField = createStyledTextField(LocalDate.now().format(formatter), "تاريخ البدء (YYYY-MM-DD)");
        downPaymentField = createStyledTextField("0", "المبلغ المقدم");
        monthsField = createStyledTextField("12", "عدد الشهور");
        profitRateField = createStyledTextField("0", "نسبة الربح السنوية %");

        inputPanel.add(createFieldWrapper(customerCombo, "اختيار العميل"));
        inputPanel.add(Box.createVerticalStrut(15));
        inputPanel.add(createFieldWrapper(dateField, "تاريخ أول قسط"));
        inputPanel.add(Box.createVerticalStrut(15));
        inputPanel.add(createFieldWrapper(downPaymentField, " المقدمة (ج.م)"));
        inputPanel.add(Box.createVerticalStrut(15));
        inputPanel.add(createFieldWrapper(monthsField, "فترة التقسيط (شهور)"));
        inputPanel.add(Box.createVerticalStrut(15));
        inputPanel.add(createFieldWrapper(profitRateField, "هامش الربح % (اختياري)"));

        JPanel actionButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        actionButtons.setOpaque(false);
        actionButtons.setMaximumSize(new Dimension(300, 50));

        JButton btnCalc = new JButton("توليد الجدول المتناقص");
        btnCalc.setFont(getCairoFont(12f));
        btnCalc.setBackground(new Color(52, 152, 219));
        btnCalc.setForeground(Color.BLACK);
        btnCalc.addActionListener(e -> calculatePlan());

        JButton btnConfirm = new JButton("إعتماد الأقساط");
        btnConfirm.setFont(getCairoFont(14f));
        btnConfirm.setBackground(new Color(46, 204, 113));
        btnConfirm.setForeground(Color.BLACK);
        btnConfirm.addActionListener(e -> handleConfirm());

        actionButtons.add(btnConfirm);
        actionButtons.add(btnCalc);

        inputPanel.add(Box.createVerticalStrut(30));
        inputPanel.add(actionButtons);

        add(inputPanel, BorderLayout.EAST);

        // --- لوحة الجدول (وسط) ---
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        String[] cols = {"م", "تاريخ الاستحقاق", "تاريخ السداد", "قيمة القسط (متناقص)", "الحالة"};
        tableModel = new DefaultTableModel(cols, 0);
        installmentsTable = new JTable(tableModel);
        setupTableUI();

        JScrollPane scroll = new JScrollPane(installmentsTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        tableContainer.add(scroll, BorderLayout.CENTER);

        // --- التصحيح هنا: عرض المبلغ الإجمالي الممرر بدلاً من 0.00 ---
        infoLabel = new JLabel("إجمالي الفاتورة: " + String.format(Locale.ENGLISH, "%.2f", totalAmount) + " ج.م");
        // -----------------------------------------------------------
        
        infoLabel.setFont(getCairoFont(18f));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        tableContainer.add(infoLabel, BorderLayout.SOUTH);

        add(tableContainer, BorderLayout.CENTER);
    }

    // =========================================================================
    // === المنطق الحسابي للقسط المتناقص ===
    // =========================================================================
    private void calculatePlan() {
        try {
            tableModel.setRowCount(0);
            
            double downPayment = Double.parseDouble(downPaymentField.getText());
            int months = Integer.parseInt(monthsField.getText());
            double annualProfitRate = Double.parseDouble(profitRateField.getText());
            
            // 1. حساب أصل الدين الصافي
            double principalLoan = totalAmount - downPayment;
            
            // 2. حساب الجزء الثابت من أصل الدين الذي يسدد شهرياً
            double fixedMonthlyPrincipal = principalLoan / months;
            
            // 3. تحويل الفائدة السنوية لشهرية (النسبة المئوية)
            double monthlyInterestRate = (annualProfitRate / 100.0) / 12.0;

            LocalDate startDate = LocalDate.parse(dateField.getText(), formatter);
            LocalDate today = LocalDate.now();

            double currentBalance = principalLoan; // الرصيد المتبقي (يبدأ بكامل الدين)
            double totalPaidWithInterest = downPayment; // لحساب الإجمالي النهائي

            for (int i = 1; i <= months; i++) {
                // أ. حساب الفائدة لهذا الشهر بناءً على الرصيد المتبقي
                double interestForThisMonth = currentBalance * monthlyInterestRate;
                
                // ب. القسط الإجمالي لهذا الشهر = (جزء الأصل الثابت) + (فائدة الرصيد الحالي)
                double totalInstallmentForMonth = fixedMonthlyPrincipal + interestForThisMonth;
                
                // ج. تحديث الرصيد المتبقي للشهر القادم
                currentBalance -= fixedMonthlyPrincipal;
                
                // د. تجميع الإجمالي
                totalPaidWithInterest += totalInstallmentForMonth;

                // هـ. تحديد التاريخ والحالة (نفس منطق اللوحة السابقة)
                LocalDate dueDate = startDate.plusMonths(i - 1);
                String status;
                String paymentDateStr;

                if (dueDate.isBefore(today)) {
                    long monthsDifference = ChronoUnit.MONTHS.between(dueDate, today);
                    if (monthsDifference >= 1) {
                        status = "خالص";
                        paymentDateStr = dueDate.format(formatter);
                    } else {
                        status = "متأخر";
                        paymentDateStr = "-";
                    }
                } else {
                    status = "انتظار";
                    paymentDateStr = "-";
                }

                tableModel.addRow(new Object[]{
                    i,
                    dueDate.format(formatter),
                    paymentDateStr,
                    totalInstallmentForMonth, // القيمة هنا ستكون مختلفة كل سطر (متناقصة)
                    status
                });
            }

            infoLabel.setText(String.format(Locale.ENGLISH, "الإجمالي النهائي: %.2f ج.م", totalPaidWithInterest));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "يرجى التأكد من صحة البيانات");
            e.printStackTrace();
        }
    }

    private void handleConfirm() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "يجب توليد خطة الأقساط أولاً");
            return;
        }

        DatabaseManager.Customer selectedCustomer = (DatabaseManager.Customer) customerCombo.getSelectedItem();
        if (selectedCustomer == null) {
            JOptionPane.showMessageDialog(this, "يرجى اختيار عميل");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "هل تريد إعتماد هذه الأقساط وحفظها؟", "تأكيد الحفظ", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            double downPayment = Double.parseDouble(downPaymentField.getText());
            List<Object[]> installmentsData = new ArrayList<>();

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String dueDateStr = (String) tableModel.getValueAt(i, 1);
                String paymentDateDisplay = (String) tableModel.getValueAt(i, 2);
                double amount = (Double) tableModel.getValueAt(i, 3);
                String statusStr = (String) tableModel.getValueAt(i, 4);

                boolean isPaid = "خالص".equals(statusStr);
                String actualPaymentDate = isPaid ? paymentDateDisplay : "-";

                installmentsData.add(new Object[]{
                    i + 1,
                    dueDateStr,
                    amount,
                    actualPaymentDate,
                    isPaid
                });
            }

            boolean success = dbManager.saveInstallmentSale(
                    selectedCustomer.id,
                    totalAmount,
                    downPayment,
                    cartItems,
                    installmentsData
            );

            if (success) {
                JOptionPane.showMessageDialog(this, "تم حفظ العملية بنجاح.");
                if (parentWindow != null) parentWindow.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "حدث خطأ غير متوقع أثناء الحفظ.");
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "خطأ أثناء الحفظ: " + ex.getMessage());
        }
    }

    private void loadCustomers() {
        List<DatabaseManager.Customer> customers = dbManager.getAllCustomersForPOS();
        for (DatabaseManager.Customer c : customers) {
            customerCombo.addItem(c);
        }
    }

    private JTextField createStyledTextField(String text, String tooltip) {
        JTextField field = new JTextField(text);
        field.setFont(getCairoFont(16f));
        field.setPreferredSize(new Dimension(0, 42));
        field.setToolTipText(tooltip);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return field;
    }

    private JPanel createFieldWrapper(JComponent component, String labelText) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(getCairoFont(13f));
        lbl.setForeground(new Color(80, 80, 80));
        p.add(lbl, BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    private void setupTableUI() {
        installmentsTable.setRowHeight(35);
        installmentsTable.setFont(getCairoFont(14f));
        installmentsTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        JTableHeader header = installmentsTable.getTableHeader();
        header.setFont(getCairoFont(14f));
        header.setBackground(new Color(245, 245, 245));

        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                String status = (String) value;
                if ("خالص".equals(status)) {
                    setForeground(new Color(46, 204, 113));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if ("متأخر".equals(status)) {
                    setForeground(new Color(231, 76, 60));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(Color.BLACK);
                }
                return this;
            }
        };

        DefaultTableCellRenderer englishNumberRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Number) {
                    value = String.format(Locale.ENGLISH, "%.2f", ((Number) value).doubleValue());
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        };
        englishNumberRenderer.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < installmentsTable.getColumnCount(); i++) {
            if (i == 3) { // عمود المبلغ
                installmentsTable.getColumnModel().getColumn(i).setCellRenderer(englishNumberRenderer);
            } else if (i == 4) { // عمود الحالة
                installmentsTable.getColumnModel().getColumn(i).setCellRenderer(statusRenderer);
            } else {
                installmentsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }
    }
}
