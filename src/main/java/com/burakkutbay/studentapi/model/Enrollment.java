package com.burakkutbay.studentapi.model;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.SequencedMap;

import com.burakkutbay.studentapi.json.JsonSerializable;

/// Bir öğrencinin belirli bir dönemde aldığı ders. `letterGrade` henüz not girilmediyse `null`'dır.
public record Enrollment(String courseCode, String semester, LetterGrade letterGrade) implements JsonSerializable {

    public Enrollment {
        Objects.requireNonNull(courseCode, "courseCode");
        Objects.requireNonNull(semester, "semester");
    }

    public boolean isGraded() {
        return letterGrade != null;
    }

    public Enrollment withGrade(LetterGrade grade) {
        return new Enrollment(courseCode, semester, grade);
    }

    @Override
    public SequencedMap<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("courseCode", courseCode);
        map.put("semester", semester);
        map.put("letterGrade", letterGrade == null ? null : letterGrade.name());
        return map;
    }
}
