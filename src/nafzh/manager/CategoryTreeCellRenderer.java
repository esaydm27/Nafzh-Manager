package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import javax.swing.border.Border;
public class CategoryTreeCellRenderer extends DefaultTableCellRenderer {
    private final ExpandableInventoryTableModel model;
    private final Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);

    public CategoryTreeCellRenderer(ExpandableInventoryTableModel model) {
        this.model = model;
        setHorizontalAlignment(JLabel.RIGHT);
        setVerticalAlignment(JLabel.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (model.isCategoryRow(row)) {
            String category = (String) value;
            boolean isExpanded = model.isExpanded(category);
            String icon = isExpanded ? "▼ " : "◀ ";
            this.setText(icon + category);
            this.setFont(getFont().deriveFont(Font.BOLD));
            this.setBackground(new Color(240, 240, 240));
            this.setForeground(Color.BLACK);
            this.setBorder(emptyBorder);
        } else {
            this.setText("   " + (value != null ? value.toString() : ""));
            this.setFont(getFont().deriveFont(Font.PLAIN));
            if (isSelected) {
                this.setBackground(table.getSelectionBackground());
                this.setForeground(table.getSelectionForeground());
            } else {
                this.setBackground(table.getBackground());
                this.setForeground(table.getForeground());
            }
            this.setBorder(emptyBorder);
        }
        return this;
    }
}
