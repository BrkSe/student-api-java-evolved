package com.burakkutbay.studentapi.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;

import com.burakkutbay.studentapi.json.JsonSerializable;

/// Değişmez öğrenci kaydı. Güncellemeler `with…` metotlarıyla yeni bir örnek üretir.
public record Student(
        Long id,
        String studentNumber,
        String firstName,
        String lastName,
        String email,
        LocalDate birthDate,
        String department,
        StudentStatus status,
        List<Enrollment> enrollments) implements JsonSerializable {

    public Student {
        Objects.requireNonNull(birthDate, "birthDate");
        Objects.requireNonNull(status, "status");
        enrollments = List.copyOf(enrollments);
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public int age(LocalDate today) {
        return Period.between(birthDate, today).getYears();
    }

    public Student withId(long newId) {
        return new Student(newId, studentNumber, firstName, lastName, email, birthDate, department, status, enrollments);
    }

    public Student withStudentNumber(String number) {
        return new Student(id, number, firstName, lastName, email, birthDate, department, status, enrollments);
    }

    public Student withEnrollments(List<Enrollment> newEnrollments) {
        return new Student(id, studentNumber, firstName, lastName, email, birthDate, department, status, newEnrollments);
    }

    @Override
    public SequencedMap<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("studentNumber", studentNumber);
        map.put("firstName", firstName);
        map.put("lastName", lastName);
        map.put("fullName", fullName());
        map.put("email", email);
        map.put("birthDate", birthDate);
        map.put("age", age(LocalDate.now()));
        map.put("department", department);
        map.put("status", status.name());
        map.put("enrollments", enrollments);
        return map;
    }
}
