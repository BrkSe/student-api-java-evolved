package com.burakkutbay.studentapi.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/// Türk üniversitelerinde kullanılan 4'lük harf notu sistemi.
public enum LetterGrade {
    AA(4.0), BA(3.5), BB(3.0), CB(2.5), CC(2.0), DC(1.5), DD(1.0), FD(0.5), FF(0.0);

    private final double points;

    LetterGrade(double points) {
        this.points = points;
    }

    public double points() {
        return points;
    }

    public boolean isPassing() {
        return points >= 1.0;
    }

    public static Optional<LetterGrade> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        var normalized = value.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(grade -> grade.name().equals(normalized))
                .findFirst();
    }
}
