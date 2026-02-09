package nafzh.manager;

/**
 * SaleItem
 * يمثل صنفاً واحداً داخل عملية البيع.
 * يحتوي على بيانات المنتج (الاسم، الوحدة)، الكمية، والسعر.
 */
public class SaleItem {
    
    private int productId;
    private String productName;
    private String unit; // الحقل الجديد لتخزين الوحدة (قطعة، علبة، كرتونة...)
    private int quantity;
    private double unitPrice;

    /**
     * المُنشئ (Constructor) لإنشاء صنف جديد.
     * 
     * @param productId رقم المنتج التعريفي
     * @param productName اسم المنتج
     * @param unit وحدة القياس (هام جداً لظهورها في الفاتورة)
     * @param quantity الكمية المباعة
     * @param unitPrice سعر البيع للوحدة الواحدة
     */
    public SaleItem(int productId, String productName, String unit, int quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // --- دوال الحصول على القيم (Getters) ---

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getUnit() {
        return unit;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    /**
     * حساب السعر الإجمالي لهذا الصنف (الكمية × السعر).
     * @return إجمالي السعر للصنف.
     */
    public double getTotalPrice() {
        return quantity * unitPrice;
    }
}
