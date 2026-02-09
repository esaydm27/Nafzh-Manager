package nafzh.manager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.math.BigDecimal;
import java.util.List;

public class PaymentDialog extends JDialog {

    // --- Components ---
    private JComboBox<Customer> customerComboBox;
    private JTextField customerBalanceField;
    private JTextField totalAmountField;
    private JTextField amountPaidField;
    private JTextField remainingAmountField;
    private JButton confirmButton;
    private JButton cancelButton;

    // --- State ---
    private boolean saleConfirmed = false;
    private final BigDecimal totalAmount;

    public PaymentDialog(Frame parent, BigDecimal totalAmount) {
        super(parent, "إتمام عملية البيع", true);
        this.totalAmount = totalAmount;

        initializeUI();
    }

    private void initializeUI() {
        setLayout(new GridBagLayout());
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Total Amount
        gbc.gridx = 0; gbc.gridy = 0; 
        add(new JLabel("إجمالي الفاتورة:"), gbc);
        
        totalAmountField = new JTextField(totalAmount.toPlainString());
        totalAmountField.setEditable(false);
        totalAmountField.setFont(new Font("Arial", Font.BOLD, 14));
        totalAmountField.setHorizontalAlignment(JTextField.CENTER);
        gbc.gridx = 1; add(totalAmountField, gbc);

        // 2. Customer Selection
        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("اختر العميل:"), gbc);
        customerComboBox = new JComboBox<>();
        ((JLabel)customerComboBox.getRenderer()).setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 1; add(customerComboBox, gbc);

        // 3. Customer Balance
        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("رصيد العميل السابق:"), gbc);
        customerBalanceField = new JTextField("0.00");
        customerBalanceField.setEditable(false);
        customerBalanceField.setForeground(Color.BLUE);
        customerBalanceField.setHorizontalAlignment(JTextField.CENTER);
        gbc.gridx = 1; add(customerBalanceField, gbc);

        // 4. Amount Paid
        gbc.gridx = 0; gbc.gridy = 3; add(new JLabel("المبلغ المدفوع:"), gbc);
        amountPaidField = new JTextField(totalAmount.toPlainString());
        amountPaidField.setHorizontalAlignment(JTextField.CENTER);
        gbc.gridx = 1; add(amountPaidField, gbc);

        // 5. Remaining Amount
        gbc.gridx = 0; gbc.gridy = 4; add(new JLabel("المبلغ المتبقي:"), gbc);
        remainingAmountField = new JTextField("0.00");
        remainingAmountField.setEditable(false);
        remainingAmountField.setForeground(Color.RED);
        remainingAmountField.setFont(new Font("Arial", Font.BOLD, 14));
        remainingAmountField.setHorizontalAlignment(JTextField.CENTER);
        gbc.gridx = 1; add(remainingAmountField, gbc);

        // 6. Buttons
        confirmButton = new JButton("تأكيد الدفع");
        cancelButton = new JButton("إلغاء");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; 
        add(buttonPanel, gbc);

        // --- Logic ---
        loadCustomers();
        updateRemainingAmount();

        customerComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateCustomerBalanceDisplay();
            }
        });

        amountPaidField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateRemainingAmount(); }
            public void removeUpdate(DocumentEvent e) { updateRemainingAmount(); }
            public void changedUpdate(DocumentEvent e) { }
        });

        confirmButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> onCancel());

        pack();
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void loadCustomers() {
        CustomerDAO customerDAO = new CustomerDAO();
        
        // Add Cash Customer
        customerComboBox.addItem(new Customer(0, "عميل نقدي", 0.0));
        
        // Add DB Customers
        List<Customer> customers = customerDAO.getAllCustomers();
        for (Customer c : customers) {
            customerComboBox.addItem(c);
        }
    }

    private void updateCustomerBalanceDisplay() {
        Customer selectedCustomer = (Customer) customerComboBox.getSelectedItem();
        if (selectedCustomer != null && selectedCustomer.getId() != 0) {
            // Get fresh balance from DB
            CustomerDAO dao = new CustomerDAO();
            BigDecimal balance = dao.getCustomerBalance(selectedCustomer.getId());
            customerBalanceField.setText(balance.toPlainString());
        } else {
            customerBalanceField.setText("0.00");
        }
    }

    private void updateRemainingAmount() {
        try {
            String text = amountPaidField.getText();
            if (text.trim().isEmpty()) text = "0";
            BigDecimal paid = new BigDecimal(text);
            BigDecimal remaining = totalAmount.subtract(paid);
            remainingAmountField.setText(remaining.toPlainString());
        } catch (NumberFormatException ex) {
            remainingAmountField.setText("خطأ");
        }
    }

    private void onConfirm() {
        try {
            new BigDecimal(amountPaidField.getText()); // Validate
            this.saleConfirmed = true;
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "الرجاء إدخال مبلغ صحيح", "خطأ", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        this.saleConfirmed = false;
        dispose();
    }

    public boolean isSaleConfirmed() { return saleConfirmed; }
    
    public Customer getSelectedCustomer() {
        return (Customer) customerComboBox.getSelectedItem();
    }
    
    public BigDecimal getAmountPaid() {
        try {
            return new BigDecimal(amountPaidField.getText());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
