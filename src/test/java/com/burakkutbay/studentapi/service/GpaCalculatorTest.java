package com.burakkutbay.studentapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.burakkutbay.studentapi.model.Enrollment;
import com.burakkutbay.studentapi.model.LetterGrade;

class GpaCalculatorTest {

    private final GpaCalculator calculator = new GpaCalculator();

    @ParameterizedTest
    @CsvSource({"AA, 4.0", "ba, 3.5", "' DD ', 1.0", "FF, 0.0"})
    void gradePointsFollowTurkishScale(String letter, double expected) {
        assertEquals(expected, LetterGrade.parse(letter).orElseThrow().points(), 0.0001);
    }

    @Test
    void unknownGradeIsRejected() {
        assertTrue(LetterGrade.parse("A+").isEmpty());
    }

    @Test
    void gpaIsCreditWeighted() {
        var enrollments = List.of(
                new Enrollment("CENG101", "2023-GUZ", LetterGrade.AA),   // 4 kredi
                new Enrollment("MATH101", "2023-GUZ", LetterGrade.CC),   // 5 kredi
                new Enrollment("MATH201", "2024-BAHAR", null));          // notsuz, sayılmaz
        // (4*4.0 + 5*2.0) / 9 = 2.888 -> 2.89
        assertEquals(2.89, calculator.calculate(enrollments).orElseThrow(), 0.0001);
        assertEquals(9, calculator.completedCredits(enrollments));
    }

    @Test
    void noGradesMeansNoGpa() {
        var gpa = calculator.calculate(List.of(new Enrollment("CENG101", "2023-GUZ", null)));
        assertTrue(gpa.isEmpty());
        assertEquals("Hesaplanamadı", calculator.honorLevel(gpa));
    }

    @Test
    void passingAndHonors() {
        assertTrue(LetterGrade.DD.isPassing());
        assertFalse(LetterGrade.FD.isPassing());
        assertEquals("Yüksek Onur", calculator.honorLevel(OptionalDouble.of(3.5)));
        assertEquals("Onur", calculator.honorLevel(OptionalDouble.of(3.2)));
        assertEquals("Normal", calculator.honorLevel(OptionalDouble.of(2.0)));
        assertEquals("Sınamalı", calculator.honorLevel(OptionalDouble.of(1.99)));
    }
}
