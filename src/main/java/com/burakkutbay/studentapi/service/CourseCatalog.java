package com.burakkutbay.studentapi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.burakkutbay.studentapi.model.Course;

/**
 * Sabit ders kataloğu.
 */
public final class CourseCatalog {

    private static final Map COURSES = new HashMap();

    static {
        register(new Course("CENG101", "Programlamaya Giriş", 4, "Bilgisayar Mühendisliği"));
        register(new Course("CENG202", "Veri Yapıları", 4, "Bilgisayar Mühendisliği"));
        register(new Course("CENG305", "Veritabanı Sistemleri", 3, "Bilgisayar Mühendisliği"));
        register(new Course("CENG350", "Yazılım Mühendisliği", 3, "Bilgisayar Mühendisliği"));
        register(new Course("MATH101", "Analiz I", 5, "Matematik"));
        register(new Course("MATH201", "Lineer Cebir", 3, "Matematik"));
        register(new Course("PHYS101", "Fizik I", 4, "Fizik"));
        register(new Course("PHYS210", "Kuantum Fiziğine Giriş", 3, "Fizik"));
    }

    private CourseCatalog() {
    }

    private static void register(Course course) {
        COURSES.put(course.getCode(), course);
    }

    public static Course findByCode(String code) {
        if (code == null) {
            return null;
        }
        return (Course) COURSES.get(code.trim().toUpperCase());
    }

    public static List findAll() {
        List list = new ArrayList(COURSES.values());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                Course c1 = (Course) o1;
                Course c2 = (Course) o2;
                return c1.getCode().compareTo(c2.getCode());
            }
        });
        return Collections.unmodifiableList(list);
    }
}
