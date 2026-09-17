package com.burakkutbay.studentapi.util;

public final class EmailValidator {

    private EmailValidator() {
    }

    public static boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        return email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
