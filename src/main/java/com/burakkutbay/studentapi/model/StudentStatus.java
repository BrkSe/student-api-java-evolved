package com.burakkutbay.studentapi.model;

import java.util.Arrays;
import java.util.Optional;

/// Öğrencinin kayıt durumu.
public enum StudentStatus {
    ACTIVE,
    GRADUATED,
    SUSPENDED,
    WITHDRAWN;

    /// Büyük/küçük harf duyarsız eşleştirme; bilinmeyen etiket için boş döner.
    public static Optional<StudentStatus> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        var normalized = label.strip();
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
