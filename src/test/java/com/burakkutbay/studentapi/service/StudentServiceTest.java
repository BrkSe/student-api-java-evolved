package com.burakkutbay.studentapi.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.burakkutbay.studentapi.model.LetterGrade;
import com.burakkutbay.studentapi.model.StudentStatus;
import com.burakkutbay.studentapi.notification.NotificationService;
import com.burakkutbay.studentapi.repository.InMemoryStudentRepository;
import com.burakkutbay.studentapi.service.ApiException.Conflict;
import com.burakkutbay.studentapi.service.ApiException.NotFound;
import com.burakkutbay.studentapi.service.ApiException.Validation;

class StudentServiceTest {

    private static final Clock FIXED = Clock.fixed(
            LocalDate.of(2026, 9, 17).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private StudentService service;

    @BeforeEach
    void setUp() {
        service = new StudentService(new InMemoryStudentRepository(), new GpaCalculator(), new NotificationService(), FIXED);
    }

    private static Map<String, Object> validBody(String email) {
        var body = new HashMap<String, Object>(Map.of(
                "firstName", "  ismail ",
                "lastName", "YILDIZ",
                "email", email,
                "birthDate", "2003-05-20",
                "department", "Bilgisayar Mühendisliği"));
        return body;
    }

    private static Map<String, Object> enrollment(String courseCode, String semester) {
        return Map.of("courseCode", courseCode, "semester", semester);
    }

    @Test
    void createsStudentWithNormalizedFields() {
        var student = service.createStudent(validBody("Ismail.Yildiz@Ogrenci.edu.tr"));
        assertAll(
                () -> assertEquals(1L, student.id()),
                () -> assertEquals("İsmail", student.firstName()),
                () -> assertEquals("Yıldız", student.lastName()),
                () -> assertEquals("ismail.yildiz@ogrenci.edu.tr", student.email()),
                () -> assertEquals(StudentStatus.ACTIVE, student.status()),
                () -> assertEquals("202600001", student.studentNumber()));
    }

    @Test
    void collectsAllValidationErrors() {
        var body = Map.<String, Object>of("email", "gecersiz", "birthDate", "2003-02-30", "status", "UZAYDA");
        var errors = assertThrows(Validation.class, () -> service.createStudent(body)).errors();
        assertEquals(6, errors.size());
        assertTrue(errors.containsAll(java.util.List.of(
                "firstName zorunludur", "email geçersiz", "birthDate yyyy-MM-dd formatında olmalıdır", "status geçersiz")));
    }

    @Test
    void rejectsDuplicateEmail() {
        service.createStudent(validBody("a@b.com"));
        assertThrows(Validation.class, () -> service.createStudent(validBody("A@B.com")));
    }

    @Test
    void enrollmentRulesAreEnforced() {
        long id = service.createStudent(validBody("a@b.com")).id();

        var enrolled = service.enroll(id, enrollment("ceng101", "2025-GUZ"));
        assertEquals("CENG101", enrolled.courseCode());
        assertNull(enrolled.letterGrade());

        var duplicate = assertThrows(Conflict.class, () -> service.enroll(id, enrollment("ceng101", "2025-GUZ")));
        assertTrue(duplicate.getMessage().contains("zaten kayıtlı"));

        // 4 + 5 + 4 + 3 + 3 = 19, sonraki 4 kredilik ders limiti aşar
        for (var course : new String[] {"MATH101", "PHYS101", "MATH201", "CENG305"}) {
            service.enroll(id, enrollment(course, "2025-GUZ"));
        }
        var tooMuch = assertThrows(Conflict.class, () -> service.enroll(id, enrollment("CENG202", "2025-GUZ")));
        assertEquals("Dönem kredi limiti aşıldı: 23 > 20", tooMuch.getMessage());
    }

    @Test
    void gradingUpdatesGpaAndBlocksDelete() {
        long id = service.createStudent(validBody("a@b.com")).id();
        service.enroll(id, enrollment("CENG101", "2025-GUZ"));

        var graded = service.gradeEnrollment(id, "ceng101", Map.of("letterGrade", "ba"));
        assertEquals(LetterGrade.BA, graded.letterGrade());

        var summary = service.gpaSummary(id);
        assertEquals(3.5, summary.get("gpa"));
        assertEquals("Yüksek Onur", summary.get("honor"));
        assertEquals(4, summary.get("completedCredits"));

        assertThrows(Conflict.class, () -> service.deleteStudent(id));
    }

    @Test
    void graduationRequiresGpa() {
        long id = service.createStudent(validBody("a@b.com")).id();
        var conflict = assertThrows(Conflict.class, () -> service.updateStudent(id, Map.of("status", "GRADUATED")));
        assertEquals("Mezuniyet için GNO en az 2.00 olmalıdır", conflict.getMessage());
    }

    @Test
    void failedUpdateDoesNotLeavePartialChanges() {
        long id = service.createStudent(validBody("a@b.com")).id();
        assertThrows(Conflict.class,
                () -> service.updateStudent(id, Map.of("lastName", "değişti", "status", "GRADUATED")));
        assertEquals("Yıldız", service.getStudent(id).lastName());
    }

    @Test
    void listsWithFilterAndSort() {
        service.createStudent(validBody("a@b.com"));
        var second = validBody("c@d.com");
        second.put("firstName", "Çağla");
        second.put("lastName", "Acar");
        second.put("department", "Matematik");
        service.createStudent(second);

        assertEquals("Acar", service.listStudents(null, null, "name").getFirst().lastName());
        assertEquals(1, service.listStudents("matematik", "active", null).size());
        assertThrows(Validation.class, () -> service.listStudents(null, null, "boy"));
    }

    @Test
    void statisticsAggregateAcrossStudents() {
        long id = service.createStudent(validBody("a@b.com")).id();
        service.enroll(id, enrollment("MATH101", "2025-GUZ"));
        service.gradeEnrollment(id, "MATH101", Map.of("letterGrade", "CC"));

        var stats = service.statistics();
        assertEquals(1, stats.get("totalStudents"));
        assertEquals(2.0, stats.get("averageGpa"));
        assertEquals(1, ((Map<?, ?>) stats.get("gradeDistribution")).get(LetterGrade.CC));
        assertEquals(1, ((Map<?, ?>) stats.get("courseEnrollments")).get("MATH101"));
    }

    @Test
    void missingStudentIsReported() {
        assertThrows(NotFound.class, () -> service.getStudent(42));
    }
}
