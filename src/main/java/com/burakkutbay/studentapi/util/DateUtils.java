package com.burakkutbay.studentapi.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class DateUtils {

    public static final String PATTERN = "yyyy-MM-dd";

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat(PATTERN);

    static {
        FORMAT.setLenient(false);
    }

    private DateUtils() {
    }

    public static synchronized Date parse(String value) throws ParseException {
        if (value == null) {
            throw new ParseException("Tarih boş", 0);
        }
        String trimmed = value.trim();
        Date date = FORMAT.parse(trimmed);
        // SimpleDateFormat sondaki fazlalıkları sessizce yutar, bu yüzden geri formatlayıp karşılaştırıyoruz
        if (!FORMAT.format(date).equals(trimmed)) {
            throw new ParseException("Geçersiz tarih: " + value, 0);
        }
        return date;
    }

    public static synchronized String format(Date date) {
        if (date == null) {
            return null;
        }
        return FORMAT.format(date);
    }

    public static int calculateAge(Date birthDate) {
        return calculateAge(birthDate, new Date());
    }

    public static int calculateAge(Date birthDate, Date now) {
        Calendar birth = Calendar.getInstance();
        birth.setTime(birthDate);
        Calendar today = Calendar.getInstance();
        today.setTime(now);
        int age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        int monthDiff = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH);
        if (monthDiff < 0 || (monthDiff == 0 && today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }
        return age;
    }

    public static int currentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }
}
