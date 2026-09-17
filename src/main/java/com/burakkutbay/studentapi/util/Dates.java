package com.burakkutbay.studentapi.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/// `yyyy-MM-dd` biçimli, katı (2003-02-30 gibi tarihleri reddeden) tarih ayrıştırma.
/// `DateTimeFormatter` değişmez ve thread-safe olduğundan senkronizasyon gerekmez.
public final class Dates {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private Dates() {
    }

    public static LocalDate parse(String value) {
        if (value == null) {
            throw new DateTimeParseException("Tarih boş", "", 0);
        }
        return LocalDate.parse(value.strip(), FORMAT);
    }
}
