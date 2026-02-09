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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static nafzh.manager.NafzhManager.getCairoFont;

public class PeriodicInstallmentsPanel extends JPanel {

    private final DatabaseManager dbManager;
    private final double totalAmount;
    private final Map<Integer, Integer> cartItems;
    private final Window parentWindow;

    private JTable installmentsTable;
    private DefaultTableModel tableModel;
    
    // حقول الإدخال
    private JTextField dateField;
    private JComboBox<DatabaseManager.Customer> customerCombo;
    private JTextField downPaymentField;
    private JTextField totalMonthsField; // مدة التقسيط الكلية
    private JComboBox<String> frequencyCombo; // القائمة الجديدة: كل كام شهر؟
    private JTextField profitRateField;
    
    private JLabel infoLabel;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // خريطة لربط النص (ربع سنوي) بالقيمة الرقمية (3)
    private final Map<String, Integer> frequencyMap = new LinkedHashMap<>();

    public PeriodicInstallmentsPanel(DatabaseManager dbManager, double totalAmount, Map<Integer, Integer> cartItems, Window parentWindow) {
        this.dbManager = dbManager;
        this.totalAmount = totalAmount;
        this.cartItems = cartItems;
        this.parentWindow = parentWindow;
        
        // تعبئة خيارات الدورية
        frequencyMap.put("كل شهرين (Bimonthly)", 2);
        frequencyMap.put("ربع سنوي - كل 3 شهور (Quarterly)", 3);
        frequencyMap.put("ثلث سنوي - كل 4 شهور", 4);
        frequencyMap.put("نصف سنوي - كل 6 شهور", 6);
        frequencyMap.put("سنوي - كل 12 شهر", 12);

        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(20, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- لوحة المدخلات ---
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setOpaque(false);
        inputPanel.setPreferredSize(new Dimension(320, 0));

        customerCombo = new JComboBox<>();
        customerCombo.setFont(getCairoFont(16f));
        loadCustomers();

        dateField = createStyledTextField(LocalDate.now().format(formatter), "تاريخ أول قسط");
        downPaymentField = createStyledTextField("0", "المبلغ المقدم");
        totalMonthsField = createStyledTextField("12", "مدة السداد الإجمالية (بالشهور)");
        
        // إعداد قائمة الدورية
        frequencyCombo = new JComboBox<>(frequencyMap.keySet().toArray(new String[0]));
        frequencyCombo.setFont(getCairoFont(14f));
        frequencyCombo.setBackground(Color.WHITE);
        frequencyCombo.setSelectedIndex(1); // افتراضياً ربع سنوي
        
        profitRateField = createStyledTextField("0", "نسبة الربح السنوية %");

        inputPanel.add(createFieldWrapper(customerCombo, "اختيار العميل"));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createFieldWrapper(dateField, "تاريخ أول قسط"));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createFieldWrapper(downPaymentField, " المقدمة (ج.م)"));
        inputPanel.add(Box.createVerticalStrut(10));
        
        // قسم المدة والدورية
        inputPanel.add(createFieldWrapper(totalMonthsField, "مدة السداد الكلية (شهور)"));
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createFieldWrapper(frequencyCombo, "نظام الدفع (دورية القسط)"));
        
        inputPanel.add(Box.createVerticalStrut(10));
        inputPanel.add(createFieldWrapper(profitRateField, "هامش الربح % (اختياري)"));

        JPanel actionButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        actionButtons.setOpaque(false);
        actionButtons.setMaximumSize(new Dimension(300, 50));

        JButton btnCalc = new JButton("توليد الخطة الدورية");
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

        // --- لوحة الجدول ---
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

    // =========================================================================
    // === منطق القسط الدوري ===
    // =========================================================================
    private void calculatePlan() {
        try {
            tableModel.setRowCount(0);
            
            double downPayment = Double.parseDouble(downPaymentField.getText());
            int totalMonths = Integer.parseInt(totalMonthsField.getText());
            
            // الحصول على قيمة التكرار (مثلاً 3 لربع سنوي)
            String selectedFreq = (String) frequencyCombo.getSelectedItem();
            int freqMonths = frequencyMap.get(selectedFreq);
            
            // التحقق من أن المدة تقبل القسمة (اختياري، لكن يفضل للتنبيه)
            if (totalMonths % freqMonths != 0) {
                JOptionPane.showMessageDialog(this, "تنبيه: مدة السداد لا تقبل القسمة بالتساوي على الدورية المختارة.\nسيتم حساب عدد الأقساط بناءً على القسمة الصحيحة.");
            }
            
            // عدد الأقساط الفعلي
            int numberOfInstallments = totalMonths / freqMonths;
            if (numberOfInstallments == 0) numberOfInstallments = 1;

            double profitRate = Double.parseDouble(profitRateField.getText()) / 100.0;

            double remainingPrincipal = totalAmount - downPayment;
            double totalWithProfit = remainingPrincipal * (1 + profitRate);
            double installmentAmount = totalWithProfit / numberOfInstallments;
            double finalTotalAmount = downPayment + totalWithProfit;

            LocalDate startDate = LocalDate.parse(dateField.getText(), formatter);
            LocalDate today = LocalDate.now();

            for (int i = 1; i <= numberOfInstallments; i++) {
                // حساب التاريخ: نزيد (i-1) * عدد شهور الدورية
                // مثال ربع سنوي: 0، 3، 6، 9
                LocalDate dueDate = startDate.plusMonths((long) (i - 1) * freqMonths);
                
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
                    installmentAmount,
                    status
                });
            }

            infoLabel.setText(String.format(Locale.ENGLISH, "السعر : %.2f ج.م ", finalTotalAmount));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "يرجى التأكد من صحة البيانات");
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
