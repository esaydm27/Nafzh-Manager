package nafzh.manager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.prefs.Preferences;

public class LicenseManager {

    private static final String SECRET_APP_KEY = "MOHAMED_INVENTORY_SYSTEM_2025_SECURE";
    private static final String APP_ID = "APP_MLO7%@FF";
    // نستخدم MD5(APP_ID) لضمان أن أسماء مفاتيح الريجستري صالحة دائماً
    private static final String SAFE_ID = getHash(APP_ID).substring(0, 8);

    private static final String PREF_FIRST_RUN = "install_date_" + SAFE_ID;
    private static final String PREF_LAST_RUN = "last_run_date_" + SAFE_ID;
    private static final String PREF_LICENSE_KEY = "activation_key_" + SAFE_ID;
    private final Preferences prefs;

    public enum LicenseStatus { TRIAL, EXPIRED, ACTIVATED, TAMPERED }

    public LicenseManager() {
        prefs = Preferences.userNodeForPackage(this.getClass());
    }

    public LicenseStatus checkLicense() {
        String savedKey = prefs.get(PREF_LICENSE_KEY, "");
        String machineID = getMachineID();
        if (generateActivationKey(machineID).equals(savedKey)) return LicenseStatus.ACTIVATED;

        String firstRunStr = prefs.get(PREF_FIRST_RUN, "");
        String lastRunStr = prefs.get(PREF_LAST_RUN, "");
        LocalDate today = LocalDate.now();

        if (firstRunStr.isEmpty()) {
            prefs.put(PREF_FIRST_RUN, today.toString());
            prefs.put(PREF_LAST_RUN, today.toString());
            return LicenseStatus.TRIAL;
        }

        try {
            LocalDate lastRunDate = LocalDate.parse(lastRunStr);
            if (today.isBefore(lastRunDate)) return LicenseStatus.TAMPERED;
            prefs.put(PREF_LAST_RUN, today.toString());
            long daysBetween = ChronoUnit.DAYS.between(LocalDate.parse(firstRunStr), today);
            return (daysBetween > 7) ? LicenseStatus.EXPIRED : LicenseStatus.TRIAL;
        } catch (Exception e) { return LicenseStatus.TAMPERED; }
    }

    public long getTrialDaysRemaining() {
        String firstRunStr = prefs.get(PREF_FIRST_RUN, LocalDate.now().toString());
        long daysUsed = ChronoUnit.DAYS.between(LocalDate.parse(firstRunStr), LocalDate.now());
        return Math.max(0, 7 - daysUsed);
    }

    public boolean activate(String inputKey) {
        String machineID = getMachineID();
        String expectedKey = generateActivationKey(machineID);
        if (expectedKey.equals(inputKey.trim())) {
            prefs.put(PREF_LICENSE_KEY, expectedKey);
            return true;
        }
        return false;
    }

    public String getRequestCode() { return getMachineID().toUpperCase(); }

    private String getMachineID() {
        try {
            String cpu = getCommandOutput("wmic cpu get processorid");
            String motherboard = getCommandOutput("wmic baseboard get serialnumber");
            return getHash(cpu.trim() + motherboard.trim()).substring(0, 16);
        } catch (Exception e) { return "GENERIC-ID-ERROR"; }
    }

    private static String generateActivationKey(String machineID) {
        try {
            String raw = machineID.toUpperCase() + SECRET_APP_KEY + APP_ID;
            return getHash(raw).substring(0, 12).toUpperCase();
        } catch (Exception e) { return ""; }
    }

    private String getCommandOutput(String command) {
        try {
            String[] cmdArray = command.split("\\s+");
            ProcessBuilder builder = new ProcessBuilder(cmdArray);
            Process p = builder.start();
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line; StringBuilder result = new StringBuilder();
            while ((line = input.readLine()) != null) {
                if (!line.isEmpty() && !line.toLowerCase().contains("serialnumber") && !line.toLowerCase().contains("processorid"))
                    result.append(line.trim());
            }
            input.close(); return result.toString();
        } catch (Exception e) { return "UNKNOWN"; }
    }

    private static String getHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString().toUpperCase();
        } catch (Exception e) { return ""; }
    }
}