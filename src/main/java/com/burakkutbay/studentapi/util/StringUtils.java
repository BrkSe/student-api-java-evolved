package com.burakkutbay.studentapi.util;

import java.util.Locale;

public final class StringUtils {

    private static final Locale TURKISH = Locale.of("tr", "TR");

    private StringUtils() {
    }

    /// `null` güvenli `String.isBlank()`.
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /// Türkçe kurallarıyla baş harfi büyütür: `"ismail"` → `"İsmail"`.
    public static String capitalize(String s) {
        if (isBlank(s)) {
            return s;
        }
        var stripped = s.strip();
        return stripped.substring(0, 1).toUpperCase(TURKISH) + stripped.substring(1).toLowerCase(TURKISH);
    }
}
