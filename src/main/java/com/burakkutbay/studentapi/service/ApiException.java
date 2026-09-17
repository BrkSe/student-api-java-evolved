package com.burakkutbay.studentapi.service;

import java.util.List;

/// İş kuralı hatalarının kapalı hiyerarşisi. HTTP katmanı `switch` ile tüm alt tipleri
/// eksiksiz (exhaustive) işler; yeni bir alt tip eklendiğinde derleyici uyarır.
public sealed abstract class ApiException extends RuntimeException
        permits ApiException.Validation, ApiException.NotFound, ApiException.Conflict {

    private ApiException(String message) {
        super(message);
    }

    /// 400 — bir veya daha fazla doğrulama hatası.
    public static final class Validation extends ApiException {

        private final List<String> errors;

        public Validation(String message) {
            this.errors = List.of(message);
            super(message);
        }

        public Validation(List<String> errors) {
            var copy = List.copyOf(errors);
            if (copy.isEmpty()) {
                throw new IllegalArgumentException("En az bir doğrulama hatası gerekli");
            }
            this.errors = copy;
            super("Doğrulama hatası");
        }

        public List<String> errors() {
            return errors;
        }
    }

    /// 404 — kaynak bulunamadı.
    public static final class NotFound extends ApiException {

        public NotFound(String message) {
            super(message);
        }
    }

    /// 409 — istek mevcut durumla çelişiyor.
    public static final class Conflict extends ApiException {

        public Conflict(String message) {
            super(message);
        }
    }
}
