package nafzh.manager;

import java.util.List;

public class SaleData {
    private int customerId;
    private double totalAmount;
    private double paidAmount;
    private List<SaleItem> items; // قائمة الأصناف في هذه الفاتورة

    // المُنشئ الفارغ (يمكن تعيين القيم لاحقاً باستخدام Setters)
    public SaleData() {
    }

    // Setters (لتعيين القيم)
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    // Getters (للحصول على القيم)
    public int getCustomerId() {
        return customerId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public List<SaleItem> getItems() {
        return items;
    }
}
