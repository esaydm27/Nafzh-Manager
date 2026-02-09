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
    
    // تم تحديث عدد الأعمدة ليتضمن عمود "الوحدة"
    private static final int COLUMNS_COUNT = 8;
    
    // قائمة أسماء الأعمدة بالترتيب الجديد
    private static final String[] COLUMN_NAMES = {
        "الفئة", "ID", "اسم الصنف", "الوحدة", "الكمية الحالية", "سعر الشراء", "سعر البيع", "الإجراءات"
    };

    public ExpandableInventoryTableModel(List<Map<String, Object>> productsData) {
        this.originalProductsData = productsData;
        this.categoryExpandState = new HashMap<>();
        initializeExpandState();
        buildDisplayedRows();
    }
    
    // تقوم هذه الدالة بإنشاء الصفوف المعروضة بناءً على حالة الطي/التوسيع
    private void buildDisplayedRows() {
        displayedRows = new ArrayList<>();
        Map<String, List<Map<String, Object>>> categorizedData = categorizeData(originalProductsData);

        for (Map.Entry<String, List<Map<String, Object>>> entry : categorizedData.entrySet()) {
            String category = entry.getKey();
            List<Map<String, Object>> products = entry.getValue();

            // إضافة صف الفئة الرئيسية
            Map<String, Object> categoryRow = createCategoryRow(category, products.size());
            displayedRows.add(categoryRow);

            // إضافة الأصناف إذا كانت الفئة موسعة (غير مطوية)
            if (categoryExpandState.getOrDefault(category, true)) { // تم تغيير القيمة الافتراضية إلى true
                displayedRows.addAll(products);
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

    private Map<String, Object> createCategoryRow(String category, int count) {
        Map<String, Object> row = new HashMap<>();
        row.put("isCategory", true);
        row.put("category", category);
        row.put("count", count);
        row.put("expanded", categoryExpandState.getOrDefault(category, true));
        return row;
    }

    private void initializeExpandState() {
        for (Map<String, Object> product : originalProductsData) {
            String category = (String) product.getOrDefault("category", "أخرى");
            categoryExpandState.putIfAbsent(category, true); // افتراض التوسع في البداية
        }
    }

    public void toggleExpandState(String category) {
        boolean currentState = categoryExpandState.getOrDefault(category, false);
        categoryExpandState.put(category, !currentState);
        buildDisplayedRows(); // إعادة بناء الصفوف بعد التغيير
    }

    // **********************************************
    // ** الدالة المضافة لحل خطأ "cannot find symbol" **
    // **********************************************
    public boolean isExpanded(String category) {
        return categoryExpandState.getOrDefault(category, false);
    }
    // **********************************************

    public boolean isCategoryRow(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < displayedRows.size()) {
            return (boolean) displayedRows.get(rowIndex).getOrDefault("isCategory", false);
        }
        return false;
    }
    
    // دالة مساعدة لتمكين المُصيِّر من معرفة عدد الأصناف في الفئة (إذا لزم الأمر)
    public int getCategoryCount(String category) {
         return originalProductsData.stream()
            .filter(p -> category.equals(p.getOrDefault("category", "أخرى")))
            .toArray().length;
    }

    public Map<String, Object> getProductDataAt(int rowIndex) {
        if (!isCategoryRow(rowIndex)) {
            return displayedRows.get(rowIndex);
        }
        return null;
    }

    public Integer getProductIdAt(int rowIndex) {
        Map<String, Object> product = getProductDataAt(rowIndex);
        if (product != null) {
            return (Integer) product.get("id");
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return displayedRows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        if (column >= 0 && column < COLUMNS_COUNT) {
            return COLUMN_NAMES[column];
        }
        return "";
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Map<String, Object> rowData = displayedRows.get(rowIndex);

        if (isCategoryRow(rowIndex)) {
            if (columnIndex == 0) {
                // تم تعديل هذا الجزء: العودة باسم الفئة فقط، ليتمكن الـ Cell Renderer من إضافة الأيقونة وحالة التوسيع/الطي.
                return rowData.get("category"); 
            }
            return null; // لا يتم عرض أي بيانات في باقي الأعمدة لصفوف الفئات
        }

        // هنا يتم التعامل مع صفوف المنتجات (التي ليست فئات)
        switch (columnIndex) {
            case 0: // الفئة
                return rowData.get("category");
            case 1: // ID
                return rowData.get("id");
            case 2: // اسم الصنف
                return rowData.get("name");
            case 3: // الوحدة - عمود جديد
                return rowData.getOrDefault("unit", "---"); 
            case 4: // الكمية الحالية
                return rowData.get("current_quantity");
            case 5: // سعر الشراء
                return rowData.get("purchase_price");
            case 6: // سعر البيع
                return rowData.get("sale_price");
            case 7: // الإجراءات (سيعرضها الـ Renderer)
                return "Actions"; 
            default:
                return null;
        }
    }
}