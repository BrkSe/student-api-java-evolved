package com.burakkutbay.studentapi.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.burakkutbay.studentapi.model.Enrollment;

public class GpaCalculatorTest {

    private GpaCalculator calculator;

    @Before
    public void setUp() {
        calculator = new GpaCalculator();
    }

    @Test
    public void gradePointsFollowTurkishScale() {
        assertEquals(4.0, calculator.gradePoint("AA"), 0.0001);
        assertEquals(3.5, calculator.gradePoint("ba"), 0.0001);
        assertEquals(1.0, calculator.gradePoint(" DD "), 0.0001);
        assertEquals(0.0, calculator.gradePoint("FF"), 0.0001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownGradeIsRejected() {
        calculator.gradePoint("A+");
    }

    @Test
    public void gpaIsCreditWeighted() {
        List enrollments = new ArrayList();
        enrollments.add(new Enrollment("CENG101", "2023-GUZ", "AA")); // 4 kredi
        enrollments.add(new Enrollment("MATH101", "2023-GUZ", "CC")); // 5 kredi
        enrollments.add(new Enrollment("MATH201", "2024-BAHAR", null)); // notsuz, sayılmaz
        // (4*4.0 + 5*2.0) / 9 = 2.888 -> 2.89
        assertEquals(2.89, calculator.calculate(enrollments), 0.0001);
        assertEquals(9, calculator.completedCredits(enrollments));
    }

    @Test
    public void noGradesMeansNoGpa() {
        List enrollments = new ArrayList();
        enrollments.add(new Enrollment("CENG101", "2023-GUZ", null));
        assertEquals(GpaCalculator.NO_GPA, calculator.calculate(enrollments), 0.0001);
        assertEquals("Hesaplanamadı", calculator.honorLevel(GpaCalculator.NO_GPA));
    }

    @Test
    public void passingAndHonors() {
        assertTrue(calculator.isPassing("DD"));
        assertFalse(calculator.isPassing("FD"));
        assertEquals("Yüksek Onur", calculator.honorLevel(3.5));
        assertEquals("Onur", calculator.honorLevel(3.2));
        assertEquals("Normal", calculator.honorLevel(2.0));
        assertEquals("Sınamalı", calculator.honorLevel(1.99));
    }
}
