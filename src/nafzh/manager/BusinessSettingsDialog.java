package nafzh.manager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.awt.FileDialog;
import static nafzh.manager.NafzhManager.getCairoFont;

public class BusinessSettingsDialog extends JDialog {

    private final DatabaseManager dbManager;
    
    // --- هنا كان سبب المشكلة: يجب تعريف salesmanField مع باقي الحقول ---
    private JTextField nameField, sloganField, phone1Field, phone2Field, salesmanField;
    // -------------------------------------------------------------------
    
    private JLabel logoPreviewLabel;
    private byte[] currentLogoBytes = null;
    private boolean dataSaved = false;

    public BusinessSettingsDialog(Frame parent, DatabaseManager dbManager) {
        super(parent, "إعدادات بيانات المؤسسة", true);
        this.dbManager = dbManager;
        initializeUI();
        loadCurrentData();
    }

    private void initializeUI() {
        setSize(500, 600);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout());
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // 1. اسم المؤسسة
        mainPanel.add(createLabel("اسم المؤسسة:"));
        nameField = createField();
        mainPanel.add(nameField);
        mainPanel.add(Box.createVerticalStrut(10));

        // 2. الشعار
        mainPanel.add(createLabel("وصف مختصر (شعار):"));
        sloganField = createField();
        mainPanel.add(sloganField);
        mainPanel.add(Box.createVerticalStrut(10));

        // 3. الهواتف
        mainPanel.add(createLabel("رقم الهاتف الأول:"));
        phone1Field = createField();
        mainPanel.add(phone1Field);
        mainPanel.add(Box.createVerticalStrut(10));

        mainPanel.add(createLabel("رقم الهاتف الثاني:"));
        phone2Field = createField();
        mainPanel.add(phone2Field);
        mainPanel.add(Box.createVerticalStrut(10));

        // 4. اسم المندوب
        mainPanel.add(createLabel("اسم المسئول / المندوب (الافتراضي):"));
        salesmanField = createField(); // الآن سيتم إنشاؤه كـ JTextField
        mainPanel.add(salesmanField);  // ولن يعطي خطأ هنا
        mainPanel.add(Box.createVerticalStrut(20));

        // 5. قسم اللوجو
        mainPanel.add(createLabel("شعار المؤسسة (Logo):"));
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logoPanel.setOpaque(false);
        
        JButton uploadBtn = new JButton("اختر صورة");
        uploadBtn.setFont(getCairoFont(12f));
        uploadBtn.addActionListener(e -> chooseImage());
        
        logoPreviewLabel = new JLabel("لا توجد صورة");
        logoPreviewLabel.setPreferredSize(new Dimension(100, 100));
        logoPreviewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        logoPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);

        logoPanel.add(uploadBtn);
        logoPanel.add(logoPreviewLabel);
        mainPanel.add(logoPanel);

        add(mainPanel, BorderLayout.CENTER);

        // زر الحفظ
        JButton saveBtn = new JButton("حفظ البيانات");
        saveBtn.setFont(getCairoFont(14f));
        saveBtn.setBackground(new Color(46, 204, 113));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> saveData());
        
        JPanel btnPanel = new JPanel();
        btnPanel.add(saveBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void saveData() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "يجب كتابة اسم المؤسسة");
            return;
        }
        
        boolean success = dbManager.saveBusinessInfo(
            nameField.getText(),
            sloganField.getText(),
            phone1Field.getText(),
            phone2Field.getText(),
            salesmanField.getText(), // لن يعطي خطأ الآن
            currentLogoBytes
        );

        if (success) {
            JOptionPane.showMessageDialog(this, "تم الحفظ بنجاح! سيتم إعادة تشغيل التطبيق.");
            dataSaved = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "فشل الحفظ");
        }
    }

    private void loadCurrentData() {
        DatabaseManager.BusinessInfo info = dbManager.getBusinessInfo();
        if (info != null) {
            nameField.setText(info.name);
            sloganField.setText(info.slogan);
            phone1Field.setText(info.phone1);
            phone2Field.setText(info.phone2);
            
            if (info.salesman != null) {
                salesmanField.setText(info.salesman); // لن يعطي خطأ الآن
            }
            
            if (info.logoBytes != null) {
                currentLogoBytes = info.logoBytes;
                ImageIcon icon = new ImageIcon(info.logoBytes);
                Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                logoPreviewLabel.setIcon(new ImageIcon(img));
                logoPreviewLabel.setText("");
            }
        }
    }

        private void chooseImage() {
        // استخدام FileDialog بدلاً من JFileChooser للشكل الحديث
        FileDialog fileDialog = new FileDialog(this, "اختر صورة الشعار", FileDialog.LOAD);
        
        // محاولة تحديد الامتدادات (قد لا تظهر كفلتر صارم في بعض نسخ الويندوز لكنها تساعد)
        fileDialog.setFile("*.jpg;*.jpeg;*.png"); 
        
        fileDialog.setVisible(true); // إظهار النافذة

        // التحقق هل تم الاختيار
        if (fileDialog.getFile() != null) {
            File file = new File(fileDialog.getDirectory() + fileDialog.getFile());
            
            // التحقق من الامتداد يدوياً لضمان أنه صورة
            String name = file.getName().toLowerCase();
            if (!name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png")) {
                JOptionPane.showMessageDialog(this, "الرجاء اختيار ملف صورة صحيح (jpg, png)", "ملف غير مدعوم", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                currentLogoBytes = fis.readAllBytes();
                
                // عرض المعاينة
                ImageIcon icon = new ImageIcon(currentLogoBytes);
                Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                logoPreviewLabel.setIcon(new ImageIcon(img));
                logoPreviewLabel.setText(""); // إزالة النص "لا توجد صورة"
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "خطأ في قراءة الصورة: " + ex.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    // Helpers
    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(getCairoFont(13f));
        lbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        return lbl;
    }
    
    private JTextField createField() {
        JTextField f = new JTextField();
        f.setFont(getCairoFont(14f));
        f.setMaximumSize(new Dimension(500, 35));
        return f;
    }
    
    public boolean isDataSaved() { return dataSaved; }
}
