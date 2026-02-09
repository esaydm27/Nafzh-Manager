package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import static nafzh.manager.NafzhManager.getCairoFont;

public class InstallmentsPanel extends JPanel {

    private final DatabaseManager dbManager;
    private final NafzhManager parentFrame;
    private JTable installmentsTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> filterStatusCombo;
    private JLabel summaryLabel;
    private List<DatabaseManager.Installment> allInstallments;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] COLUMN_NAMES = {
        "رقم", "إسم العميل", "مبلغ القسط", "تاريخ الاستحقاق", "تاريخ السداد", "الحالة"
    };

    public InstallmentsPanel(DatabaseManager dbManager, NafzhManager parentFrame) {
        this.dbManager = dbManager;
        this.parentFrame = parentFrame;
        initializePanel();
        loadInstallmentData();
    }

    private void initializePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(Color.WHITE);

        // --- الجزء العلوي ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("بحث باسم العميل:");
        searchLabel.setFont(getCairoFont(14f));
        
        searchField = new JTextField(20);
        searchField.setFont(getCairoFont(14f));
        searchField.setHorizontalAlignment(JTextField.RIGHT);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        String[] filters = {
            "الكل", "مستحق", "آتي", "----------------", "خالص", "متأخر", 
            "مرتقب", "مقترب", "قادم", "مؤقت", "مرجأ", "مجدول", "منتظر"
        };
        filterStatusCombo = new JComboBox<>(filters);
        filterStatusCombo.setFont(getCairoFont(12f));
        filterStatusCombo.addActionListener(e -> applyFilters());

        searchPanel.add(filterStatusCombo);
        searchPanel.add(new JLabel("  الحالة: "));
        searchPanel.add(searchField);
        searchPanel.add(searchLabel);

        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- الجدول ---
        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };

        installmentsTable = new JTable(tableModel);
        installmentsTable.setRowHeight(35);
        installmentsTable.setFont(getCairoFont(12f));
        installmentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        installmentsTable.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        installmentsTable.setShowGrid(true); // إظهار الشبكة
        installmentsTable.setGridColor(new Color(230, 230, 230));

        setColumnWidths();
        customizeTableHeader(); // تم إصلاحها بالأسفل
        customizeTableCells();

        JScrollPane scrollPane = new JScrollPane(installmentsTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- الجزء السفلي ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        
        summaryLabel = new JLabel("إجمالي الأقساط المتأخرة: 0.00 ج.م");
        summaryLabel.setFont(getCairoFont(16f).deriveFont(Font.BOLD));
        summaryLabel.setForeground(new Color(192, 57, 43));
        
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonsPanel.setOpaque(false);

        JButton refreshBtn = new JButton("تحديث الجدول");
        refreshBtn.setFont(getCairoFont(12f));
        refreshBtn.addActionListener(e -> refreshData());

        JButton updateStatusBtn = new JButton("تعديل حالة القسط المحدد");
        updateStatusBtn.setFont(getCairoFont(12f));
        updateStatusBtn.setBackground(new Color(52, 152, 219));
        updateStatusBtn.setForeground(Color.BLACK);
        updateStatusBtn.addActionListener(e -> openUpdateStatusDialog());

        buttonsPanel.add(refreshBtn);
        buttonsPanel.add(updateStatusBtn);

        bottomPanel.add(summaryLabel, BorderLayout.EAST);
        bottomPanel.add(buttonsPanel, BorderLayout.WEST);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // --- إصلاح مشكلة اختفاء الرؤوس (Header Fix) ---
    private void customizeTableHeader() {
        JTableHeader header = installmentsTable.getTableHeader();
        header.setReorderingAllowed(false); // منع إعادة ترتيب الأعمدة
        
        // إنشاء Renderer مخصص لضمان الرسم الصحيح
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                setBackground(new Color(52, 73, 94)); // لون الخلفية الأزرق الداكن
                setForeground(Color.WHITE);           // لون النص الأبيض
                setFont(getCairoFont(13f).deriveFont(Font.BOLD));
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(200, 200, 200))); // حدود خفيفة
                
                return this;
            }
        };
        
        header.setDefaultRenderer(headerRenderer);
    }
    // ----------------------------------------------

    private String determineStatus(DatabaseManager.Installment inst) {
        if (inst.isPaid) return "خالص";
        try {
            LocalDate dueDate = LocalDate.parse(inst.dueDate, formatter);
            LocalDate today = LocalDate.now();

            if (dueDate.isBefore(today)) {
                return "متأخر";
            } else {
                long monthsDiff = ChronoUnit.MONTHS.between(today, dueDate);
                if (monthsDiff < 1) return "مرتقب";
                if (monthsDiff < 2) return "مقترب";
                if (monthsDiff < 3) return "قادم";
                if (monthsDiff < 4) return "مؤقت";
                if (monthsDiff < 5) return "مرجأ";
                if (monthsDiff < 6) return "مجدول";
                return "منتظر";
            }
        } catch (Exception e) {
            return "مستحق";
        }
    }

    private void applyFilters() {
        tableModel.setRowCount(0);
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedFilter = (String) filterStatusCombo.getSelectedItem();
        
        if (selectedFilter != null && selectedFilter.contains("---")) return;

        List<DatabaseManager.Installment> filteredList = allInstallments.stream()
            .filter(inst -> {
                boolean matchesSearch = inst.customerName.toLowerCase().contains(searchText);
                String status = determineStatus(inst);
                
                boolean matchesStatus = true;
                if ("الكل".equals(selectedFilter)) {
                    matchesStatus = true;
                } else if ("مستحق".equals(selectedFilter)) {
                    matchesStatus = !inst.isPaid;
                } else if ("آتي".equals(selectedFilter)) {
                    matchesStatus = !inst.isPaid && !status.equals("متأخر");
                } else {
                    matchesStatus = status.equals(selectedFilter);
                }
                
                return matchesSearch && matchesStatus;
            })
            .collect(Collectors.toList());

        for (DatabaseManager.Installment inst : filteredList) {
            Vector<Object> row = new Vector<>();
            row.add(inst.id);
            row.add(inst.customerName);
            row.add(String.format("%.2f", inst.amount));
            row.add(inst.dueDate);
            row.add(inst.paymentDate);
            row.add(determineStatus(inst)); 
            tableModel.addRow(row);
        }
    }

    private void customizeTableCells() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                String status = (String) value;
                
                switch (status) {
                    case "خالص":
                        setForeground(new Color(39, 174, 96)); 
                        setFont(getCairoFont(12f).deriveFont(Font.BOLD));
                        break;
                    case "متأخر":
                        setForeground(new Color(192, 57, 43)); 
                        setFont(getCairoFont(12f).deriveFont(Font.BOLD));
                        break;
                    case "مرتقب": setForeground(new Color(230, 126, 34)); break;
                    case "مقترب": setForeground(new Color(243, 156, 18)); break;
                    case "قادم": setForeground(new Color(142, 68, 173)); break;
                    case "مؤقت": setForeground(new Color(41, 128, 185)); break;
                    case "مرجأ": setForeground(new Color(52, 152, 219)); break;
                    case "مجدول": setForeground(new Color(22, 160, 133)); break;
                    case "منتظر": setForeground(new Color(127, 140, 141)); break;
                    default: setForeground(Color.BLACK);
                }
                
                if (isSelected) setForeground(Color.WHITE);
                return this;
            }
        };

        for (int i = 0; i < installmentsTable.getColumnCount(); i++) {
            if (i == 5) { 
                installmentsTable.getColumnModel().getColumn(i).setCellRenderer(statusRenderer);
            } else {
                installmentsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }
    }

    private void openUpdateStatusDialog() {
        int selectedRow = installmentsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "يرجى تحديد قسط من الجدول أولاً");
            return;
        }
        int installmentId = (int) tableModel.getValueAt(selectedRow, 0);
        DatabaseManager.Installment currentInst = allInstallments.stream().filter(i -> i.id == installmentId).findFirst().orElse(null);
        if (currentInst == null) return;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "تحديث حالة القسط", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("تاريخ السداد:"), gbc);
        gbc.gridx = 1;
        String defaultDate = (currentInst.isPaid && !currentInst.paymentDate.equals("-")) ? currentInst.paymentDate : LocalDate.now().format(formatter);
        JTextField paymentDateField = new JTextField(defaultDate);
        dialog.add(paymentDateField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        dialog.add(new JLabel("الحالة:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"مستحق", "خالص"});
        statusCombo.setSelectedIndex(currentInst.isPaid ? 1 : 0);
        dialog.add(statusCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        JButton saveBtn = new JButton("حفظ التعديلات");
        saveBtn.setBackground(new Color(46, 204, 113));
        saveBtn.setForeground(Color.BLACK);
        saveBtn.addActionListener(e -> {
            boolean newIsPaid = statusCombo.getSelectedIndex() == 1;
            String newDate = paymentDateField.getText();
            if (!newIsPaid) newDate = "-";
            if (dbManager.updateInstallmentStatus(installmentId, newDate, newIsPaid)) {
                dialog.dispose();
                refreshData();
                JOptionPane.showMessageDialog(this, "تم تحديث الحالة بنجاح");
            } else {
                JOptionPane.showMessageDialog(this, "حدث خطأ");
            }
        });
        dialog.add(saveBtn, gbc);
        dialog.setVisible(true);
    }

    private void setColumnWidths() {
        TableColumnModel columnModel = installmentsTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setPreferredWidth(200);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(120);
        columnModel.getColumn(4).setPreferredWidth(120);
        columnModel.getColumn(5).setPreferredWidth(100);
    }

    private void loadInstallmentData() {
        allInstallments = dbManager.getAllInstallments(); 
        applyFilters(); 
        updateSummary();
        SwingUtilities.invokeLater(this::checkForDueInstallments);
    }

    private void checkForDueInstallments() {
        LocalDate today = LocalDate.now();
        int dueCount = 0;
        for (DatabaseManager.Installment inst : allInstallments) {
            if (!inst.isPaid) {
                try {
                    LocalDate dueDate = LocalDate.parse(inst.dueDate, formatter);
                    if (!dueDate.isAfter(today)) dueCount++;
                } catch (Exception e) {}
            }
        }
        if (dueCount > 0) {
            Object[] options = {"عرض الأقساط المتأخرة", "إلغاء"};
            int choice = JOptionPane.showOptionDialog(this,
                "يوجد عدد (" + dueCount + ") أقساط مستحقة اليوم أو متأخرة.\nهل تريد تصفيتها الآن؟",
                "تنبيه", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            
            if (choice == JOptionPane.YES_OPTION) {
                filterStatusCombo.setSelectedItem("متأخر");
                if (parentFrame != null) {
                    parentFrame.showPanel("Installments");
                }
            }
        }
    }

    private void updateSummary() {
        double totalUnpaid = dbManager.getTotalUnpaidInstallments();
        summaryLabel.setText(String.format("إجمالي الأقساط المتأخرة: %.2f ج.م", totalUnpaid));
    }
    
    public void refreshData() {
        allInstallments = dbManager.getAllInstallments(); 
        applyFilters(); 
        updateSummary();
    }
}
