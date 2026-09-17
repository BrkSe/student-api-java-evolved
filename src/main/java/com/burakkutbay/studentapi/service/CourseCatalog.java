package com.burakkutbay.studentapi.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.burakkutbay.studentapi.model.Course;

/// Sabit, değişmez ders kataloğu.
public final class CourseCatalog {

    private static final List<Course> ALL = Stream.of(
                    new Course("CENG101", "Programlamaya Giriş", 4, "Bilgisayar Mühendisliği"),
                    new Course("CENG202", "Veri Yapıları", 4, "Bilgisayar Mühendisliği"),
                    new Course("CENG305", "Veritabanı Sistemleri", 3, "Bilgisayar Mühendisliği"),
                    new Course("CENG350", "Yazılım Mühendisliği", 3, "Bilgisayar Mühendisliği"),
                    new Course("MATH101", "Analiz I", 5, "Matematik"),
                    new Course("MATH201", "Lineer Cebir", 3, "Matematik"),
                    new Course("PHYS101", "Fizik I", 4, "Fizik"),
                    new Course("PHYS210", "Kuantum Fiziğine Giriş", 3, "Fizik"))
            .sorted(Comparator.comparing(Course::code))
            .toList();

    private static final Map<String, Course> BY_CODE = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(Course::code, Function.identity()));

    private CourseCatalog() {
    }

    public static Optional<Course> findByCode(String code) {
        return Optional.ofNullable(code)
                .map(c -> c.strip().toUpperCase(Locale.ROOT))
                .map(BY_CODE::get);
    }

    public static List<Course> findAll() {
        return ALL;
    }
}
