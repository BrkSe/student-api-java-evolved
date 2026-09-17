package com.burakkutbay.studentapi.util;

/**
 * Uygulama genelinde benzersiz kimlik üretici (singleton).
 */
public class IdGenerator {

    private static IdGenerator instance;

    private long current = 0;

    private IdGenerator() {
    }

    public static IdGenerator getInstance() {
        if (instance == null) {
            synchronized (IdGenerator.class) {
                if (instance == null) {
                    instance = new IdGenerator();
                }
            }
        }
        return instance;
    }

    public synchronized Long nextId() {
        current++;
        return new Long(current);
    }

    public synchronized void ensureAbove(long value) {
        if (value > current) {
            current = value;
        }
    }

    public synchronized void reset() {
        current = 0;
    }

    public static String studentNumber(int year, Long id) {
        String number = String.valueOf(id.longValue());
        while (number.length() < 5) {
            number = "0" + number;
        }
        return String.valueOf(year) + number;
    }
}
