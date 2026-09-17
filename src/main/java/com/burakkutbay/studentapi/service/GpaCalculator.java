package com.burakkutbay.studentapi.service;

import java.util.Iterator;
import java.util.List;

import com.burakkutbay.studentapi.model.Course;
import com.burakkutbay.studentapi.model.Enrollment;

/**
 * Kredi ağırlıklı genel not ortalaması (GNO) hesaplayıcı.
 */
public class GpaCalculator {

    public static final double NO_GPA = -1.0;

    public static final String[] LETTER_GRADES = new String[] {
            "AA", "BA", "BB", "CB", "CC", "DC", "DD", "FD", "FF"
    };

    public double gradePoint(String letterGrade) {
        if (letterGrade == null) {
            throw new IllegalArgumentException("Harf notu boş olamaz");
        }
        String letter = letterGrade.trim().toUpperCase();
        if (letter.equals("AA")) {
            return 4.0;
        } else if (letter.equals("BA")) {
            return 3.5;
        } else if (letter.equals("BB")) {
            return 3.0;
        } else if (letter.equals("CB")) {
            return 2.5;
        } else if (letter.equals("CC")) {
            return 2.0;
        } else if (letter.equals("DC")) {
            return 1.5;
        } else if (letter.equals("DD")) {
            return 1.0;
        } else if (letter.equals("FD")) {
            return 0.5;
        } else if (letter.equals("FF")) {
            return 0.0;
        } else {
            throw new IllegalArgumentException("Geçersiz harf notu: " + letterGrade);
        }
    }

    public boolean isValidGrade(String letterGrade) {
        try {
            gradePoint(letterGrade);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isPassing(String letterGrade) {
        return gradePoint(letterGrade) >= 1.0;
    }

    public double calculate(List enrollments) {
        double totalPoints = 0.0;
        int totalCredits = 0;
        for (Iterator it = enrollments.iterator(); it.hasNext();) {
            Enrollment enrollment = (Enrollment) it.next();
            if (enrollment.getLetterGrade() == null) {
                continue;
            }
            Course course = CourseCatalog.findByCode(enrollment.getCourseCode());
            if (course == null) {
                continue;
            }
            totalPoints = totalPoints + gradePoint(enrollment.getLetterGrade()) * course.getCredits();
            totalCredits = totalCredits + course.getCredits();
        }
        if (totalCredits == 0) {
            return NO_GPA;
        }
        return round(totalPoints / totalCredits);
    }

    public int completedCredits(List enrollments) {
        int credits = 0;
        for (int i = 0; i < enrollments.size(); i++) {
            Enrollment enrollment = (Enrollment) enrollments.get(i);
            if (enrollment.getLetterGrade() != null && isPassing(enrollment.getLetterGrade())) {
                Course course = CourseCatalog.findByCode(enrollment.getCourseCode());
                if (course != null) {
                    credits += course.getCredits();
                }
            }
        }
        return credits;
    }

    public String honorLevel(double gpa) {
        if (gpa == NO_GPA) {
            return "Hesaplanamadı";
        }
        if (gpa >= 3.5) {
            return "Yüksek Onur";
        } else if (gpa >= 3.0) {
            return "Onur";
        } else if (gpa >= 2.0) {
            return "Normal";
        } else {
            return "Sınamalı";
        }
    }

    static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
