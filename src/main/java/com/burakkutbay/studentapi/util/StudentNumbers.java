package com.burakkutbay.studentapi.util;

import java.time.Year;
import java.util.Locale;

public final class StudentNumbers {

    private StudentNumbers() {
    }

    /// Kayıt yılı + en az 5 haneye tamamlanmış kimlik: `2026` + `9` → `202600009`.
    public static String of(Year year, long id) {
        return String.format(Locale.ROOT, "%d%05d", year.getValue(), id);
    }
}
