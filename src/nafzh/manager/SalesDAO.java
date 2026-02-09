package nafzh.manager;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * SalesDAO
 * فئة مسؤولة عن إدارة العمليات الخاصة بالمبيعات في قاعدة البيانات (SQLite).
 * تعالج حفظ الفواتير، الأصناف، وتحديث أرصدة العملاء والمخزون.
 */
public class SalesDAO {

    /**
     * حفظ عملية بيع جديدة بالكامل ضمن معاملة واحدة (Transaction).
     * 
     * @param customerId معرّف العميل (0 أو -1 للعميل النقدي).
     * @param customerName اسم العميل (لحفظه في الفاتورة للرجوع إليه حتى لو حُذف العميل).
     * @param totalAmount إجمالي قيمة الفاتورة.
     * @param paidAmount المبلغ المدفوع.
     * @param items قائمة الأصناف المباعة.
     * @return true إذا تمت العملية بنجاح، false إذا فشلت.
     */
    public boolean saveSaleTransaction(int customerId, String customerName, double totalAmount, double paidAmount, List<SaleItem> items) {
        Connection conn = null;
        
        // 1. استعلام إدراج الفاتورة (يشمل اسم العميل والتاريخ النصي لـ SQLite)
        String insertSaleSQL = "INSERT INTO Sales (customer_id, customer_name, total_amount, amount_paid, sale_date) VALUES (?, ?, ?, ?, ?)";
        
        // 2. استعلام إدراج الأصناف (يشمل اسم الوحدة product_unit)
        String insertSaleItemSQL = "INSERT INTO sale_items (sale_id, product_id, product_name, product_unit, quantity, sale_price, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        // 3. استعلام تحديث رصيد العميل (الرصيد - المتبقي)
        // إذا كان عليه دين (المتبقي موجب)، الرصيد ينقص.
        // إذا دفع زيادة (المتبقي سالب)، الرصيد يزيد.
        String updateCustomerBalanceSQL = "UPDATE customers SET balance = balance + ? WHERE id = ?";
        
        // 4. استعلام تحديث المخزون
        String updateProductStockSQL = "UPDATE products SET current_quantity = current_quantity - ? WHERE id = ?";

        // تاريخ اليوم بتنسيق YYYY-MM-DD
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // بدء المعاملة الذرية

            // --- أ. تنفيذ إدراج الفاتورة ---
            long saleId = 0;
            try (PreparedStatement psSale = conn.prepareStatement(insertSaleSQL, Statement.RETURN_GENERATED_KEYS)) {
                if (customerId <= 0) {
                    psSale.setNull(1, java.sql.Types.INTEGER);
                    // إذا لم يمرر اسم، نستخدم "عميل نقدي" كاحتياط
                    psSale.setString(2, (customerName != null && !customerName.isEmpty()) ? customerName : "عميل نقدي");
                } else {
                    psSale.setInt(1, customerId);
                    psSale.setString(2, customerName);
                }
                psSale.setDouble(3, totalAmount);
                psSale.setDouble(4, paidAmount);
                psSale.setString(5, currentDate);
                
                int affectedRows = psSale.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating sale failed, no rows affected.");
                }

                try (ResultSet generatedKeys = psSale.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        saleId = generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Creating sale failed, no ID obtained.");
                    }
                }
            }

            // --- ب. تنفيذ إدراج الأصناف وتحديث المخزون ---
            try (PreparedStatement psItem = conn.prepareStatement(insertSaleItemSQL);
                 PreparedStatement psStock = conn.prepareStatement(updateProductStockSQL)) {
                 
                for (SaleItem item : items) {
                    // إعداد بيانات الصنف
                    psItem.setLong(1, saleId);
                    psItem.setInt(2, item.getProductId());
                    psItem.setString(3, item.getProductName());
                    psItem.setString(4, item.getUnit()); // حفظ الوحدة
                    psItem.setInt(5, item.getQuantity());
                    psItem.setDouble(6, item.getUnitPrice());
                    psItem.setDouble(7, item.getTotalPrice()); // subtotal
                    psItem.addBatch();

                    // إعداد تحديث المخزون
                    psStock.setInt(1, item.getQuantity());
                    psStock.setInt(2, item.getProductId());
                    psStock.addBatch();
                }
                
                psItem.executeBatch();
                psStock.executeBatch();
            }

            // --- ج. تحديث رصيد العميل ---
            if (customerId > 0) {
                double remainingDebt = totalAmount - paidAmount;
                // نحدث الرصيد فقط إذا كان هناك مبلغ متبقي (دين) أو دفع زائد
                if (remainingDebt != 0) {
                    try (PreparedStatement psUpdate = conn.prepareStatement(updateCustomerBalanceSQL)) {
                        psUpdate.setDouble(1, remainingDebt);
                        psUpdate.setInt(2, customerId);
                        psUpdate.executeUpdate();
                    }
                }
            }

            // تثبيت المعاملة (كل شيء تم بنجاح)
            conn.commit();
            System.out.println("تم حفظ الفاتورة بنجاح رقم: " + saleId);
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            // في حالة حدوث أي خطأ، نتراجع عن كل التغييرات
            if (conn != null) {
                try {
                    System.err.println("حدث خطأ! جاري التراجع عن المعاملة...");
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            // إعادة الاتصال للوضع الطبيعي وإغلاقه
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * تسجيل دفعة جديدة لفاتورة سابقة (سداد الآجل).
     * يقوم بتحديث المدفوع في الفاتورة وتعديل رصيد العميل.
     */
    public boolean registerNewPayment(int saleId, double paymentAmount) {
         String getSaleSql = "SELECT customer_id, total_amount, amount_paid FROM Sales WHERE id = ?";
         String updateSaleSql = "UPDATE Sales SET amount_paid = amount_paid + ? WHERE id = ?";
         // عند السداد، الرصيد يزيد (يقل الدين السالب ويقترب للصفر أو يصبح موجباً)
         String updateCustomerSql = "UPDATE customers SET balance = balance - ? WHERE id = ?";
         
         Connection conn = null;
         try {
             conn = DatabaseManager.getConnection();
             conn.setAutoCommit(false); // بدء المعاملة
             
             // 1. التحقق من الفاتورة وجلب العميل
             int customerId = 0;
             try(PreparedStatement ps = conn.prepareStatement(getSaleSql)) {
                 ps.setInt(1, saleId);
                 try(ResultSet rs = ps.executeQuery()) {
                     if(rs.next()) {
                         customerId = rs.getInt("customer_id");
                         if(rs.wasNull()) customerId = 0;
                     } else {
                         throw new SQLException("Sale not found with ID: " + saleId);
                     }
                 }
             }
             
             // 2. تحديث المبلغ المدفوع في الفاتورة
             try(PreparedStatement ps = conn.prepareStatement(updateSaleSql)) {
                 ps.setDouble(1, paymentAmount);
                 ps.setInt(2, saleId);
                 ps.executeUpdate();
             }
             
             // 3. تحديث رصيد العميل
             if(customerId != 0) {
                 try(PreparedStatement ps = conn.prepareStatement(updateCustomerSql)) {
                     ps.setDouble(1, paymentAmount);
                     ps.setInt(2, customerId);
                     ps.executeUpdate();
                 }
             }
             
             conn.commit();
             return true;
             
         } catch(SQLException e) {
             try { if(conn!=null) conn.rollback(); } catch(SQLException ex) { ex.printStackTrace(); }
             e.printStackTrace();
             return false;
         } finally {
             try { if(conn!=null) { conn.setAutoCommit(true); conn.close(); } } catch(SQLException e) { e.printStackTrace(); }
         }
    }
    
    /**
     * جلب تفاصيل الفاتورة (Header) لعرضها في لوحة الفاتورة.
     * يشمل البيانات الأساسية والمبالغ.
     */
    public List<Object> getSaleDetails(int saleId) {
        String sql = "SELECT s.id, s.sale_date, s.customer_name, s.customer_id, s.total_amount, s.amount_paid FROM Sales s WHERE s.id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Arrays.asList(
                        rs.getInt("id"), 
                        rs.getString("sale_date"), 
                        rs.getString("customer_name"), 
                        rs.getInt("customer_id"), 
                        rs.getDouble("total_amount"), 
                        "المندوب", // حقل المندوب (ثابت حالياً أو يمكن جلبه إذا وجد)
                        rs.getDouble("amount_paid")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * جلب قائمة الأصناف لفاتورة معينة.
     * يستخدم لعرض الجدول داخل الفاتورة.
     */
    public List<List<Object>> getSaleItems(int saleId) {
        List<List<Object>> items = new ArrayList<>();
        // استعلام يجلب الوحدة (product_unit)
        String sql = "SELECT product_name, product_unit, quantity, sale_price, subtotal FROM sale_items WHERE sale_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(Arrays.asList(
                        rs.getString("product_name"), 
                        rs.getString("product_unit"), 
                        rs.getInt("quantity"), 
                        rs.getDouble("sale_price"), 
                        rs.getDouble("subtotal")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }
}
