package nafzh.manager;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * A utility class to convert numbers into Arabic text (Tafqeet).
 * Supports positive numbers with decimals (e.g., for currency).
 */
public class Tafqeet {

    private static final String[] ones = {
        "", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة",
        "عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر"
    };

    private static final String[] tens = {
        "", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون"
    };

    private static final String[] hundreds = {
        "", "مائة", "مئتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة"
    };

    private static final String[] thousands = {
        "ألف", "آلاف", "مليون", "ملايين", "مليار", "مليارات"
    };
    
    private static String convertLessThanOneThousand(int number) {
        if (number == 0) return "";

        if (number < 20) {
            return ones[number];
        }

        if (number < 100) {
            return ones[number % 10] + (number % 10 != 0 ? " و" : "") + tens[number / 10];
        }

        if (number < 1000) {
            String remainder = convertLessThanOneThousand(number % 100);
            return hundreds[number / 100] + (!remainder.isEmpty() ? " و" : "") + remainder;
        }

        return "";
    }
    
    private static String convert(long number) {
        if (number == 0) {
            return "صفر";
        }

        String result = "";
        int i = 0;

        while (number > 0) {
            long remainder = number % 1000;
            if (remainder != 0) {
                String part = convertLessThanOneThousand((int) remainder);
                if (i > 0) {
                    if (remainder == 1) {
                        part = thousands[i * 2 - 2];
                    } else if (remainder == 2) {
                        part = thousands[i * 2 - 2].equals("ألف") ? "ألفان" : thousands[i * 2 - 2] + "ان";
                    } else if (remainder >= 3 && remainder <= 10) {
                        part += " " + thousands[i * 2 - 1];
                    } else {
                        part += " " + thousands[i * 2 - 2];
                    }
                }
                if (!result.isEmpty()) {
                    result = part + " و" + result;
                } else {
                    result = part;
                }
            }
            number /= 1000;
            i++;
        }
        return result;
    }

    /**
     * Converts a BigDecimal number to its Arabic text representation.
     * @param number The number to convert.
     * @param currency The main currency name (e.g., "جنيه").
     * @param subCurrency The sub-currency name (e.g., "قرش").
     * @return The Arabic text representation.
     */
    public static String convertToWords(BigDecimal number, String currency, String subCurrency) {
        if (number == null || number.compareTo(BigDecimal.ZERO) < 0) {
            return "مبلغ غير صحيح";
        }
        if (number.compareTo(BigDecimal.ZERO) == 0) {
            return "صفر " + currency;
        }

        long integerPart = number.longValue();
        int fractionalPart = number.subtract(new BigDecimal(integerPart)).multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).intValue();

        String integerText = convert(integerPart);
        String fractionalText = convert(fractionalPart);

        StringBuilder result = new StringBuilder();
        result.append("فقط ");
        result.append(integerText);
        result.append(" ");
        result.append(currency);

        if (integerPart > 2 && integerPart < 11) {
             result.append("ات"); // جنيه -> جنيهات
        }

        if (fractionalPart > 0) {
            result.append(" و");
            result.append(fractionalText);
            result.append(" ");
            result.append(subCurrency);
            if(fractionalPart > 2 && fractionalPart < 11){
                result.append("ات");
            }
        }
        
        result.append(" لا غير");
        return result.toString().replaceAll("\\s+", " ").trim();
    }
}
