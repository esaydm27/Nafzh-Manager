package nafzh.manager;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Updater {

    private static final String CURRENT_APP_VERSION = "1.0.0"; 
    private static final String GITHUB_REPO_OWNER = "esaydm27"; 
    private static final String GITHUB_REPO_NAME = "Nafzh-Manager"; 
    private static final String APP_JAR_NAME = "Nafzh_Manager.jar"; 

    private static final String GITHUB_RAW_VERSION_URL = "https://raw.githubusercontent.com/" + GITHUB_REPO_OWNER + "/" + GITHUB_REPO_NAME + "/main/version.txt";
    private static final String GITHUB_DOWNLOAD_URL = "https://github.com/" + GITHUB_REPO_OWNER + "/" + GITHUB_REPO_NAME + "/releases/download/v" + "%s" + "/" + APP_JAR_NAME;

    private JFrame parentFrame; 

    public Updater(JFrame parent) { 
        this.parentFrame = parent;
    }

    private String getCurrentVersion() {
    try {
        Path path = Paths.get(System.getProperty("user.dir"), "version.txt");
        if (Files.exists(path)) {
            return Files.readAllLines(path).get(0).trim();
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
    return null; // إذا لم يوجد الملف أو حدث خطأ، ترجع null
}

public String checkForUpdate(boolean checkRemote) {
    System.out.println("[Updater] بدء فحص التحديثات...");
    String latestVersion = null;
    try {
        if (checkRemote) {
            latestVersion = readVersionFromUrl(GITHUB_RAW_VERSION_URL);
        } else {
            latestVersion = readVersionFromFile("version.txt");
        }

        if (latestVersion != null && !latestVersion.isEmpty()) {
            String currentVersion = getCurrentVersion();
            if (currentVersion != null && compareVersions(latestVersion, currentVersion) > 0) {
                return latestVersion;
            }
        }
    } catch (IOException e) {
        System.err.println("[Updater] خطأ أثناء الفحص: " + e.getMessage());
    }
    return null;
}


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

    private String readVersionFromFile(String fileName) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"), fileName);
        if (Files.exists(path)) {
            return Files.readAllLines(path).get(0).trim();
        }
        return null;
    }

    

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

    public boolean downloadUpdate(String latestVersion) {
        String downloadUrl = String.format(GITHUB_DOWNLOAD_URL, latestVersion);
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
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
                executeUpdaterScript(tempDir.toString(), newJarPath.toString(), latestVersion);
                return true;
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentFrame, "خطأ في تحميل التحديث: " + e.getMessage());
        }
        return false;
    }

    private void executeUpdaterScript(String tempDirPath, String newJarPath, String latestVersion) throws IOException {
        String currentJarPath = getRunningJarPath();

        if (currentJarPath == null) {
            JOptionPane.showMessageDialog(parentFrame, "لا يمكن تحديد مسار ملف JAR الحالي.", "خطأ", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // توليد السكربت مع تمرير رقم النسخة الجديدة
        String scriptContent = generateUpdateScript(latestVersion);
        Path scriptPath = Paths.get(tempDirPath, "updater.bat");
        Files.write(scriptPath, scriptContent.getBytes("UTF-8"));

        // تشغيل السكربت في نافذة جديدة مع تمرير المسارات
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "updater.bat", currentJarPath, newJarPath);
        pb.directory(new File(tempDirPath));
        pb.start();

        System.out.println("[Updater] تم تشغيل سكريبت التحديث بنجاح. سيتم إغلاق البرنامج الآن.");
        System.exit(0);
    }

    private String getRunningJarPath() {
        try {
            String path = Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            path = java.net.URLDecoder.decode(path, "UTF-8");
            
            // تصحيح مسار ويندوز إذا بدأ بـ /C:/
            if (path.startsWith("/") && path.contains(":")) {
                path = path.substring(1);
            }
            // توحيد الفواصل لتكون Backslashes
            return new File(path).getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String generateUpdateScript(String latestVersion) {
        return "@echo off\n" +
               "title Nafzh Manager - Update Console\n" +
               "SET \"OLD_JAR=%~1\"\n" +
               "SET \"NEW_JAR=%~2\"\n\n" +

               "echo. ===============================================\n" +
               "echo.            NAFZH MANAGER UPDATER\n" +
               "echo. ===============================================\n\n" +
               
               "echo. [1] Waiting for application to close...\n" +
               "timeout /t 5 /nobreak\n\n" +

               "echo. [2] Killing remaining Java processes...\n" +
               "taskkill /F /IM javaw.exe /T >nul 2>&1\n\n" +

               "echo. [3] Replacing old file with new one...\n" +
               "echo. FROM: %NEW_JAR%\n" +
               "echo. TO:   %OLD_JAR%\n" +
               "copy /Y \"%NEW_JAR%\" \"%OLD_JAR%\"\n" +
               "if errorlevel 1 (\n" +
               "    echo. -----------------------------------------------\n" +
               "    echo. [ERROR] Failed to replace the file! \n" +
               "    echo. Please check permissions or if the file is locked.\n" +
               "    echo. -----------------------------------------------\n" +
               "    pause\n" +
               "    exit\n" +
               ")\n\n" +

               "echo. [3.5] Writing new version to version.txt...\n" +
               "echo " + latestVersion + " > \"%~dp0version.txt\"\n\n" +

               "echo. [4] Update Successful!\n" +
               "echo. Restarting the application...\n" +
               "start \"\" javaw -jar \"%OLD_JAR%\"\n\n" +

               "echo. ===============================================\n" +
               "echo. DONE. Press any key to close this window.\n" +
               "echo. ===============================================\n" +
               "pause\n";
    }
}
