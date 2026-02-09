package nafzh.manager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
public class CategoryCellRenderer extends DefaultTableCellRenderer {
    private final int categoryColumnIndex;
    private final Border noBorder;
    private final JTable table;

    public CategoryCellRenderer(JTable table, int categoryColumnIndex) {
        this.table = table;
        this.categoryColumnIndex = categoryColumnIndex;
        setHorizontalAlignment(JLabel.CENTER);
        setVerticalAlignment(JLabel.CENTER);
        this.noBorder = new EmptyBorder(0, 0, 0, 0);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        boolean isFirstRowOfCategory = (row == 0)
                || !value.equals(table.getValueAt(row - 1, categoryColumnIndex));
        if (isFirstRowOfCategory) {
            this.setText(value == null ? "" : value.toString());
            this.setToolTipText(value == null ? null : value.toString());
        } else {
            this.setText("");
            this.setToolTipText(null);
        }
        this.setBorder(noBorder);
        return this;
    }
}
