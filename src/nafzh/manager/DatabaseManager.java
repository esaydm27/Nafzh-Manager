package nafzh.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.Vector;



import java.text.NumberFormat; // <-- إضافة مهمة
import java.text.ParseException; // <-- إضافة مهمة
import java.util.Locale; 

public class DatabaseManager {

    //private static final String URL = "jdbc:sqlite:inventory.db";
    private static String URL; // تم تحويلها لمتغير نهائي غير ساكن ليتم ضبطه برمجياً
    private static Connection connection;
    private static boolean isInitialized = false; // متغير الحالة لمنع التكرار
   
    public static class Customer {
        public final int id;
        public final String name;
        public final double balance; // Added balance for payment logic

        // Constructor for basic usage (dropdowns)
        public Customer(int id, String name) {
            this(id, name, 0.0);
        }

        // Full constructor
        public Customer(int id, String name, double balance) {
            this.id = id;
            this.name = name;
            this.balance = balance;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

   public User checkUserCredentials(String username, String password) {
    String sql = "SELECT id, username, role FROM users WHERE username = ? AND password = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setString(1, username);
        pstmt.setString(2, password);
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                // إذا تم العثور على المستخدم، قم بإرجاع كائن User
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("role"));
            }
        }
    } catch (SQLException e) {
        System.err.println("خطأ أثناء التحقق من بيانات الدخول: " + e.getMessage());
    }
    return null; // إذا فشل التحقق، أرجع null
}

   boolean checkLogin(String user, String pass) {
    return checkUserCredentials(user, pass) != null;
}
   
    public static class Installment {
        public final int id;
        public final int saleId;
        public final String customerName; // مضاف للتوافق مع الجدول
        public final double amount;
        public final String dueDate;
        public final String paymentDate; // مضاف لعرض تاريخ السداد الفعلي
        public final boolean isPaid;

        public Installment(int id, int saleId, String customerName, double amount, String dueDate, String paymentDate, boolean isPaid) {
            this.id = id;
            this.saleId = saleId;
            this.customerName = customerName;
            this.amount = amount;
            this.dueDate = dueDate;
            this.paymentDate = (paymentDate == null || paymentDate.isEmpty()) ? "-" : paymentDate;
            this.isPaid = isPaid;
        }
    }
   
    
    public static class ProductForPOS {
        public final int id;
        public final String name;
        public final String unit;
        public final double salePrice;
        public final int currentQuantity;

        public ProductForPOS(int id, String name, String unit, double salePrice, int currentQuantity) {
            this.id = id;
            this.name = name;
            this.unit = unit;
            this.salePrice = salePrice;
            this.currentQuantity = currentQuantity;
        }
    }

    public DatabaseManager() {
        // نضمن أن المسار يتم تعريفه مرة واحدة فقط عند تشغيل التطبيق لأول مرة
        if (URL == null) {
            initializeDatabasePath();
        }
        
        // فتح الاتصال
        connect();
        
        // إنشاء الجداول والبيانات الأساسية مرة واحدة فقط لمنع ثقل التشغيل
        if (!isInitialized) {
            createNewTables();
            isInitialized = true; 
            System.out.println("Maintenance: Database Initialized Successfully.");
        }
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                if (URL != null) {
                    connection = DriverManager.getConnection(URL);
                }
            }
        } catch (SQLException e) {
            System.err.println("Critical Error: Connection lost. " + e.getMessage());
        }
        return connection;
    }
    
    private static String getDatabasePath() {
    String dbName = "inventory.db";
    String workingDirectory;
    String OS = System.getProperty("os.name").toUpperCase();

    if (OS.contains("WIN")) {
        // في ويندوز نستخدم AppData/Local/NafzhManager
        workingDirectory = System.getenv("AppData") + File.separator + "NafzhManager";
    } else {
        // في لينكس أو ماك نستخدم مجلد المستخدم الرئيسي
        workingDirectory = System.getProperty("user.home") + File.separator + ".nafzh_manager";
    }

    // إنشاء المجلد إذا لم يكن موجوداً
    File folder = new File(workingDirectory);
    if (!folder.exists()) {
        folder.mkdirs();
    }

    return "jdbc:sqlite:" + workingDirectory + File.separator + dbName;
}
   
    private void initializeDatabasePath() {
        String dbName = "inventory.db";
        String oldPath = System.getProperty("user.dir") + File.separator + dbName;
        String newFolderPath;
        String OS = System.getProperty("os.name").toUpperCase();

        if (OS.contains("WIN")) {
            newFolderPath = System.getenv("AppData") + File.separator + "Nafzh";
        } else {
            newFolderPath = System.getProperty("user.home") + File.separator + ".nafzh";
        }

        File folder = new File(newFolderPath);
        if (!folder.exists()) folder.mkdirs();

        String targetPath = newFolderPath + File.separator + dbName;
        File oldDbFile = new File(oldPath);
        File targetDbFile = new File(targetPath);

        // هجرة البيانات (Migration)
        if (oldDbFile.exists() && !targetDbFile.exists()) {
            try {
                Files.move(oldDbFile.toPath(), targetDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Maintenance: Database moved to AppData.");
            } catch (IOException e) {
                URL = "jdbc:sqlite:" + oldPath;
                return;
            }
        }
        URL = "jdbc:sqlite:" + targetPath;
    }
   /* 
    public void createNewTables() {
    String[] sqlCommands = {
        // جدول الفئات
        "CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, category_name TEXT NOT NULL UNIQUE);",

        // جدول المنتجات
        "CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, category TEXT NOT NULL, unit TEXT NOT NULL DEFAULT 'قطعة', current_quantity INTEGER NOT NULL DEFAULT 0, purchase_price REAL NOT NULL DEFAULT 0.0, sale_price REAL NOT NULL DEFAULT 0.0, category_id INTEGER, FOREIGN KEY(category_id) REFERENCES categories(id));",
        
        // جدول العملاء
        "CREATE TABLE IF NOT EXISTS customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, address TEXT, phone TEXT, balance REAL DEFAULT 0.0, transaction_count INTEGER DEFAULT 0, status INTEGER DEFAULT 1);", 
        
        // جدول المبيعات (محدث بالأعمدة الجديدة)
        "CREATE TABLE IF NOT EXISTS Sales (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_date TEXT NOT NULL, customer_id INTEGER, customer_name TEXT, total_amount REAL NOT NULL, amount_paid REAL DEFAULT 0.0, paid_amount REAL DEFAULT 0.0, remaining_amount REAL DEFAULT 0.0, final_amount REAL DEFAULT 0.0, FOREIGN KEY(customer_id) REFERENCES customers(id));",
        
        // جدول أصناف المبيعات (sale_items هو الاسم المعتمد)
        "CREATE TABLE IF NOT EXISTS sale_items (sale_id INTEGER, product_id INTEGER, product_name TEXT, product_unit TEXT, quantity INTEGER NOT NULL, sale_price REAL NOT NULL, subtotal REAL NOT NULL, PRIMARY KEY (sale_id, product_id), FOREIGN KEY (sale_id) REFERENCES Sales(id), FOREIGN KEY (product_id) REFERENCES products(id));",
        
        // جدول الوحدات
        "CREATE TABLE IF NOT EXISTS units (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE);",
    
        // جدول الأقساط
        "CREATE TABLE IF NOT EXISTS installments (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER, customer_id INTEGER, due_date TEXT, payment_date TEXT, amount REAL, is_paid INTEGER DEFAULT 0, FOREIGN KEY(sale_id) REFERENCES Sales(id), FOREIGN KEY(customer_id) REFERENCES customers(id));",

        // جدول المستخدمين
        "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password TEXT NOT NULL, role TEXT DEFAULT 'user');",
    
        // إعدادات التطبيق
        "CREATE TABLE IF NOT EXISTS app_settings (id INTEGER PRIMARY KEY CHECK (id = 1), company_name TEXT, slogan TEXT, phone1 TEXT, phone2 TEXT, salesman TEXT, logo BLOB);"
    };
    // داخل دالة createNewTables في DatabaseManager.java، بعد حلقة for الخاصة بالجداول:


        // إضافة الوحدات الافتراضية
        String[] defaultUnits = {"قطعة", "علبة", "باكيت", "كرتونة","دستة", "طقم", "شريط", "درزن", "ربطة", "زوج", "صندوق", "كيس", "لفة"};
        try (Statement stmt = connection.createStatement()) {
            for (String unit : defaultUnits) {
                // نستخدم INSERT OR IGNORE لتجنب التكرار إذا كانت موجودة
                stmt.execute("INSERT OR IGNORE INTO units (name) VALUES ('" + unit + "')");
            }
            System.out.println("تم إضافة الوحدات الافتراضية.");
        } catch (SQLException e) {
            System.err.println("خطأ في إضافة الوحدات: " + e.getMessage());
        }


    try (Statement stmt = connection.createStatement()) {
        for(String sql : sqlCommands) stmt.execute(sql);
        System.out.println("تم إنشاء الجداول الموحدة بنجاح.");
    } catch (SQLException e) { 
        System.err.println("Error creating tables: " + e.getMessage()); 
    }
}
*/
    public void createNewTables() {
        // الكود لا يتغير هنا، فقط نستخدمه داخل شرط المرة الواحدة في الـ Constructor
        String[] tables = {
            "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, role TEXT)",
            "CREATE TABLE IF NOT EXISTS units (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)",
            "CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, category TEXT, barcode TEXT UNIQUE, unit_id INTEGER, purchase_price REAL, sale_price REAL, stock_quantity REAL, min_stock REAL, FOREIGN KEY(unit_id) REFERENCES units(id))",
            "CREATE TABLE IF NOT EXISTS customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, address TEXT, balance REAL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS sales (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER, sale_date TEXT, total_amount REAL, amount_paid REAL, payment_status TEXT, user_id INTEGER, FOREIGN KEY(customer_id) REFERENCES customers(id))",
            "CREATE TABLE IF NOT EXISTS sale_items (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER, product_id INTEGER, quantity REAL, unit_price REAL, total_price REAL, FOREIGN KEY(sale_id) REFERENCES sales(id), FOREIGN KEY(product_id) REFERENCES products(id))",
            "CREATE TABLE IF NOT EXISTS business_settings (id INTEGER PRIMARY KEY CHECK (id = 1), name TEXT, slogan TEXT, phone1 TEXT, phone2 TEXT, salesman_name TEXT, logo BLOB)",
            "CREATE TABLE IF NOT EXISTS installments (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER, customer_id INTEGER, amount REAL, due_date TEXT, payment_date TEXT, is_paid INTEGER DEFAULT 0, FOREIGN KEY(sale_id) REFERENCES sales(id), FOREIGN KEY(customer_id) REFERENCES customers(id))"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : tables) {
                stmt.execute(sql);
            }
            initializeDefaultData(stmt);
        } catch (SQLException e) {
            System.err.println("Table Creation Error: " + e.getMessage());
        }
    }
    private void ensureTableStructureIsUpdated() {
        // مثال لمستقبل التطبيق: إذا أردت إضافة عمود رقم الهاتف الثاني لجدول المستخدمين
        // checkAndAddColumn("users", "phone_secondary", "TEXT DEFAULT ''");
    }
    
    public final void connect() {
        try {
            if (URL == null) {
                initializeDatabasePath(); // تأمين إضافي لضمان عدم وجود URL فارغ
            }
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL);
            }
        } catch (SQLException e) {
            System.err.println("Connection Error: " + e.getMessage());
        }
    }
    
    private void checkAndAddColumn(String tableName, String columnName, String columnType) {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, columnName);
            if (!rs.next()) {
                stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
                System.out.println("Maintenance: Added column " + columnName + " to " + tableName);
            }
        } catch (SQLException e) {
            System.err.println("Column Update Error: " + e.getMessage());
        }
    }
 
    private void initializeDefaultData(Statement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM units");
        if (rs.next() && rs.getInt(1) == 0) {
            String[] defaultUnits = {"قطعة", "كرتون", "كيلو", "متر", "علبة"};
            for (String unit : defaultUnits) {
                stmt.execute("INSERT INTO units (name) VALUES ('" + unit + "')");
            }
            System.out.println("تم إضافة الوحدات الافتراضية.");
        }
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
        if (rs.next() && rs.getInt(1) == 0) {
            stmt.execute("INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'super_admin')");
            System.out.println("تم إضافة المستخدم المسؤول الافتراضي.");
        }
    }
    
    private void ensureColumnsExist() {
        try (Statement stmt = connection.createStatement()) {
            // إضافة عمود paid_amount لجدول المبيعات إذا لم يكن موجوداً stmt.execute("ALTER TABLE sales ADD COLUMN paid_amount REAL DEFAULT 0") ;
            System.out.println("[تتبع] تم فحص وتحديث أعمدة قاعدة البيانات.") ;
        }
        catch (SQLException e) {
            // نتجاهل الخطأ إذا كان العمود موجوداً بالفعل if (!e.getMessage().contains("duplicate column name")) {
                System.err.println("تنبيه عند تحديث الجداول: " + e.getMessage()) ;
            }
        }
   
    public double getTotalUnpaidInstallments() {
        String sql = "SELECT SUM(amount) FROM installments WHERE is_paid = 0";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Error fetching total unpaid installments: " + e.getMessage());
        }
        return 0.0;
    }
  
    public boolean markInstallmentAsPaid(int installmentId) {
        String today = LocalDate.now().toString();
        String sql = "UPDATE installments SET is_paid = 1, payment_date = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, today);
            pstmt.setInt(2, installmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating installment status: " + e.getMessage());
            return false;
        }
    }
   
    public boolean updateInstallmentStatus(int installmentId, String paymentDate, boolean newIsPaid) {
    String selectSql = "SELECT customer_id, amount, is_paid FROM installments WHERE id = ?";
    String updateInstSql = "UPDATE installments SET payment_date = ?, is_paid = ? WHERE id = ?";
    String updateCustomerSql = "UPDATE customers SET balance = balance + ? WHERE id = ?";

    try {
        connection.setAutoCommit(false); // مهم جداً لضمان تناسق البيانات

        int customerId = -1;
        double amount = 0;
        boolean oldIsPaid = false;

        // 1. جلب البيانات الحالية للقسط
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setInt(1, installmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    customerId = rs.getInt("customer_id");
                    amount = rs.getDouble("amount");
                    oldIsPaid = rs.getInt("is_paid") == 1;
                } else {
                    return false; // القسط غير موجود
                }
            }
        }

        // إذا لم تتغير الحالة، لا نفعل شيئاً سوى تحديث التاريخ ربما
        if (oldIsPaid == newIsPaid) {
             // تحديث التاريخ فقط
            try (PreparedStatement ps = connection.prepareStatement(updateInstSql)) {
                ps.setString(1, paymentDate);
                ps.setInt(2, newIsPaid ? 1 : 0);
                ps.setInt(3, installmentId);
                ps.executeUpdate();
            }
            connection.commit();
            connection.setAutoCommit(true);
            return true;
        }

        // 2. تحديث جدول الأقساط
        try (PreparedStatement ps = connection.prepareStatement(updateInstSql)) {
            ps.setString(1, paymentDate);
            ps.setInt(2, newIsPaid ? 1 : 0);
            ps.setInt(3, installmentId);
            ps.executeUpdate();
        }

        // 3. تحديث رصيد العميل (أخطر خطوة)
        // الرصيد الموجب = دين على العميل
        // إذا سدد (Unpaid -> Paid) : نقلل الرصيد (-amount)
        // إذا ألغى السداد (Paid -> Unpaid) : نعيد الدين (+amount)
        double balanceChange = 0;
        if (!oldIsPaid && newIsPaid) {
            balanceChange = -amount; // سداد
        } else if (oldIsPaid && !newIsPaid) {
            balanceChange = amount;  // تراجع عن السداد
        }

        if (customerId != -1 && balanceChange != 0) {
            try (PreparedStatement ps = connection.prepareStatement(updateCustomerSql)) {
                ps.setDouble(1, balanceChange);
                ps.setInt(2, customerId);
                ps.executeUpdate();
            }
        }

        connection.commit();
        return true;

    } catch (SQLException e) {
        try { connection.rollback(); } catch (SQLException ex) {}
        System.err.println("Error updating installment status: " + e.getMessage());
        return false;
    } finally {
        try { connection.setAutoCommit(true); } catch (SQLException e) {}
    }
}

    public boolean saveInstallmentSale(int customerId, double totalAmount, double downPayment, Map<Integer, Integer> cartItems, List<Object[]> installments) {
    String saleSql = "INSERT INTO Sales (customer_id, customer_name, total_amount, amount_paid, sale_date) VALUES (?, ?, ?, ?, ?)";
    // تم تعديل ترتيب الأعمدة هنا ليكون مطابقًا للترتيب في الواجهة
    String instSql = "INSERT INTO installments (sale_id, customer_id, due_date, amount, payment_date, is_paid) VALUES (?, ?, ?, ?, ?, ?)";
    String updateStockSql = "UPDATE products SET current_quantity = current_quantity - ? WHERE id = ?";
    String itemSql = "INSERT INTO sale_items(sale_id, product_id, product_name, quantity, sale_price, subtotal, product_unit) VALUES(?, ?, ?, ?, ?, ?, ?)";
    String updateCustomerSql = "UPDATE customers SET balance = balance + ? WHERE id = ?";
    String customerNameSql = "SELECT name FROM customers WHERE id = ?";

    try {
        connection.setAutoCommit(false); // بدء المعاملة

        // --- أ: جلب اسم العميل ---
        String customerName = "عميل تقسيط";
        if (customerId > 0) {
            try (PreparedStatement pstmt = connection.prepareStatement(customerNameSql)) {
                pstmt.setInt(1, customerId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    customerName = rs.getString("name");
                }
            }
        }

        // --- ب: حفظ رأس الفاتورة ---
        int saleId;
        try (PreparedStatement pstmt = connection.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, customerId);
            pstmt.setString(2, customerName);
            pstmt.setDouble(3, totalAmount);
            pstmt.setDouble(4, downPayment);
            pstmt.setString(5, LocalDate.now().toString());
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                saleId = rs.getInt(1);
            } else {
                throw new SQLException("فشل في الحصول على رقم الفاتورة.");
            }
        }

        // --- ج: حفظ الأصناف وتحديث المخزون ---
        try (PreparedStatement itemPstmt = connection.prepareStatement(itemSql);
             PreparedStatement stockPstmt = connection.prepareStatement(updateStockSql)) {
            for (Map.Entry<Integer, Integer> entry : cartItems.entrySet()) {
                int productId = entry.getKey();
                int qty = entry.getValue();
                ProductForPOS p = getProductByIdForPOS(productId);
                if (p == null || p.currentQuantity < qty) throw new SQLException("الكمية غير كافية للمنتج: " + (p != null ? p.name : "ID " + productId));

                double subtotal = p.salePrice * qty;
                itemPstmt.setInt(1, saleId);
                itemPstmt.setInt(2, p.id);
                itemPstmt.setString(3, p.name);
                itemPstmt.setInt(4, qty);
                itemPstmt.setDouble(5, p.salePrice);
                itemPstmt.setDouble(6, subtotal);
                itemPstmt.setString(7, p.unit);
                itemPstmt.addBatch();

                stockPstmt.setInt(1, qty);
                stockPstmt.setInt(2, productId);
                stockPstmt.addBatch();
            }
            itemPstmt.executeBatch();
            stockPstmt.executeBatch();
        }

        // --- د: حفظ الأقساط وحساب الدين ---
        double totalDebtToAdd = 0.0;
        try (PreparedStatement pstmt = connection.prepareStatement(instSql)) {
            for (Object[] row : installments) {
                pstmt.setInt(1, saleId);
                pstmt.setInt(2, customerId);

                // --- الترتيب الصحيح والنهائي للقراءة ---
                // الآن الكود يقرأ البيانات بالترتيب الصحيح الذي أرسلناه
                
                // العنصر 1: تاريخ الاستحقاق
                String dueDate = row[1].toString();
                pstmt.setString(3, dueDate);

                // العنصر 2: المبلغ
                double amount = (double) row[2];
                pstmt.setDouble(4, amount);
                
                // العنصر 3: تاريخ السداد
                pstmt.setString(5, row[3].toString());
                
                // العنصر 4: حالة السداد
                boolean isPaid = (boolean) row[4];
                pstmt.setInt(6, isPaid ? 1 : 0);
                
                pstmt.addBatch();

                if (!isPaid) {
                    totalDebtToAdd += amount;
                }
            }
            pstmt.executeBatch();
        }

        // --- هـ: تحديث مديونية العميل ---
        if (customerId > 0 && totalDebtToAdd > 0) {
            try (PreparedStatement pstmt = connection.prepareStatement(updateCustomerSql)) {
                pstmt.setDouble(1, totalDebtToAdd);
                pstmt.setInt(2, customerId);
                pstmt.executeUpdate();
            }
        }

        connection.commit(); // اعتماد كل العمليات
        return true;

    } catch (SQLException e) {
        try {
            if (connection != null) connection.rollback(); // التراجع عند الخطأ
        } catch (SQLException ex) {
            System.err.println("فشل التراجع عن العملية: " + ex.getMessage());
        }
        System.err.println("Error saving installment sale: " + e.getMessage());
        e.printStackTrace();
        return false;
    } finally {
        try {
            if (connection != null) connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

    public boolean updateProductQuantity(int productId, int quantityChange) {
    String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ?" ;
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setInt(1, quantityChange) ;
        pstmt.setInt(2, productId) ;
        return pstmt.executeUpdate() > 0 ;
    }
    catch (SQLException e) {
        System.err.println("خطأ أثناء تحديث كمية المنتج: " + e.getMessage()) ;
        return false ;
    }
}

    private double getProductPrice(int productId) {
        String sql = "SELECT sale_price FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDouble("sale_price");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
        
    public boolean addProduct(String name, String category, String unit, int currentQuantity, double purchasePrice, double salePrice) {
        String sql = "INSERT INTO products(name, category, unit, current_quantity, purchase_price, sale_price) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name); pstmt.setString(2, category); pstmt.setString(3, unit);
            pstmt.setInt(4, currentQuantity); pstmt.setDouble(5, purchasePrice); pstmt.setDouble(6, salePrice);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { System.err.println("Error adding product: " + e.getMessage()); return false; }
    }

    public List<Map<String, Object>> getAllProducts() {
        List<Map<String, Object>> productsList = new ArrayList<>();
        String sql = "SELECT id, name, category, unit, current_quantity, purchase_price, sale_price FROM products ORDER BY category, id";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", rs.getInt("id")); product.put("name", rs.getString("name"));
                product.put("category", rs.getString("category")); product.put("unit", rs.getString("unit"));
                product.put("current_quantity", rs.getInt("current_quantity"));
                product.put("purchase_price", rs.getDouble("purchase_price")); product.put("sale_price", rs.getDouble("sale_price"));
                productsList.add(product);
            }
        } catch (SQLException e) { System.err.println("Error fetching all products: " + e.getMessage()); }
        return productsList;
    }

    public String getProductUnit(int id) {
        String sql = "SELECT unit FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getString("unit"); }
        } catch (SQLException e) { System.err.println("Error fetching product unit: " + e.getMessage()); }
        return "";
    }

    public boolean updateProduct(int id, String name, String category, String unit, int currentQuantity, double purchasePrice, double salePrice) {
        String sql = "UPDATE products SET name = ?, category = ?, unit = ?, current_quantity = ?, purchase_price = ?, sale_price = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name); pstmt.setString(2, category); pstmt.setString(3, unit);
            pstmt.setInt(4, currentQuantity); pstmt.setDouble(5, purchasePrice); pstmt.setDouble(6, salePrice);
            pstmt.setInt(7, id);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { System.err.println("Error updating product: " + e.getMessage()); return false; }
    }

    public boolean deleteProduct(int id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error deleting product: " + e.getMessage()); return false; }
    }

    public boolean addUser(String username, String password, String role) {
        String sql = "INSERT INTO users(username, password, role) VALUES(?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("خطأ في إضافة المستخدم (قد يكون الاسم مكرر): " + e.getMessage());
            return false;
        }
    }

    public boolean isUsersTableEmpty() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT id, username, role FROM users")) {
        while (rs.next()) {
            users.add(new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("role")
            ));
        }
    } catch (SQLException e) { e.printStackTrace(); }
    return users;
}

    public boolean deleteUser(int id) {
        try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public int getLowStockCount() {
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(id) FROM products WHERE current_quantity <= 5 AND current_quantity > 0")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("Error getting low stock count: " + e.getMessage()); }
        return 0;
    }

    public int getProductCount() {
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(id) FROM products")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("Error getting product count: " + e.getMessage()); }
        return 0;
    }

    public double getTotalSalesByDate(String date) {
        String sql = "SELECT SUM(total_amount) FROM Sales WHERE sale_date = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, date);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getDouble(1); }
        } catch (SQLException e) { System.err.println("Error getting total sales by date: " + e.getMessage()); }
        return 0.0;
    }

    public List<List<Object>> getAllSaleTransactions() {
    List<List<Object>> list = new ArrayList<>();
    
    // هذا الاستعلام يطبق المنطق الذي طلبته بالضبط:
    // إذا كانت العملية لها أقساط: الإجمالي = المقدم (amount_paid) + مجموع الأقساط.
    // إذا كانت كاش: الإجمالي = total_amount المسجل.
    
    String query = """
        SELECT 
            s.id, 
            s.sale_date, 
            s.customer_name,
            CASE 
                -- إذا وجدنا أقساط لهذه العملية في جدول installments
                WHEN EXISTS (SELECT 1 FROM installments WHERE sale_id = s.id) THEN 
                    -- الإجمالي = المدفوع مقدماً + مجموع الأقساط
                    COALESCE(s.amount_paid, 0) + COALESCE((SELECT SUM(amount) FROM installments WHERE sale_id = s.id), 0)
                ELSE 
                    -- وإلا فهي كاش، نأخذ الرقم المسجل
                    s.total_amount 
            END as calculated_total
        FROM Sales s
        ORDER BY s.id DESC
    """;

    try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
        while (rs.next()) {
            double total = rs.getDouble("calculated_total");
            
            list.add(Arrays.asList(
                rs.getInt("id"), 
                rs.getString("sale_date"), 
                rs.getString("customer_name"), 
                total // سيعرض 60580 بدلاً من 56580
            ));
        }
    } catch (SQLException e) { 
        System.err.println("خطأ في جلب سجل المبيعات: " + e.getMessage()); 
    }
    return list;
}

    private double getInstallmentsTotal(int saleId) {
        try (PreparedStatement pstmt = connection.prepareStatement("SELECT SUM(amount) FROM Installments WHERE sale_id = ?")) {
            pstmt.setInt(1, saleId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { return 0; }
        return 0;
    }
    
    public double getRealSaleTotal(int saleId) {
        // هذه الدالة تحسب الإجمالي الحقيقي للفاتورة الواحدة
        // المعادلة: المقدم (amount_paid) + مجموع الأقساط (installments)
        // وإذا لم يكن هناك أقساط، تعيد total_amount العادي

        String query = """
            SELECT 
                CASE 
                    WHEN EXISTS (SELECT 1 FROM installments WHERE sale_id = s.id) THEN 
                        COALESCE(s.amount_paid, 0) + COALESCE((SELECT SUM(amount) FROM installments WHERE sale_id = s.id), 0)
                    ELSE 
                        s.total_amount 
                END as real_total
            FROM Sales s
            WHERE s.id = ?
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("real_total");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error calculating real sale total: " + e.getMessage());
        }
        return 0.0;
    }

    private double calculateRealTotal(int saleId, double originalTotal) {
            // نحسب مجموع الأقساط لهذه البيعة
            double installmentsSum = 0;
            try (PreparedStatement pstmt = connection.prepareStatement("SELECT SUM(amount) FROM Installments WHERE sale_id = ?")) {
                pstmt.setInt(1, saleId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    installmentsSum = rs.getDouble(1);
                }
            } catch (SQLException e) {
                return originalTotal; // في حالة الخطأ، نعود للأصل
            }

            // المنطق:
            // إذا كان مجموع الأقساط 0، فهذا بيع كاش، والسعر هو originalTotal.
            if (installmentsSum == 0) {
                return originalTotal;
            }

            // إذا كان هناك أقساط:
            // هنا المعضلة: كيف نعرف المقدم؟
            // سنفترض أن originalTotal هو المبلغ "الكاش" قبل الفائدة.
            // وأن installmentsSum هو المبلغ "المقسط" (المتبقي + الفائدة).
            // إذن: السعر الكلي = المقدم + مجموع الأقساط.
            // المقدم = originalTotal - (أصل مبلغ الأقساط بدون فائدة). <-- هذا مجهول!

            // محاولة ذكية: 
            // هل originalTotal في جدول Sales يتم تحديثه ليشمل الفائدة؟ لا يبدو ذلك.

            // الحل الأرجح:
            // في أنظمة التقسيط، عادة يتم تسجيل البيعة بـ:
            // total_amount = السعر الكاش (أو السعر الأساسي).
            // ويتم دفع جزء منه كمقدم.

            // إذا افترضنا أن الـ 4000 (المقدم) + الـ 56580 (الأقساط) = 60580.
            // هذا يعني أننا يجب أن نجمع المقدم + الأقساط.

            // ولكننا لا نعرف المقدم من قاعدة البيانات مباشرة لعدم وجود عمود paid_amount.
            // لذا، سنضطر لاستنتاجه من تفاصيل الفاتورة (SaleItems).

            // نحسب إجمالي سعر المنتجات (سعر الوحدة * الكمية) من جدول SaleItems
            double productsTotal = 0;
            try (PreparedStatement pstmt = connection.prepareStatement("SELECT SUM(total_price) FROM SaleItems WHERE sale_id = ?")) {
                pstmt.setInt(1, saleId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    productsTotal = rs.getDouble(1);
                }
            } catch (SQLException e) { }

            // الآن: 
            // productsTotal هو (60580) الذي رأيته في الفاتورة؟ أم هو السعر الكاش؟
            // إذا كان productsTotal هو السعر النهائي (بالفائدة)، فالحل بسيط جداً: استخدم productsTotal بدلاً من total_amount!

            // دعنا نجرب استخدام مجموع SaleItems كحل بديل وموثوق
            return productsTotal > 0 ? productsTotal : originalTotal;
        }

    public List<Object> getSaleDetails(int saleId) {
        String sql = "SELECT s.id, s.sale_date, s.customer_id, s.customer_name, s.total_amount, s.amount_paid, c.balance " +
                     "FROM Sales s " +
                     "LEFT JOIN customers c ON s.customer_id = c.id " +
                     "WHERE s.id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Arrays.asList(
                        rs.getInt("id"), 
                        rs.getString("sale_date"), 
                        rs.getString("customer_name"), 
                        rs.getInt("customer_id"), 
                        rs.getDouble("total_amount"), 
                        "مندوب المبيعات", 
                        rs.getDouble("amount_paid"), // New field
                        rs.getDouble("balance")      // Customer Balance
                    );
                }
            }
        } catch (SQLException e) { System.err.println("Error fetching sale header details: " + e.getMessage()); }
        return null;
    }

    public double getCustomerPriorBalance(int customerId) {
        String sql = "SELECT balance FROM customers WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getDouble("balance"); }
        } catch (SQLException e) { System.err.println("Error fetching customer balance: " + e.getMessage()); }
        return 0.0;
    }

    public List<List<Object>> getSaleItems(int saleId) {
        List<List<Object>> items = new ArrayList<>();
        String sql = "SELECT product_name, product_unit, quantity, sale_price, subtotal FROM sale_items WHERE sale_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, saleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) items.add(Arrays.asList(rs.getString("product_name"), rs.getString("product_unit"), rs.getInt("quantity"), rs.getDouble("sale_price"), rs.getDouble("subtotal")));
            }
        } catch (SQLException e) { System.err.println("Error fetching sale items: " + e.getMessage()); }
        return items;
    }

    public ProductForPOS getProductByIdForPOS(int productId) {
        String sql = "SELECT id, name, unit, sale_price, current_quantity FROM products WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return new ProductForPOS(rs.getInt("id"), rs.getString("name"), rs.getString("unit"), rs.getDouble("sale_price"), rs.getInt("current_quantity"));
            }
        } catch (SQLException e) { System.err.println("Error fetching product by ID: " + e.getMessage()); }
        return null;
    }

    public List<ProductForPOS> getProductsByName(String partialName) {
        List<ProductForPOS> products = new ArrayList<>();
        String sql = "SELECT id, name, unit, sale_price, current_quantity FROM products WHERE name LIKE ? ORDER BY name";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, partialName + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) products.add(new ProductForPOS(rs.getInt("id"), rs.getString("name"), rs.getString("unit"), rs.getDouble("sale_price"), rs.getInt("current_quantity")));
            }
        } catch (SQLException e) { System.err.println("Error fetching products by name: " + e.getMessage()); }
        return products;
    }

    public int addSaleTransaction(int customerId, String customerName, double totalAmount, double amountPaid, Map<Integer, Integer> cartItems) {
        int saleId = -1;
        String saleDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        
        // تم التعديل لإضافة amount_paid
        String sqlSale = "INSERT INTO Sales(sale_date, customer_id, customer_name, total_amount, amount_paid) VALUES(?, ?, ?, ?, ?)";

        try {
            connection.setAutoCommit(false); // Begin Transaction

            // 1. Insert Sale Record
            try (PreparedStatement pstmtSale = connection.prepareStatement(sqlSale, Statement.RETURN_GENERATED_KEYS)) {
                pstmtSale.setString(1, saleDate);
                if (customerId == -1) pstmtSale.setNull(2, java.sql.Types.INTEGER);
                else pstmtSale.setInt(2, customerId);
                pstmtSale.setString(3, customerName);
                pstmtSale.setDouble(4, totalAmount);
                pstmtSale.setDouble(5, amountPaid); // New
                
                pstmtSale.executeUpdate();
                try (ResultSet rs = pstmtSale.getGeneratedKeys()) {
                    if (rs.next()) saleId = rs.getInt(1);
                }
            }
            if (saleId == -1) throw new SQLException("Failed to create sale, no ID obtained.");

            String sqlItem = "INSERT INTO sale_items(sale_id, product_id, product_name, quantity, sale_price, subtotal, product_unit) VALUES(?, ?, ?, ?, ?, ?, ?)";
            String sqlUpdateStock = "UPDATE products SET current_quantity = current_quantity - ? WHERE id = ?";

            try (PreparedStatement pstmtItem = connection.prepareStatement(sqlItem); PreparedStatement pstmtUpdateStock = connection.prepareStatement(sqlUpdateStock)) {
                for (Map.Entry<Integer, Integer> entry : cartItems.entrySet()) {
                    int productId = entry.getKey();
                    int quantitySold = entry.getValue();
                    ProductForPOS p = getProductByIdForPOS(productId);
                    if (p == null || p.currentQuantity < quantitySold) throw new SQLException("Stock not available for product ID: " + productId);

                    // Insert Item
                    pstmtItem.setInt(1, saleId);
                    pstmtItem.setInt(2, p.id);
                    pstmtItem.setString(3, p.name);
                    pstmtItem.setInt(4, quantitySold);
                    pstmtItem.setDouble(5, p.salePrice);
                    pstmtItem.setDouble(6, p.salePrice * quantitySold);
                    pstmtItem.setString(7, p.unit);
                    pstmtItem.addBatch();

                    // Update Stock
                    pstmtUpdateStock.setInt(1, quantitySold);
                    pstmtUpdateStock.setInt(2, p.id);
                    pstmtUpdateStock.addBatch();
                }
                pstmtItem.executeBatch();
                pstmtUpdateStock.executeBatch();
            }
            
            // 2. Update Customer Balance if needed (New Logic)
            if (customerId != -1) {
                double remaining = totalAmount - amountPaid;
                if (remaining != 0 || totalAmount > 0) {
                     // Add the remaining debt (or credit) to the customer balance
                     // balance = balance + (total - paid)
                     String updateBalanceSql = "UPDATE customers SET balance = balance + ? WHERE id = ?";
                     try(PreparedStatement psBalance = connection.prepareStatement(updateBalanceSql)) {
                         psBalance.setDouble(1, remaining);
                         psBalance.setInt(2, customerId);
                         psBalance.executeUpdate();
                     }
                }
            }

            connection.commit(); // Commit Transaction

        } catch (SQLException e) {
            System.err.println("Transaction failed: " + e.getMessage());
            try { if (connection != null) connection.rollback(); } catch (SQLException ex) { System.err.println("Rollback failed: " + ex.getMessage()); }
            return -1;
        } finally {
            try { if (connection != null) connection.setAutoCommit(true); } catch (SQLException e) { System.err.println("Error resetting auto-commit: " + e.getMessage()); }
        }
        return saleId;
    }
    
    public int addSaleTransaction(int customerId, String customerName, double totalAmount, Map<Integer, Integer> cartItems) {
        return addSaleTransaction(customerId, customerName, totalAmount, totalAmount, cartItems);
    }
    
    public boolean registerNewPayment(int saleId, double paymentAmount) {
         String getSaleSql = "SELECT customer_id, total_amount, amount_paid FROM Sales WHERE id = ?";
         String updateSaleSql = "UPDATE Sales SET amount_paid = amount_paid + ? WHERE id = ?";
         String updateCustomerSql = "UPDATE customers SET balance = balance - ? WHERE id = ?";
         
         try {
             connection.setAutoCommit(false);
             
             int customerId = -1;
             try(PreparedStatement ps = connection.prepareStatement(getSaleSql)) {
                 ps.setInt(1, saleId);
                 try(ResultSet rs = ps.executeQuery()) {
                     if(rs.next()) {
                         customerId = rs.getInt("customer_id");
                         if(rs.wasNull()) customerId = -1;
                     } else {
                         throw new SQLException("Sale not found");
                     }
                 }
             }
             
             // Update Sale
             try(PreparedStatement ps = connection.prepareStatement(updateSaleSql)) {
                 ps.setDouble(1, paymentAmount);
                 ps.setInt(2, saleId);
                 ps.executeUpdate();
             }
             
             // Update Customer Balance
             if(customerId != -1) {
                 try(PreparedStatement ps = connection.prepareStatement(updateCustomerSql)) {
                     ps.setDouble(1, paymentAmount); // Reduce debt
                     ps.setInt(2, customerId);
                     ps.executeUpdate();
                 }
             }
             
             connection.commit();
             return true;
         } catch(SQLException e) {
             try { connection.rollback(); } catch(SQLException ex) {}
             e.printStackTrace();
             return false;
         } finally {
             try { connection.setAutoCommit(true); } catch(SQLException e) {}
         }
    }

    public Vector<String> getAllCategories() {
        Vector<String> cats = new Vector<>();
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT category_name FROM categories ORDER BY category_name")) {
            while (rs.next()) cats.add(rs.getString("category_name"));
        } catch (SQLException e) { System.err.println("Error fetching categories: " + e.getMessage()); }
        return cats;
    }

    public boolean addCategory(String categoryName) {
        try (PreparedStatement pstmt = connection.prepareStatement("INSERT INTO categories(category_name) VALUES(?)")) {
            pstmt.setString(1, categoryName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public boolean deleteCategory(String categoryName) {
        try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM categories WHERE category_name = ?")) {
            pstmt.setString(1, categoryName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public Vector<String> getAllUnits() {
        Vector<String> units = new Vector<>();
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT name FROM units ORDER BY name")) {
            while (rs.next()) units.add(rs.getString("name"));
        } catch (SQLException e) { System.err.println("Error fetching units: " + e.getMessage()); }
        return units;
    }

    public List<Customer> getCustomersForPOS() {
    List<Customer> customers = new ArrayList<>();
    // --- بداية التعديل ---
    // إضافة شرط "WHERE status = 1" لجلب العملاء النشطين فقط
    try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id, name, balance FROM customers WHERE status = 1 ORDER BY name ASC")) {
    // --- نهاية التعديل ---
        while (rs.next()) customers.add(new Customer(rs.getInt("id"), rs.getString("name"), rs.getDouble("balance")));
    } catch (SQLException e) { 
        System.err.println("Error fetching customers for POS: " + e.getMessage()); 
    }
    return customers;
}

    public List<Map<String, Object>> getAllCustomersWithStatus() {
    List<Map<String, Object>> list = new ArrayList<>();
    // تم استخدام استعلام يجلب كل الأعمدة بما في ذلك عمود "status"
    String sql = "SELECT id, name, address, phone, balance, transaction_count, status FROM customers ORDER BY id DESC";
    try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            Map<String, Object> c = new HashMap<>();
            c.put("id", rs.getInt("id")); 
            c.put("name", rs.getString("name"));
            c.put("address", rs.getString("address")); 
            c.put("phone", rs.getString("phone"));
            c.put("balance", rs.getDouble("balance")); 
            c.put("transaction_count", rs.getInt("transaction_count"));
            // إضافة الحالة إلى البيانات المجلوبة
            c.put("status", rs.getInt("status")); 
            list.add(c);
        }
    } catch (SQLException e) { 
        System.err.println("Error fetching all customers with status: " + e.getMessage()); 
    }
    return list;
}

    public List<Map<String, Object>> getAllCustomers() {
    // أصبحت الآن تستدعي الدالة الجديدة لضمان توحيد مصدر البيانات
    return getAllCustomersWithStatus(); 
}

    public void addStatusColumnIfMissing() {
        String sql = "ALTER TABLE customers ADD COLUMN status INTEGER DEFAULT 1";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("تم إضافة عمود status بنجاح.");
        } catch (SQLException e) {
            // إذا كان العمود موجوداً بالفعل، سيحدث خطأ وسنتجاهله
            if (!e.getMessage().contains("duplicate column name")) {
                System.err.println("ملاحظة: " + e.getMessage());
            }
        }
    }
    
    public boolean addCustomer(String name, String address, String phone, double balance, int count) {
    // --- بداية التعديل ---
    // تم تعديل الاستعلام ليشمل عمود "status" ويعطيه القيمة 1 (نشط) بشكل افتراضي
    String sql = "INSERT INTO customers(name, address, phone, balance, transaction_count, status) VALUES(?,?,?,?,?,1)" ;
    // --- نهاية التعديل ---
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setString(1, name) ;
        pstmt.setString(2, address) ;
        pstmt.setString(3, phone) ;
        pstmt.setDouble(4, balance) ;
        pstmt.setInt(5, count) ;
        return pstmt.executeUpdate() > 0 ;
    }
    catch (SQLException e) {
        e.printStackTrace() ;
        return false ;
    }
}
 
    public boolean updateCustomer(int id, String name, String address, String phone, double balance) {
        String sql = "UPDATE customers SET name=?, address=?, phone=?, balance=? WHERE id=?" ;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name) ;
            pstmt.setString(2, address) ;
            pstmt.setString(3, phone) ;
            pstmt.setDouble(4, balance) ;
            pstmt.setInt(5, id) ;
            return pstmt.executeUpdate() > 0 ;
        }
        catch (SQLException e) {
            e.printStackTrace() ;
            return false ;
        }
    }
    
    public static class User {
    public final int id;
    public final String username;
    public final String role;

    public User(int id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    @Override
    public String toString() {
        return username + " (" + role + ")"; // عرض مناسب في ComboBox
    }
}

    public String getUserRole(String username) {
    String role = null;
    try {
        String sql = "SELECT role FROM users WHERE username = ?";
        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            role = rs.getString("role");
        }
        rs.close();
        stmt.close();
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return role;
}

    public boolean isSuperAdmin(String username) {
    String role = getUserRole(username);
    return role != null && role.equals("super_admin");
}

    public void updateCustomerStatus(int customerId, int status) {
    String sql = "UPDATE customers SET status = ? WHERE id = ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setInt(1, status);
        pstmt.setInt(2, customerId);
        pstmt.executeUpdate();
    } catch (SQLException e) {
        System.err.println("Error updating customer status: " + e.getMessage());
    }
}

    public boolean toggleAllCustomerStatuses() {
    // استعلام ذكي لعكس الحالة: 1 يصبح 0، و 0 يصبح 1
    String sql = "UPDATE customers SET status = 1 - status"; 
    try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate(sql);
        return true;
    } catch (SQLException e) {
        System.err.println("Error toggling all customer statuses: " + e.getMessage());
        return false;
    }
}
  
    public boolean deleteCustomer(int id) {
        String sql = "DELETE FROM customers WHERE id=?" ;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id) ;
            return pstmt.executeUpdate() > 0 ;
        }
        catch (SQLException e) {
            e.printStackTrace() ;
            return false ;
        }
    }
    
    public boolean isCustomerNameExists(String name) {
        try (PreparedStatement pstmt = connection.prepareStatement("SELECT 1 FROM customers WHERE name = ? LIMIT 1")) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { System.err.println("Error checking customer name existence: " + e.getMessage()); }
        return false;
    }

    public void close() {
    try {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("تم إغلاق الاتصال بقاعدة البيانات.");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    private void checkAndCreateTables() {
    try (Statement stmt = connection.createStatement()) {
        // إنشاء الجداول الأساسية
        stmt.execute("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)");
        stmt.execute("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, category_id INTEGER, price REAL, stock INTEGER, unit TEXT, FOREIGN KEY(category_id) REFERENCES categories(id))");
        stmt.execute("CREATE TABLE IF NOT EXISTS customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, address TEXT, phone TEXT, balance REAL DEFAULT 0, status INTEGER DEFAULT 1)");
        
        // >>> التعديل المهم جداً هنا: إضافة paid_amount و remaining_amount <<<
        stmt.execute("CREATE TABLE IF NOT EXISTS sales (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER, total_amount REAL, discount REAL, final_amount REAL, paid_amount REAL DEFAULT 0, remaining_amount REAL DEFAULT 0, sale_date TEXT, FOREIGN KEY(customer_id) REFERENCES customers(id))");
        
        // >>> التأكد من جدول التفاصيل <<<
        stmt.execute("CREATE TABLE IF NOT EXISTS sale_details (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER, product_id INTEGER, quantity INTEGER, price REAL, total REAL, FOREIGN KEY(sale_id) REFERENCES sales(id), FOREIGN KEY(product_id) REFERENCES products(id))");
        
        stmt.execute("CREATE TABLE IF NOT EXISTS installments (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER, customer_id INTEGER, amount REAL, due_date TEXT, payment_date TEXT, is_paid INTEGER DEFAULT 0, FOREIGN KEY(sale_id) REFERENCES sales(id), FOREIGN KEY(customer_id) REFERENCES customers(id))");
        stmt.execute("CREATE TABLE IF NOT EXISTS inventory_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, product_id INTEGER, change_amount INTEGER, type TEXT, reason TEXT, log_date TEXT, FOREIGN KEY(product_id) REFERENCES products(id))");

        System.out.println("تم إنشاء الجداول بالهيكل الجديد (شاملاً paid_amount).");
    } catch (SQLException e) {
        System.err.println("خطأ في إنشاء الجداول: " + e.getMessage());
    }
}

    public List<Customer> getAllCustomersForPOS() {
    List<Customer> list = new ArrayList<>() ;
    // --- بداية التعديل ---
    // تم إضافة شرط "WHERE status = 1" هنا أيضًا
    String sql = "SELECT id, name, balance FROM customers WHERE status = 1 ORDER BY name ASC" ;
    // --- نهاية التعديل ---
    try (Statement stmt = connection.createStatement() ;
    ResultSet rs = stmt.executeQuery(sql)) {
        while (rs.next()) {
            list.add(new Customer( rs.getInt("id"), rs.getString("name"), rs.getDouble("balance") )) ;
        }
    }
    catch (SQLException e) {
        System.err.println("Error fetching customers for POS: " + e.getMessage()) ;
    }
    return list ;
}
    
    public List<Installment> getAllInstallments() {
        List<Installment> list = new ArrayList<>();
        // استخدام أسماء الأعمدة الصريحة لتجنب تداخل البيانات
        String sql = "SELECT i.id, i.sale_id, c.name as customer_name, i.amount, i.due_date, i.payment_date, i.is_paid " +
                     "FROM installments i " +
                     "JOIN customers c ON i.customer_id = c.id " +
                     "ORDER BY i.due_date ASC";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Installment(
                    rs.getInt("id"),
                    rs.getInt("sale_id"),
                    rs.getString("customer_name"),
                    rs.getDouble("amount"),
                    rs.getString("due_date"), // هنا كان الخطأ، الآن سيقرأ التاريخ بشكل صحيح
                    rs.getString("payment_date"),
                    rs.getInt("is_paid") == 1
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching installments: " + e.getMessage());
        }
        return list;
    }

    public static class BusinessInfo {
        public String name;
        public String slogan;
        public String phone1;
        public String phone2;
        public String salesman; // الحقل الجديد
        public byte[] logoBytes;

        // الكونستركتور المحدث
        public BusinessInfo(String name, String slogan, String phone1, String phone2, String salesman, byte[] logoBytes) {
            this.name = name;
            this.slogan = slogan;
            this.phone1 = phone1;
            this.phone2 = phone2;
            this.salesman = salesman; // تهيئة المتغير الجديد
            this.logoBytes = logoBytes;
        }
    }

    public boolean saveBusinessInfo(String name, String slogan, String phone1, String phone2, String salesman, byte[] logoBytes) {
        // تم إضافة salesman في أسماء الأعمدة والقيم
        String sql = "REPLACE INTO app_settings (id, company_name, slogan, phone1, phone2, salesman, logo) VALUES (1, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, slogan);
            pstmt.setString(3, phone1);
            pstmt.setString(4, phone2);
            pstmt.setString(5, salesman); // الآن المتغير ممرر وصحيح
            pstmt.setBytes(6, logoBytes);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public BusinessInfo getBusinessInfo() {
        String sql = "SELECT * FROM app_settings WHERE id = 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new BusinessInfo(
                    rs.getString("company_name"),
                    rs.getString("slogan"),
                    rs.getString("phone1"),
                    rs.getString("phone2"),
                    rs.getString("salesman"), // قراءة العمود الجديد
                    rs.getBytes("logo")
                );
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return null;
    }

   public boolean isInstallmentSale(int saleId) {
    try (PreparedStatement pstmt = connection.prepareStatement("SELECT 1 FROM installments WHERE sale_id = ? LIMIT 1")) {
        pstmt.setInt(1, saleId);
        try (ResultSet rs = pstmt.executeQuery()) { return rs.next(); }
    } catch (SQLException e) { return false; }
}

    public double getSaleDownPayment(int saleId) {
    try (PreparedStatement pstmt = connection.prepareStatement("SELECT amount_paid FROM Sales WHERE id = ?")) {
        pstmt.setInt(1, saleId);
        try (ResultSet rs = pstmt.executeQuery()) { if(rs.next()) return rs.getDouble(1); }
    } catch (SQLException e) { }
    return 0.0;
}

   public List<Installment> getInstallmentsBySaleId(int saleId) {
    List<Installment> list = new ArrayList<>();
    String sql = "SELECT id, sale_id, customer_id, amount, due_date, payment_date, is_paid FROM installments WHERE sale_id = ? ORDER BY due_date";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setInt(1, saleId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            list.add(new Installment(
                rs.getInt("id"), rs.getInt("sale_id"), "", // الاسم غير مهم هنا
                rs.getDouble("amount"), rs.getString("due_date"), 
                rs.getString("payment_date"), rs.getInt("is_paid") == 1
            ));
        }
    } catch (SQLException e) { e.printStackTrace(); }
    return list;
}

}