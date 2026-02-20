package nafzh.manager;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;


public class Updater {

    // ⚠️ مهم: إصدار التطبيق الحالي
    private static final String CURRENT_APP_VERSION = "1.0.1"; // <--- يجب أن يكون هذا رقم إصدار نسختك الحالية

    // ⚠️ مهم: اسم حسابك على GitHub
    private static final String GITHUB_REPO_OWNER = "esaydm27"; 
    
    // ⚠️ مهم: اسم المستودع الجديد (مطابق للرابط الذي أرسلته)
    private static final String GITHUB_REPO_NAME = "Nafzh-Manager"; // <--- تم التعديل هنا

    // ⚠️ مهم: اسم ملف JAR الخاص بتطبيقك (تأكد من مطابقته تماماً)
    private static final String APP_JAR_NAME = "NafzhManager.jar"; // <--- تأكد أن هذا هو اسم ملف JAR الذي يتم بناؤه

    // روابط ملفات التحديث (يتم توليدها)
    private static final String GITHUB_RAW_VERSION_URL = "https://raw.githubusercontent.com/" + GITHUB_REPO_OWNER + "/" + GITHUB_REPO_NAME + "/main/version.txt";
    private static final String GITHUB_DOWNLOAD_URL = "https://github.com/" + GITHUB_REPO_OWNER + "/" + GITHUB_REPO_NAME + "/releases/download/v" + "%s" + "/" + APP_JAR_NAME;

    private JFrame parentFrame; 

    public Updater(JFrame parent) { 
        this.parentFrame = parent;
    }

    // =========================================================================================
    // === 1. فحص التحديثات (اللوجيك الأساسي) ===
    // =========================================================================================

    // فحص إذا كان هناك تحديث متوفر
    public String checkForUpdate(boolean checkRemote) {
        String latestVersion = null;
        try {
            if (checkRemote) {
                // قراءة من GitHub
                latestVersion = readVersionFromUrl(GITHUB_RAW_VERSION_URL);
            } else {
                // قراءة من ملف محلي (يفترض أن يكون بجوار JAR)
                latestVersion = readVersionFromFile("version.txt");
            }

            if (latestVersion != null && !latestVersion.isEmpty()) {
                // مقارنة الإصدارات (افترض أن الإصدارات كلها x.y.z)
                if (compareVersions(latestVersion, CURRENT_APP_VERSION) > 0) {
                    return latestVersion; // يوجد إصدار أحدث
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentFrame, "خطأ في فحص التحديثات: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
        return null; // لا يوجد تحديث أو حدث خطأ
    }

    // جلب رقم الإصدار من ملف على الإنترنت
    private String readVersionFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return reader.readLine();
        } finally {
            connection.disconnect();
        }
    }

    // جلب رقم الإصدار من ملف محلي
    private String readVersionFromFile(String fileName) throws IOException {
        // يفضل أن يكون الملف بجوار JAR
        Path path = Paths.get(System.getProperty("user.dir"), fileName);
        if (Files.exists(path)) {
            return Files.readAllLines(path).get(0);
        }
        return null;
    }

    // مقارنة أرقام الإصدارات (V1 > V2 -> موجب، V1 < V2 -> سالب، V1 = V2 -> صفر)
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            int num2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;
            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }

    // =========================================================================================
    // === 2. تحميل الملف الجديد ===
    // =========================================================================================
    public boolean downloadUpdate(String latestVersion) {
        String downloadUrl = String.format(GITHUB_DOWNLOAD_URL, latestVersion);
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Path tempDir = Files.createTempDirectory("NafzhUpdater");
                Path newJarPath = Paths.get(tempDir.toString(), APP_JAR_NAME);

                try (InputStream in = connection.getInputStream();
                     OutputStream out = Files.newOutputStream(newJarPath)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                // تنفيذ سكريبت التحديث
                executeUpdaterScript(tempDir.toString(), newJarPath.toString());
                return true;
            } else {
                JOptionPane.showMessageDialog(parentFrame, "فشل تحميل التحديث. كود الاستجابة: " + responseCode, "خطأ", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentFrame, "خطأ في تحميل التحديث: " + e.getMessage(), "خطأ", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    // =========================================================================================
    // === 3. تنفيذ سكريبت التحديث ===
    // =========================================================================================
    private void executeUpdaterScript(String tempDirPath, String newJarPath) throws IOException {
        String currentJarPath = getRunningJarPath();
        if (currentJarPath == null) {
            JOptionPane.showMessageDialog(parentFrame, "لا يمكن تحديد مسار ملف JAR الحالي.", "خطأ", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String scriptContent = generateUpdateScript(currentJarPath, newJarPath);
        Path scriptPath = Paths.get(tempDirPath, "updater.bat"); // اسم السكريبت

        Files.write(scriptPath, scriptContent.getBytes()); // كتابة السكريبت

        // تشغيل السكريبت وإنهاء التطبيق الحالي
        Runtime.getRuntime().exec("cmd /c start \"" + scriptPath.toString() + "\" \"" + scriptPath.toString() + "\"");
        System.exit(0); // إنهاء التطبيق الحالي
    }

    // جلب المسار الحالي لملف JAR
    private String getRunningJarPath() {
        try {
            // هذا يعطي مسار JAR الحالي
            String path = Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            // أحياناً يكون Path مشفراً (مثلاً بمسافات %20)، يجب فك التشفير
            path = java.net.URLDecoder.decode(path, "UTF-8");
            // حذف "file:/" أو "file:/C:" إذا كانت موجودة
            if (path.startsWith("/")) path = path.substring(1); // For Windows, remove leading /
            return path;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================================================
    // === 4. محتوى سكريبت التحديث (updater.bat) ===
    // =========================================================================================
    private String generateUpdateScript(String currentJarPath, String newJarPath) {
        Path currentDir = Paths.get(currentJarPath).getParent(); // مجلد التطبيق الحالي
        String currentAppName = Paths.get(currentJarPath).getFileName().toString(); // اسم JAR الحالي (NafzhManager.jar)

        return "@echo off\n" +
               "CHCP 65001\n" + // لدعم اللغة العربية في CMD
               "echo. Nafzh Manager Updater - جاري تحديث التطبيق...\n" +
               "echo. الرجاء عدم إغلاق هذه النافذة.\n" +
               "timeout /t 5 >nul\n" + // انتظار 5 ثواني لضمان إغلاق التطبيق الحالي
               "echo. جارٍ استبدال الملف القديم...\n" +
               "del \"" + currentJarPath + "\"\n" + // حذف JAR القديم
               "move \"" + newJarPath + "\" \"" + currentJarPath + "\"\n" + // نقل JAR الجديد
               "echo. تم التحديث بنجاح! جاري إعادة تشغيل التطبيق...\n" +
               "start \"Nafzh Manager\" javaw -jar \"" + currentJarPath + "\"\n" + // إعادة تشغيل التطبيق
               "del \"%~f0\"\n" + // حذف سكريبت التحديث نفسه
               "rmdir /s /q \"" + Paths.get(newJarPath).getParent().toString() + "\"\n" + // حذف المجلد المؤقت
               "exit";
    }
}
