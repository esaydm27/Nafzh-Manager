package nafzh.manager;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpandableInventoryTableModel extends AbstractTableModel {

    private final List<Map<String, Object>> originalProductsData;
    private List<Map<String, Object>> displayedRows;
    private final Map<String, Boolean> categoryExpandState;
    
    private static final String[] COLUMN_NAMES = {
        "الفئة", "ID", "اسم الصنف", "الوحدة", "الكمية الحالية", "سعر الشراء", "سعر البيع", "الإجراءات"
    };

    public ExpandableInventoryTableModel(List<Map<String, Object>> productsData) {
        this.originalProductsData = productsData;
        this.categoryExpandState = new HashMap<>();
        initializeExpandState();
        buildDisplayedRows();
    }
    
    private void initializeExpandState() {
        for (Map<String, Object> product : originalProductsData) {
            String category = (String) product.getOrDefault("category", "أخرى");
            // الفئات مفتوحة افتراضياً
            categoryExpandState.putIfAbsent(category, true);
        }
    }

    public void buildDisplayedRows() {
        displayedRows = new ArrayList<>();
        Map<String, List<Map<String, Object>>> categorizedData = categorizeData(originalProductsData);

        // ترتيب الفئات أبجدياً
        List<String> sortedCategories = new ArrayList<>(categorizedData.keySet());
        sortedCategories.sort(String::compareTo);

        for (String category : sortedCategories) {
            List<Map<String, Object>> products = categorizedData.get(category);
            if (products == null || products.isEmpty()) continue;

            boolean isExpanded = categoryExpandState.getOrDefault(category, true);

            // 1. دائماً نضيف المنتج الأول (ليدمج مع الفئة)
            displayedRows.add(products.get(0));

            // 2. إذا كانت الفئة مفتوحة (Expanded)، نضيف باقي المنتجات
            if (isExpanded) {
                for (int i = 1; i < products.size(); i++) {
                    displayedRows.add(products.get(i));
                }
            }
        }
        fireTableDataChanged();
    }
    
    private Map<String, List<Map<String, Object>>> categorizeData(List<Map<String, Object>> data) {
        Map<String, List<Map<String, Object>>> map = new HashMap<>();
        for (Map<String, Object> product : data) {
            String category = (String) product.getOrDefault("category", "أخرى");
            map.computeIfAbsent(category, k -> new ArrayList<>()).add(product);
        }
        return map;
    }

    public void toggleExpandState(String category) {
        boolean currentState = categoryExpandState.getOrDefault(category, true);
        categoryExpandState.put(category, !currentState);
        buildDisplayedRows(); // إعادة بناء الجدول لإظهار/إخفاء الصفوف
    }

    public boolean isExpanded(String category) {
        return categoryExpandState.getOrDefault(category, true);
    }
    
    // هل يوجد منتجات أخرى في هذه الفئة؟ (لإظهار زر + أو -)
    public boolean hasMoreProducts(String category) {
        long count = originalProductsData.stream()
                .filter(p -> category.equals(p.getOrDefault("category", "أخرى")))
                .count();
        return count > 1;
    }

    public Map<String, Object> getProductDataAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < displayedRows.size()) {
            return displayedRows.get(rowIndex);
        }
        return null;
    }

    public Integer getProductIdAt(int rowIndex) {
        Map<String, Object> product = getProductDataAt(rowIndex);
        return product != null ? (Integer) product.get("id") : null;
    }

    @Override
    public int getRowCount() {
        return displayedRows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Map<String, Object> rowData = displayedRows.get(rowIndex);
        if (rowData == null) return null;

        switch (columnIndex) {
            case 0: return rowData.get("category");
            case 1: return rowData.get("id");
            case 2: return rowData.get("name");
            case 3: return rowData.getOrDefault("unit", "---");
            case 4: return rowData.get("current_quantity");
            case 5: return rowData.get("purchase_price");
            case 6: return rowData.get("sale_price");
            case 7: return "Actions";
            default: return null;
        }
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 7;
    }
}
