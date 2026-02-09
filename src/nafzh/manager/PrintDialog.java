package nafzh.manager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import static nafzh.manager.NafzhManager.getCairoFont;

public class PrintDialog extends JDialog {
    private final int saleId;
    private final DatabaseManager dbManager;
    private InvoicePanel invoicePanel;

    public PrintDialog(Frame owner, int saleId, DatabaseManager dbManager) {
        super(owner, "معاينة وطباعة الفاتورة رقم " + saleId, true);
        this.saleId = saleId;
        this.dbManager = dbManager;
        initializeDialog();
    }

    private void initializeDialog() {
        setSize(850, 950);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        invoicePanel = new InvoicePanel(saleId, dbManager);
        JScrollPane scrollPane = new JScrollPane(invoicePanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.SOUTH);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBackground(new Color(230, 230, 230));
        JButton printBtn = new JButton("🖨 طباعة الآن");
        printBtn.setFont(getCairoFont(16f));
        printBtn.setBackground(new Color(60, 179, 113));
        printBtn.setForeground(Color.WHITE);
        printBtn.setFocusPainted(false);
        printBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        printBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        printBtn.addActionListener(this::handlePrintAction);
        JButton closeBtn = new JButton("إغلاق");
        closeBtn.setFont(getCairoFont(16f));
        closeBtn.addActionListener(e -> dispose());
        panel.add(printBtn);
        panel.add(closeBtn);
        return panel;
    }

    private void handlePrintAction(ActionEvent e) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new ComponentPrintable(invoicePanel));
        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "تم إرسال الفاتورة للطباعة بنجاح.", "طباعة", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "حدث خطأ أثناء الطباعة: " + ex.getMessage(), "خطأ في الطباعة", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}
