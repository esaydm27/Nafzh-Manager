package nafzh.manager;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import static nafzh.manager.NafzhManager.getCairoFont;

public class TableActionRendererEditor extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {

    public static enum ActionType {
        EDIT_DELETE, VIEW_ONLY, NO_ACTION
    }

    private final JPanel panel;
    private final JTable table;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JButton viewButton;
    private int selectedRow;
    private final ActionType type;
    private final Component emptyComponent = new JLabel();
    private final Border emptyBorder = new EmptyBorder(0, 0, 0, 0);
    private final Consumer<Integer> onEditAction;
    private final Consumer<Integer> onDeleteAction;
    private final Consumer<Integer> onViewAction;

    public TableActionRendererEditor(JTable table, Consumer<Integer> onEditAction, Consumer<Integer> onDeleteAction, Consumer<Integer> onViewAction, ActionType type) {
        this.table = table;
        this.table.setGridColor(new Color(110, 110, 110));
        this.onEditAction = onEditAction;
        this.onDeleteAction = onDeleteAction;
        this.onViewAction = onViewAction;
        this.type = type;

        panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panel.setOpaque(true);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        // تعديل الألوان هنا: جعلنا اللون المرر هو لون الخط بدلاً من الخلفية
        editButton = createActionButton("تعديل", new Color(25, 118, 210), this);
        deleteButton = createActionButton("حذف", new Color(244, 67, 54), this);
        viewButton = createActionButton("عرض", new Color(76, 175, 80), this);

        if (type == ActionType.EDIT_DELETE) {
            panel.add(editButton);
            panel.add(deleteButton);
        } else if (type == ActionType.VIEW_ONLY) {
            panel.add(viewButton);
        }
    }

    private JButton createActionButton(String text, Color textColor, ActionListener listener) {
        JButton button = new JButton(text);
        
        // إعدادات جعل الزر يظهر كنص فقط
        button.setContentAreaFilled(false); // إخفاء خلفية الزر
        button.setBorderPainted(false);     // إخفاء إطار الزر
        button.setFocusPainted(false);      // إخفاء إطار التركيز
        button.setOpaque(false);            // شفافية كاملة
        
        button.setForeground(textColor);    // لون النص
        button.setFont(getCairoFont(11f).deriveFont(Font.BOLD)); // جعل الخط سميك قليلاً ليظهر كزر نصي
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(listener);
        
        // هوامش بسيطة للنص
        button.setBorder(new EmptyBorder(5, 8, 5, 8));
        
        return button;
    }

    // داخل كلاس TableActionRendererEditor.java

@Override
public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    Object idValue = table.getValueAt(row, 1);
    
    // شرط فحص الصفوف الفارغة (عناوين الفئات)
    if (idValue == null || idValue.toString().trim().isEmpty()) {
        JLabel emptyLabel = new JLabel("");
        emptyLabel.setOpaque(true);
        emptyLabel.setBackground(table.getBackground());
        // رسم حدود الشبكة باللون الموحد 170
        emptyLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(170, 170, 170)));
        return emptyLabel;
    }
    
    panel.setVisible(true);
    updatePanelBackground(isSelected);
    // إضافة حدود للوحة الأزرار لتوحيد الشبكة باللون 170
    panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(170, 170, 170)));
    return panel;
}

@Override
public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    this.selectedRow = row;
    Object idValue = table.getValueAt(row, 1);
    
    if (idValue == null || idValue.toString().isEmpty()) {
        return new JLabel(""); 
    }
    
    panel.setVisible(true);
    updatePanelBackground(true);
    return panel;
}

    private void updatePanelBackground(boolean isSelected) {
        if (isSelected) {
            panel.setBackground(table.getSelectionBackground());
        } else {
            panel.setBackground(table.getBackground());
        }
    }

    @Override
    public Object getCellEditorValue() {
        return "";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fireEditingStopped();
        if (type == ActionType.EDIT_DELETE) {
            if (e.getSource() == editButton) {
                if (onEditAction != null) {
                    onEditAction.accept(selectedRow);
                }
            } else if (e.getSource() == deleteButton) {
                if (onDeleteAction != null) {
                    onDeleteAction.accept(selectedRow);
                }
            }
        } else if (type == ActionType.VIEW_ONLY) {
            if (e.getSource() == viewButton) {
                if (onViewAction != null) {
                    onViewAction.accept(selectedRow);
                }
            }
        }
    }
}