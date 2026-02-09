package nafzh.manager;

import static nafzh.manager.NafzhManager.getCairoFont;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Vector;
import javax.swing.table.JTableHeader;

public class DashboardPanel extends JPanel {
    private final DatabaseManager dbManager;
    private JPanel lowStockCard;
    private JPanel totalProductsCard;
    private JPanel salesCard;
    private JLabel lowStockValueLabel;
    private JLabel totalProductsValueLabel;
    private JLabel salesValueLabel;
    private JTable lowStockTable;
    private DefaultTableModel lowStockTableModel;
    private static final String[] LOW_STOCK_COLUMN_NAMES = {"ID", "اسم الصنف", "الفئة", "الكمية الحالية"};

    public DashboardPanel(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        initializePanel();
        loadDashboardData();
    }

    private void initializePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        setName("Dashboard");
        
        JPanel statCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT , 1, 0));
        statCardsPanel.setOpaque(false);
        lowStockCard = createStatCard("النواقص", "\uf071", new Color(244, 67, 54));
        totalProductsCard = createStatCard("إجمالي الأصناف", "\uf466", new Color(66, 165, 245)); // Corrected Title
        salesCard = createStatCard(String.format("مبيعات اليوم(%s)", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))), "\uf155", new Color(76, 175, 80));
        statCardsPanel.add(lowStockCard); // Added low stock card
        statCardsPanel.add(totalProductsCard); // Added total products card
        statCardsPanel.add(salesCard);
        add(statCardsPanel, BorderLayout.NORTH);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        JLabel lowStockTitle = new JLabel("الأصناف ذات المخزون المنخفض (أقل من 2)", SwingConstants.RIGHT);
        lowStockTitle.setFont(getCairoFont(15f));
        lowStockTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        lowStockTableModel = new DefaultTableModel(LOW_STOCK_COLUMN_NAMES, 0);
        lowStockTable = new JTable(lowStockTableModel);
        customizeTable(lowStockTable);
        JScrollPane scrollPane = new JScrollPane(lowStockTable);
        //scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        centerPanel.add(lowStockTitle, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    public void loadDashboardData() {
        int lowStockCount = dbManager.getLowStockCount();
        if (lowStockValueLabel != null) {
            lowStockValueLabel.setText(String.valueOf(lowStockCount));
        }
        int totalProducts = dbManager.getProductCount();
        if (totalProductsValueLabel != null) {
            totalProductsValueLabel.setText(String.valueOf(totalProducts));
        }
        String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        double totalSalesToday = dbManager.getTotalSalesByDate(todayDate);
        if (salesValueLabel != null) {
            salesValueLabel.setText(String.format("%.2f ج", totalSalesToday));
        }
        updateLowStockTable();
    }

    private void updateLowStockTable() {
        lowStockTableModel.setRowCount(0);
        List<java.util.Map<String, Object>> allProducts = dbManager.getAllProducts();
        for (java.util.Map<String, Object> product : allProducts) {
            int quantity = (int) product.get("current_quantity");
            if (quantity <= 1 && quantity > 0) {
                Vector<Object> row = new Vector<>();
                row.add(product.get("id"));
                row.add(product.get("name"));
                row.add(product.get("category"));
                row.add(product.get("current_quantity"));
                lowStockTableModel.addRow(row);
            }
        }
        lowStockTable.revalidate();
        lowStockTable.repaint();
    }

    private JPanel createStatCard(String title, String iconChar, Color iconBackground) {
        JPanel card = new JPanel(new BorderLayout(0, 430));
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(230, 50));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100)),
                BorderFactory.createEmptyBorder(1, 10, 1, 10)
        ));
        card.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JPanel textPanel = new JPanel(new GridLayout(1, 1));
        textPanel.setOpaque(false);
        textPanel.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        JLabel titleLabel = new JLabel(title, SwingConstants.RIGHT);
        titleLabel.setFont(getCairoFont(10f));
        titleLabel.setForeground(Color.BLACK);
        JLabel valueLabel = new JLabel("0", SwingConstants.RIGHT);
        valueLabel.setFont(getCairoFont(10f));
        textPanel.add(titleLabel);
        textPanel.add(valueLabel);
        card.add(textPanel, BorderLayout.EAST);
        if (title.contains("النواقص")) {
            this.lowStockValueLabel = valueLabel;
        } else if (title.contains("الأصناف")) { // Corrected condition
            this.totalProductsValueLabel = valueLabel;
        } else if (title.contains("مبيعات")) {
            this.salesValueLabel = valueLabel;
        }
        JLabel iconLabel = new JLabel(iconChar, SwingConstants.CENTER);
        iconLabel.setFont(getCairoFont(24f));
        iconLabel.setForeground(Color.WHITE);
        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setPreferredSize(new Dimension(50, 50));
        iconPanel.setBackground(iconBackground);
        iconPanel.add(iconLabel);
        card.add(iconPanel, BorderLayout.WEST);
        return card;
    }

    private void customizeTable(JTable table) {
        table.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        table.setRowHeight(35);
        table.setFont(getCairoFont(12f));
        table.setForeground(Color.BLACK);
        table.setBackground(new Color(200, 220, 255));
        table.setGridColor(new Color(110, 110, 110));
        table.setShowGrid(true);
        table.setSelectionBackground(new Color(0, 0, 0));
        table.setSelectionForeground(Color.WHITE);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        cellRenderer.setVerticalAlignment(SwingConstants.CENTER);
        cellRenderer.setForeground(Color.BLACK);
        cellRenderer.setBackground(new Color(200, 220, 255));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        JTableHeader header = table.getTableHeader();
        header.setFont(getCairoFont(12f));
        header.setBackground(new Color(200, 220, 255));
        header.setForeground(Color.BLACK);
        DefaultTableCellRenderer headerRenderer
                = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    }
}
