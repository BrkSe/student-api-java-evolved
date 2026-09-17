package com.burakkutbay.studentapi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List errors;

    public ValidationException(String message) {
        super(message);
        List list = new ArrayList();
        list.add(message);
        this.errors = Collections.unmodifiableList(list);
    }

    public ValidationException(List errors) {
        super("Doğrulama hatası");
        this.errors = Collections.unmodifiableList(new ArrayList(errors));
    }

    public List getErrors() {
        return errors;
    }
}
