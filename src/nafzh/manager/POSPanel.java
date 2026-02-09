package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nafzh.manager.DatabaseManager.ProductForPOS;
import static nafzh.manager.NafzhManager.getCairoFont;


public class POSPanel extends JPanel {

    private final DatabaseManager dbManager;
    private final NafzhManager parentFrame;

    private JTextField searchField;
    private JTable searchTable;
    private DefaultTableModel searchModel;

    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel grandTotalLabel;

    private double grandTotal = 0.0;
    private final Map<Integer, Integer> cartDetails; // {productId, quantity}

    private static final String[] SEARCH_COLUMN_NAMES = {"ID", "اسم الصنف", "السعر", "متبقي", "تحديد"};
    private static final String[] CART_COLUMN_NAMES = {"ID", "اسم الصنف", "الوحدة", "الكمية", "السعر للوحدة", "الإجمالي الفرعي"};

    public POSPanel(DatabaseManager dbManager, NafzhManager parentFrame) {
        this.dbManager = dbManager;
        this.parentFrame = parentFrame;
        this.cartDetails = new HashMap<>();
        initializePanel();
    }

    private void initializePanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setName("POS");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);

        // --- لوحة البحث ---
        JPanel searchPanel = new JPanel(new BorderLayout(10, 10));
        searchPanel.setBorder(BorderFactory.createTitledBorder(null, "بحث وإضافة صنف", javax.swing.border.TitledBorder.RIGHT, javax.swing.border.TitledBorder.DEFAULT_POSITION, getCairoFont(12f)));
        searchPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        searchField = new JTextField(20);
        searchField.setFont(getCairoFont(14f));
        searchField.setHorizontalAlignment(JTextField.RIGHT);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performSearch();
            }
        });

        searchModel = new DefaultTableModel(SEARCH_COLUMN_NAMES, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 4 ? Boolean.class : super.getColumnClass(col);
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 4;
            }
        };

        searchTable = new JTable(searchModel);
        customizeTable(searchTable);
        searchTable.getColumnModel().getColumn(1).setPreferredWidth(250);

        searchTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && searchTable.getSelectedRow() != -1) {
                    int row = searchTable.getSelectedRow();
                    if ((int) searchModel.getValueAt(row, 3) > 0) {
                        addItemToCart((int) searchModel.getValueAt(row, 0), 1);
                    } else {
                        JOptionPane.showMessageDialog(POSPanel.this, "هذا الصنف غير متوفر بالمخزون.", "نفد المخزون", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        });

        searchPanel.add(searchField, BorderLayout.NORTH);
        searchPanel.add(new JScrollPane(searchTable), BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0.4;
        add(searchPanel, gbc);

        // --- لوحة السلة ---
        JPanel cartPanel = new JPanel(new BorderLayout(10, 10));
        cartPanel.setBorder(BorderFactory.createTitledBorder(null, "سلة المبيعات", javax.swing.border.TitledBorder.RIGHT, javax.swing.border.TitledBorder.DEFAULT_POSITION, getCairoFont(12f)));
        cartPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        cartModel = new DefaultTableModel(CART_COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 3;
            }
        };

        cartTable = new JTable(cartModel);
        customizeTable(cartTable);

        cartModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && e.getColumn() == 3) {
                int row = e.getFirstRow();
                if (row >= 0) {
                    try {
                        int productId = (int) cartModel.getValueAt(row, 0);
                        int newQuantity = Integer.parseInt(cartModel.getValueAt(row, 3).toString());
                        updateCartItemQuantity(productId, newQuantity);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        });

        cartPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // --- اللوحة السفلية (الأزرار والإجمالي) ---
        JPanel southCartPanel = new JPanel(new BorderLayout());
        southCartPanel.setOpaque(false);

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        grandTotalLabel = new JLabel("الإجمالي الكلي: 0.00 ج.م.");
        grandTotalLabel.setFont(getCairoFont(16f));
        totalPanel.add(grandTotalLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton fetchSelectedButton = createStyledButton("جلب التحديد", new Color(25, 118, 210));
        fetchSelectedButton.addActionListener(e -> fetchSelectedToCart());

        JButton calculateQuantityButton = createStyledButton("حساب كمية", new Color(66, 165, 245));
        calculateQuantityButton.addActionListener(e -> showQuantityCalculationDialog());

        JButton removeButton = createStyledButton("إزالة التحديد", new Color(255, 152, 0));
        removeButton.addActionListener(e -> removeSelectedCartItem());

        JButton clearButton = createStyledButton("مسح العربة", new Color(244, 67, 54));
        clearButton.addActionListener(e -> clearCart());

        JButton checkoutButton = createStyledButton("إتمام البيع", new Color(76, 175, 80));
        checkoutButton.addActionListener(e -> handleCheckout());

        
        
        // الزر الجديد: بيع تقسيط
        JButton installmentButton = new JButton("بيع تقسيط");
        installmentButton.setFont(getCairoFont(11f));
        installmentButton.setBackground(new Color(155, 89, 182)); 
        installmentButton.setForeground(Color.BLACK);
        installmentButton.addActionListener(e -> openInstallmentDialog());
        
       
       
       
        
        buttonPanel.add(fetchSelectedButton);
        buttonPanel.add(calculateQuantityButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(checkoutButton);
        buttonPanel.add(installmentButton) ;
        buttonPanel.add(checkoutButton) ;
        
       
        
        southCartPanel.add(totalPanel, BorderLayout.EAST);
        southCartPanel.add(buttonPanel, BorderLayout.WEST);
        cartPanel.add(southCartPanel, BorderLayout.SOUTH);

        gbc.gridy = 1; gbc.weighty = 0.6;
        add(cartPanel, gbc);
    }
    
    // دالة مساعدة لإنشاء الأزرار بنفس الستايل لتقليل التكرار
    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK); // أو الأبيض حسب التباين
        btn.setFont(getCairoFont(10f));
        return btn;
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        searchModel.setRowCount(0);
        if (query.isEmpty()) return;

        dbManager.getProductsByName(query).forEach(p -> {
            int reserved = cartDetails.getOrDefault(p.id, 0);
            searchModel.addRow(new Object[]{p.id, p.name, p.salePrice, p.currentQuantity - reserved, false});
        });
    }

    private void addItemToCart(int productId, int quantityToAdd) {
        ProductForPOS p = dbManager.getProductByIdForPOS(productId);
        if (p == null) return;

        int currentInCart = cartDetails.getOrDefault(productId, 0);
        if (currentInCart + quantityToAdd > p.currentQuantity) {
            JOptionPane.showMessageDialog(this, "الكمية المطلوبة غير متوفرة.", "نفد المخزون", JOptionPane.WARNING_MESSAGE);
            return;
        }

        cartDetails.put(productId, currentInCart + quantityToAdd);
        updateCartDisplay();
        performSearch();
    }

    private void fetchSelectedToCart() {
        for (int i = searchModel.getRowCount() - 1; i >= 0; i--) {
            if (Boolean.TRUE.equals(searchModel.getValueAt(i, 4))) {
                if ((int) searchModel.getValueAt(i, 3) > 0) {
                    addItemToCart((int) searchModel.getValueAt(i, 0), 1);
                }
            }
        }
    }

    private void showQuantityCalculationDialog() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "الرجاء تحديد صنف من سلة المبيعات.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int productId = (int) cartModel.getValueAt(selectedRow, 0);

        JTextField cartonField = new JTextField("1", 5);
        JTextField dozenField = new JTextField("1", 5);
        JTextField itemsPerDozenField = new JTextField("12", 5);

        JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
        panel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        panel.add(new JLabel("عدد الكراتين:"));
        panel.add(cartonField);
        panel.add(new JLabel("عدد الدستة:"));
        panel.add(dozenField);
        panel.add(new JLabel("عدد الأصناف بالدستة:"));
        panel.add(itemsPerDozenField);

        if (JOptionPane.showConfirmDialog(this, panel, "حساب الكمية", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                int qty = Integer.parseInt(cartonField.getText()) * Integer.parseInt(dozenField.getText()) * Integer.parseInt(itemsPerDozenField.getText());
                if (qty > 0) updateCartItemQuantity(productId, qty);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "الرجاء إدخال أرقام صحيحة.", "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateCartItemQuantity(int productId, int newQuantity) {
        ProductForPOS p = dbManager.getProductByIdForPOS(productId);
        if (p == null) return;

        if (newQuantity <= 0) {
            cartDetails.remove(productId);
        } else if (newQuantity > p.currentQuantity) {
            JOptionPane.showMessageDialog(this, "أقصى كمية متاحة هي: " + p.currentQuantity, "خطأ", JOptionPane.ERROR_MESSAGE);
            cartDetails.put(productId, p.currentQuantity);
        } else {
            cartDetails.put(productId, newQuantity);
        }
        updateCartDisplay();
        performSearch();
    }

    private void removeSelectedCartItem() {
        if (cartTable.getSelectedRow() != -1) {
            cartDetails.remove((int) cartModel.getValueAt(cartTable.getSelectedRow(), 0));
            updateCartDisplay();
            performSearch();
        }
    }

    private void updateCartDisplay() {
        cartModel.setRowCount(0);
        grandTotal = 0.0;
        for (Map.Entry<Integer, Integer> entry : cartDetails.entrySet()) {
            ProductForPOS p = dbManager.getProductByIdForPOS(entry.getKey());
            if (p != null) {
                double subtotal = p.salePrice * entry.getValue();
                grandTotal += subtotal;
                cartModel.addRow(new Object[]{p.id, p.name, p.unit, entry.getValue(), p.salePrice, subtotal});
            }
        }
        grandTotalLabel.setText(String.format("الإجمالي الكلي: %.2f ج.م.", grandTotal));
    }

        // دالة مساعدة لاستخراج البيانات من جدول JTable بدقة (مع الوحدة)
    private List<SaleItem> getCartItems() {
        List<SaleItem> items = new ArrayList<>();
        // نعتمد على بيانات الجدول لأنها تحتوي على كل التفاصيل المعروضة (بما فيها الوحدة)
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            int productId = (int) cartModel.getValueAt(i, 0);
            String productName = (String) cartModel.getValueAt(i, 1);
            String unit = (String) cartModel.getValueAt(i, 2); // التقاط الوحدة
            int quantity = Integer.parseInt(cartModel.getValueAt(i, 3).toString());
            double price = Double.parseDouble(cartModel.getValueAt(i, 4).toString());
            
            // استخدام الكونستركتور الجديد الذي يقبل الوحدة
            items.add(new SaleItem(productId, productName, unit, quantity, price)); 
        }
        return items;
    }

    private void openInstallmentDialog() {
        if (cartDetails.isEmpty()) {
            JOptionPane.showMessageDialog(this, "السلة فارغة!", "تنبيه", JOptionPane.WARNING_MESSAGE) ;
            return ;
        }
        InstallmentSaleDialog dialog = new InstallmentSaleDialog(parentFrame, dbManager, grandTotal, cartDetails) ;
        dialog.setVisible(true) ;
    }

    private void handleCheckout() {
        System.out.println("--- [تتبع] بدء عملية إتمام البيع ---");

        if (cartDetails.isEmpty()) {
            JOptionPane.showMessageDialog(this, "سلة المبيعات فارغة.", "تنبيه", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1. فتح نافذة الدفع
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        // نمرر الـ grandTotal المحسوب مسبقاً في POSPanel
        PaymentDialog paymentDialog = new PaymentDialog(parent, new BigDecimal(grandTotal));
        paymentDialog.setVisible(true);

        // 2. التحقق من التأكيد
        if (paymentDialog.isSaleConfirmed()) {
            System.out.println("[تتبع] تم تأكيد الدفع.");

            // 3. جمع البيانات
            Customer selectedCustomer = paymentDialog.getSelectedCustomer();
            BigDecimal amountPaid = paymentDialog.getAmountPaid();
            
            // تحديد المعرف والاسم (مهم جداً للتعديل الأخير)
            int customerId = (selectedCustomer != null) ? selectedCustomer.getId() : 0;
            String customerName = (selectedCustomer != null) ? selectedCustomer.getName() : "عميل نقدي";

            // 4. تحويل السلة لقائمة أصناف (باستخدام الدالة المساعدة الصحيحة)
            List<SaleItem> saleItems = getCartItems();

            // 5. الحفظ عبر SalesDAO (بالتوقيع الجديد الكامل)
            SalesDAO salesDAO = new SalesDAO();
            boolean success = salesDAO.saveSaleTransaction(
                customerId, 
                customerName, // تمرير الاسم
                grandTotal, 
                amountPaid.doubleValue(), 
                saleItems
            );

            if (success) {
                JOptionPane.showMessageDialog(this, "تمت عملية البيع بنجاح!", "نجاح", JOptionPane.INFORMATION_MESSAGE);
                
                clearCart();
                parentFrame.updateDashboardData();
                parentFrame.updateInventoryDisplay();
                System.out.println("[تتبع] تم التحديث بنجاح.");
            } else {
                JOptionPane.showMessageDialog(this, "فشل حفظ الفاتورة!", "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            System.out.println("[تتبع] تم إلغاء العملية.");
        }
    }


    public void clearCart() {
        cartDetails.clear();
        updateCartDisplay();
        searchField.setText("");
        performSearch();
    }

    public void resetPanel() {
        clearCart();
    }

    private void customizeTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(getCairoFont(12f));
        table.setShowGrid(true);
        table.setGridColor(new Color(200, 200, 200));
        table.getTableHeader().setFont(getCairoFont(12f));
        table.getTableHeader().setBackground(new Color(220, 220, 220));
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
    }
}
