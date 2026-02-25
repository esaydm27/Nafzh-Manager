package nafzh.manager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

import static nafzh.manager.NafzhManager.getCairoFont;

public class BalloonInstallmentsPanel extends JPanel {

    private final DatabaseManager dbManager;
    private final double totalAmount;
    private final Map<Integer, Integer> cartItems;
    private final Window parentWindow;

    private JTable installmentsTable;
    private DefaultTableModel tableModel;
    
    // حقول الإدخال الأساسية
    private JTextField dateField;
    private JComboBox<DatabaseManager.Customer> customerCombo;
    private JTextField downPaymentField;
    private JTextField monthsField;
    private JTextField profitRateField;
    
    // حقول الكباري
    private JTextField balloonMonthField;
    private JTextField balloonAmountField;
    private DefaultListModel<String> balloonsListModel;
    private JList<String> balloonsList;
    private Map<Integer, Double> balloonsMap; // لتخزين الكباري (الشهر -> المبلغ)

    private JLabel infoLabel;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public BalloonInstallmentsPanel(DatabaseManager dbManager, double totalAmount, Map<Integer, Integer> cartItems, Window parentWindow) {
        this.dbManager = dbManager;
        this.totalAmount = totalAmount;
        this.cartItems = cartItems;
        this.parentWindow = parentWindow;
        this.balloonsMap = new HashMap<>();

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
        inputPanel.setPreferredSize(new Dimension(320, 0)); // زيادة العرض قليلاً

        customerCombo = new JComboBox<>();
        customerCombo.setFont(getCairoFont(14f));
        loadCustomers();

        dateField = createStyledTextField(LocalDate.now().format(formatter), "YYYY-MM-DD");
        downPaymentField = createStyledTextField("0", "المبلغ المقدم");
        monthsField = createStyledTextField("12", "عدد الشهور");
        profitRateField = createStyledTextField("0", "نسبة الربح السنوية %");

        // إضافة الحقول الأساسية
        inputPanel.add(createFieldWrapper(customerCombo, "اختيار العميل"));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createFieldWrapper(dateField, "تاريخ أول قسط"));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createFieldWrapper(downPaymentField, " المقدمة (ج.م)"));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createFieldWrapper(monthsField, "فترة التقسيط (شهور)"));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createFieldWrapper(profitRateField, "هامش الربح % (على المتبقي)"));
        inputPanel.add(Box.createVerticalStrut(15));

        // --- قسم إدارة الكباري ---
        JPanel balloonPanel = new JPanel(new GridBagLayout());
        balloonPanel.setOpaque(false);
        balloonPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                "إضافة كباري (دفعات استثنائية)",
                TitledBorder.RIGHT, TitledBorder.TOP, getCairoFont(12f), Color.BLUE));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // الصف الأول: الحقول
        gbc.gridx = 0; gbc.gridy = 0;
        balloonPanel.add(new JLabel("رقم القسط:"), gbc);
        
        balloonMonthField = new JTextField(3);
        balloonMonthField.setHorizontalAlignment(JTextField.CENTER);
        gbc.gridx = 1; 
        balloonPanel.add(balloonMonthField, gbc);
        
        gbc.gridx = 2;
        balloonPanel.add(new JLabel("المبلغ:"), gbc);
        
        balloonAmountField = new JTextField(5);
        gbc.gridx = 3;
        balloonPanel.add(balloonAmountField, gbc);

        // الصف الثاني: الأزرار والقائمة
        JButton addBalloonBtn = new JButton("إضافة");
        addBalloonBtn.setBackground(new Color(46, 204, 113));
        addBalloonBtn.setForeground(Color.WHITE);
        addBalloonBtn.setFont(getCairoFont(11f));
        addBalloonBtn.addActionListener(e -> addBalloon());
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 4;
        balloonPanel.add(addBalloonBtn, gbc);

        balloonsListModel = new DefaultListModel<>();
        balloonsList = new JList<>(balloonsListModel);
        balloonsList.setFont(getCairoFont(11f));
        JScrollPane listScroll = new JScrollPane(balloonsList);
        listScroll.setPreferredSize(new Dimension(0, 60));
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4;
        balloonPanel.add(listScroll, gbc);
        
        JButton removeBalloonBtn = new JButton("حذف المحدد");
        removeBalloonBtn.setFont(getCairoFont(10f));
        removeBalloonBtn.setForeground(Color.RED);
        removeBalloonBtn.addActionListener(e -> removeSelectedBalloon());
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        balloonPanel.add(removeBalloonBtn, gbc);

        inputPanel.add(balloonPanel);
        inputPanel.add(Box.createVerticalStrut(15));

        // --- أزرار التحكم السفلية ---
        JPanel actionButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        actionButtons.setOpaque(false);
        actionButtons.setMaximumSize(new Dimension(300, 40));

        JButton btnCalc = new JButton("توليد الخطة");
        btnCalc.setFont(getCairoFont(14f));
        btnCalc.setBackground(new Color(52, 152, 219));
        btnCalc.setForeground(Color.BLACK);
        btnCalc.addActionListener(e -> calculatePlan());

        JButton btnConfirm = new JButton("إعتماد");
        btnConfirm.setFont(getCairoFont(14f));
        btnConfirm.setBackground(new Color(46, 204, 113));
        btnConfirm.setForeground(Color.BLACK);
        btnConfirm.addActionListener(e -> handleConfirm());

        actionButtons.add(btnConfirm);
        actionButtons.add(btnCalc);

        inputPanel.add(actionButtons);
        add(inputPanel, BorderLayout.EAST);

        // --- لوحة الجدول (وسط) ---
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);

        String[] cols = {"م", "تاريخ الاستحقاق", "تاريخ السداد", "قيمة القسط", "الحالة"};
        tableModel = new DefaultTableModel(cols, 0);
        installmentsTable = new JTable(tableModel);
        setupTableUI();

        JScrollPane scroll = new JScrollPane(installmentsTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        tableContainer.add(scroll, BorderLayout.CENTER);

        infoLabel = new JLabel("إجمالي الفاتورة: " + String.format(Locale.ENGLISH, "%.2f", totalAmount) + " ج.م");
        infoLabel.setFont(getCairoFont(18f));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        tableContainer.add(infoLabel, BorderLayout.SOUTH);

        add(tableContainer, BorderLayout.CENTER);
    }

    // --- منطق الكباري ---
    private void addBalloon() {
        try {
            int month = Integer.parseInt(balloonMonthField.getText().trim());
            double amount = Double.parseDouble(balloonAmountField.getText().trim());
            int totalMonths = Integer.parseInt(monthsField.getText().trim());

            if (month < 1 || month > totalMonths) {
                JOptionPane.showMessageDialog(this, "رقم القسط يجب أن يكون بين 1 و " + totalMonths);
                return;
            }
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "المبلغ يجب أن يكون أكبر من صفر");
                return;
            }

            balloonsMap.put(month, amount);
            updateBalloonList();
            balloonMonthField.setText("");
            balloonAmountField.setText("");
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "أدخل أرقام صحيحة");
        }
    }

    private void removeSelectedBalloon() {
        int index = balloonsList.getSelectedIndex();
        if (index != -1) {
            String val = balloonsListModel.getElementAt(index);
            // استخراج رقم الشهر من النص (شهر X: ...)
            int month = Integer.parseInt(val.split(":")[0].replace("شهر ", ""));
            balloonsMap.remove(month);
            updateBalloonList();
        }
    }

    private void updateBalloonList() {
        balloonsListModel.clear();
        // ترتيب العرض حسب رقم الشهر
        new TreeMap<>(balloonsMap).forEach((month, amount) -> 
            balloonsListModel.addElement("شهر " + month + ": " + amount));
    }

    // =========================================================================
    // === المنطق الحسابي (الكباري) ===
    // =========================================================================
    private void calculatePlan() {
        try {
            tableModel.setRowCount(0);
            
            double downPayment = Double.parseDouble(downPaymentField.getText());
            int months = Integer.parseInt(monthsField.getText());
            double profitRate = Double.parseDouble(profitRateField.getText()) / 100.0;

            // 1. حساب أصل الدين + الربح الكامل
            double remainingPrincipal = totalAmount - downPayment;
            double totalDebtWithProfit = remainingPrincipal * (1 + profitRate);

            // 2. حساب مجموع الكباري
            double totalBalloons = balloonsMap.values().stream().mapToDouble(Double::doubleValue).sum();

            // 3. التحقق من أن الكباري لا تتجاوز الدين
            if (totalBalloons >= totalDebtWithProfit) {
                JOptionPane.showMessageDialog(this, "خطأ: مجموع الكباري أكبر من إجمالي الدين!");
                return;
            }

            // 4. المبلغ المتبقي لتقسيطه بانتظام
            double amountForRegularInstallments = totalDebtWithProfit - totalBalloons;
            double monthlyRegularAmount = amountForRegularInstallments / months;

            LocalDate startDate = LocalDate.parse(dateField.getText(), formatter);
            LocalDate today = LocalDate.now();

            for (int i = 1; i <= months; i++) {
                // القسط = الجزء المنتظم + الكبري (إن وجد في هذا الشهر)
                double balloonForThisMonth = balloonsMap.getOrDefault(i, 0.0);
                double totalForMonth = monthlyRegularAmount + balloonForThisMonth;

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
                    totalForMonth,
                    status
                });
            }

            infoLabel.setText(String.format(Locale.ENGLISH, "الإجمالي النهائي: %.2f ج.م", totalDebtWithProfit));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "تأكد من صحة البيانات (خاصة التواريخ والأرقام)");
        }
    }

    private void handleConfirm() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "يجب توليد الجدول أولاً");
            return;
        }

        DatabaseManager.Customer selectedCustomer = (DatabaseManager.Customer) customerCombo.getSelectedItem();
        if (selectedCustomer == null) {
            JOptionPane.showMessageDialog(this, "يرجى اختيار عميل");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "هل تريد إعتماد هذه الأقساط؟", "تأكيد", JOptionPane.YES_NO_OPTION);
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
                    i + 1, dueDateStr, amount, actualPaymentDate, isPaid
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
                JOptionPane.showMessageDialog(this, "تم الحفظ بنجاح.");
                if (parentWindow != null) parentWindow.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "فشل الحفظ.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // --- UI Helpers ---
    private void loadCustomers() {
        List<DatabaseManager.Customer> customers = dbManager.getAllCustomersForPOS();
        for (DatabaseManager.Customer c : customers) customerCombo.addItem(c);
    }

    private JTextField createStyledTextField(String text, String tooltip) {
        JTextField field = new JTextField(text);
        field.setFont(getCairoFont(14f));
        field.setPreferredSize(new Dimension(0, 35));
        field.setToolTipText(tooltip);
        return field;
    }

    private JPanel createFieldWrapper(JComponent component, String labelText) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(getCairoFont(12f));
        p.add(lbl, BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    private void setupTableUI() {
        installmentsTable.setRowHeight(30);
        installmentsTable.setFont(getCairoFont(13f));
        installmentsTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        JTableHeader header = installmentsTable.getTableHeader();
        header.setFont(getCairoFont(13f));
        header.setBackground(new Color(245, 245, 245));
        ((DefaultTableCellRenderer)header.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        DefaultTableCellRenderer englishNumberRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Number) value = String.format(Locale.ENGLISH, "%.2f", ((Number) value).doubleValue());
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        };
        englishNumberRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                String status = (String) value;
                if ("خالص".equals(status)) setForeground(new Color(46, 204, 113));
                else if ("متأخر".equals(status)) setForeground(new Color(231, 76, 60));
                else setForeground(Color.BLACK);
                return this;
            }
        };

        for (int i = 0; i < installmentsTable.getColumnCount(); i++) {
            if (i == 3) installmentsTable.getColumnModel().getColumn(i).setCellRenderer(englishNumberRenderer);
            else if (i == 4) installmentsTable.getColumnModel().getColumn(i).setCellRenderer(statusRenderer);
            else installmentsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }
}
