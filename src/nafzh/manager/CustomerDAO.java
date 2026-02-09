package nafzh.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public List<Customer> getAllCustomers() {
        List<Customer> customers = new ArrayList<>();
        // --- START: تعديل الاستعلام لجلب العملاء النشطين فقط ---
        String sql = "SELECT id, name, balance FROM customers WHERE status = 1 ORDER BY name ASC";
        // --- END: تعديل الاستعلام ---

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double balance = rs.getDouble("balance"); 

                customers.add(new Customer(id, name, balance));
            }

        } catch (SQLException e) {
            System.err.println("خطأ أثناء جلب العملاء: " + e.getMessage());
            e.printStackTrace();
        }

        return customers;
    }

    public BigDecimal getCustomerBalance(int customerId) {
        if (customerId == 0) return BigDecimal.ZERO;

        String sql = "SELECT balance FROM customers WHERE id = ?";
        double balance = 0.0;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    balance = rs.getDouble("balance");
                }
            }

        } catch (SQLException e) {
            System.err.println("خطأ أثناء جلب رصيد العميل: " + e.getMessage());
            e.printStackTrace();
        }
        
        return BigDecimal.valueOf(balance);
    }
}
