package com.burakkutbay.studentapi.service;

import java.util.Collection;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import com.burakkutbay.studentapi.model.Enrollment;

/// Kredi ağırlıklı genel not ortalaması (GNO) hesaplayıcı.
public final class GpaCalculator {

    private record WeightedGrade(double points, int credits) {
    }

    /// Notu girilmiş ve katalogda bulunan derslerin kredi ağırlıklı ortalaması;
    /// hiç not yoksa boş döner.
    public OptionalDouble calculate(Collection<Enrollment> enrollments) {
        return enrollments.stream()
                .filter(Enrollment::isGraded)
                .<WeightedGrade>mapMulti((enrollment, sink) -> CourseCatalog.findByCode(enrollment.courseCode())
                        .ifPresent(course -> sink.accept(new WeightedGrade(
                                enrollment.letterGrade().points() * course.credits(), course.credits()))))
                .collect(Collectors.teeing(
                        Collectors.summingDouble(WeightedGrade::points),
                        Collectors.summingInt(WeightedGrade::credits),
                        (points, credits) -> credits == 0
                                ? OptionalDouble.empty()
                                : OptionalDouble.of(round(points / credits))));
    }

    public int completedCredits(Collection<Enrollment> enrollments) {
        return enrollments.stream()
                .filter(e -> e.isGraded() && e.letterGrade().isPassing())
                .flatMap(e -> CourseCatalog.findByCode(e.courseCode()).stream())
                .mapToInt(course -> course.credits())
                .sum();
    }

    public String honorLevel(OptionalDouble gpa) {
        if (gpa.isEmpty()) {
            return "Hesaplanamadı";
        }
        double value = gpa.getAsDouble();
        if (value >= 3.5) {
            return "Yüksek Onur";
        } else if (value >= 3.0) {
            return "Onur";
        } else if (value >= 2.0) {
            return "Normal";
        }
        return "Sınamalı";
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
