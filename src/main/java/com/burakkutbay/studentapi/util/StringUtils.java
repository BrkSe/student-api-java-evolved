package com.burakkutbay.studentapi.util;

import java.util.Locale;

public final class StringUtils {

    private static final Locale TURKISH = new Locale("tr", "TR");

    private StringUtils() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static String trimToNull(String s) {
        if (isBlank(s)) {
            return null;
        }
        return s.trim();
    }

    public static String repeat(String s, int count) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static String padRight(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        return s + repeat(" ", width - s.length());
    }

    /**
     * "ayşe" -> "Ayşe", "ismail" -> "İsmail"
     */
    public static String capitalize(String s) {
        if (isBlank(s)) {
            return s;
        }
        String trimmed = s.trim();
        return trimmed.substring(0, 1).toUpperCase(TURKISH) + trimmed.substring(1).toLowerCase(TURKISH);
    }
}
