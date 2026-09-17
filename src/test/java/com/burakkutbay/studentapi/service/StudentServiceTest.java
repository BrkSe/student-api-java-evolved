package com.burakkutbay.studentapi.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.burakkutbay.studentapi.model.Enrollment;
import com.burakkutbay.studentapi.model.Student;
import com.burakkutbay.studentapi.model.StudentStatus;
import com.burakkutbay.studentapi.notification.NotificationService;
import com.burakkutbay.studentapi.repository.InMemoryStudentRepository;
import com.burakkutbay.studentapi.util.IdGenerator;

public class StudentServiceTest {

    private StudentService service;

    @Before
    public void setUp() {
        IdGenerator.getInstance().reset();
        service = new StudentService(new InMemoryStudentRepository(), new GpaCalculator(), new NotificationService());
    }

    private Map validBody(String email) {
        Map body = new LinkedHashMap();
        body.put("firstName", "  ismail ");
        body.put("lastName", "YILDIZ");
        body.put("email", email);
        body.put("birthDate", "2003-05-20");
        body.put("department", "Bilgisayar Mühendisliği");
        return body;
    }

    private Map map(String key, Object value) {
        Map map = new LinkedHashMap();
        map.put(key, value);
        return map;
    }

    @Test
    public void createsStudentWithNormalizedFields() {
        Student student = service.createStudent(validBody("Ismail.Yildiz@Ogrenci.edu.tr"));
        assertEquals(new Long(1), student.getId());
        assertEquals("İsmail", student.getFirstName());
        assertEquals("Yıldız", student.getLastName());
        assertEquals("ismail.yildiz@ogrenci.edu.tr", student.getEmail());
        assertEquals(StudentStatus.ACTIVE, student.getStatus());
        assertTrue(student.getStudentNumber().endsWith("00001"));
    }

    @Test
    public void collectsAllValidationErrors() {
        Map body = new LinkedHashMap();
        body.put("email", "gecersiz");
        body.put("birthDate", "2003-02-30");
        body.put("status", "UZAYDA");
        try {
            service.createStudent(body);
            fail("ValidationException bekleniyordu");
        } catch (ValidationException e) {
            List errors = e.getErrors();
            assertEquals(6, errors.size());
            assertTrue(errors.contains("firstName zorunludur"));
            assertTrue(errors.contains("email geçersiz"));
            assertTrue(errors.contains("birthDate yyyy-MM-dd formatında olmalıdır"));
            assertTrue(errors.contains("status geçersiz"));
        }
    }

    @Test(expected = ValidationException.class)
    public void rejectsDuplicateEmail() {
        service.createStudent(validBody("a@b.com"));
        service.createStudent(validBody("A@B.com"));
    }

    @Test
    public void enrollmentRulesAreEnforced() {
        Student student = service.createStudent(validBody("a@b.com"));
        Long id = student.getId();

        Map enroll = new LinkedHashMap();
        enroll.put("courseCode", "ceng101");
        enroll.put("semester", "2025-GUZ");
        Enrollment enrollment = service.enroll(id, enroll);
        assertEquals("CENG101", enrollment.getCourseCode());
        assertNull(enrollment.getLetterGrade());

        try {
            service.enroll(id, enroll);
            fail("Aynı derse ikinci kayıt engellenmeli");
        } catch (ConflictException expected) {
            assertTrue(expected.getMessage().contains("zaten kayıtlı"));
        }

        // 4 + 5 + 4 + 3 + 3 = 19, sonraki 4 kredilik ders limiti aşar
        String[] courses = new String[] {"MATH101", "PHYS101", "MATH201", "CENG305"};
        for (int i = 0; i < courses.length; i++) {
            Map body = new LinkedHashMap();
            body.put("courseCode", courses[i]);
            body.put("semester", "2025-GUZ");
            service.enroll(id, body);
        }
        Map tooMuch = new LinkedHashMap();
        tooMuch.put("courseCode", "CENG202");
        tooMuch.put("semester", "2025-GUZ");
        try {
            service.enroll(id, tooMuch);
            fail("Kredi limiti aşılmamalı");
        } catch (ConflictException expected) {
            assertEquals("Dönem kredi limiti aşıldı: 23 > 20", expected.getMessage());
        }
    }

    @Test
    public void gradingUpdatesGpaAndBlocksDelete() {
        Long id = service.createStudent(validBody("a@b.com")).getId();
        Map enroll = new LinkedHashMap();
        enroll.put("courseCode", "CENG101");
        enroll.put("semester", "2025-GUZ");
        service.enroll(id, enroll);

        Enrollment graded = service.gradeEnrollment(id, "ceng101", map("letterGrade", "ba"));
        assertEquals("BA", graded.getLetterGrade());

        Map summary = service.gpaSummary(id);
        assertEquals(new Double(3.5), summary.get("gpa"));
        assertEquals("Yüksek Onur", summary.get("honor"));
        assertEquals(new Integer(4), summary.get("completedCredits"));

        try {
            service.deleteStudent(id);
            fail("Notlu öğrenci silinmemeli");
        } catch (ConflictException expected) {
            // beklenen
        }
    }

    @Test
    public void graduationRequiresGpa() {
        Long id = service.createStudent(validBody("a@b.com")).getId();
        try {
            service.updateStudent(id, map("status", "GRADUATED"));
            fail("GNO olmadan mezuniyet olmamalı");
        } catch (ConflictException expected) {
            assertEquals("Mezuniyet için GNO en az 2.00 olmalıdır", expected.getMessage());
        }
    }

    @Test
    public void listsWithFilterAndSort() {
        service.createStudent(validBody("a@b.com"));
        Map second = validBody("c@d.com");
        second.put("firstName", "Çağla");
        second.put("lastName", "Acar");
        second.put("department", "Matematik");
        service.createStudent(second);

        List byName = service.listStudents(null, null, "name");
        assertEquals("Acar", ((Student) byName.get(0)).getLastName());

        List math = service.listStudents("matematik", "active", null);
        assertEquals(1, math.size());

        try {
            service.listStudents(null, null, "boy");
            fail("Geçersiz sıralama reddedilmeli");
        } catch (ValidationException expected) {
            // beklenen
        }
    }

    @Test
    public void statisticsAggregateAcrossStudents() {
        Long id = service.createStudent(validBody("a@b.com")).getId();
        Map enroll = new LinkedHashMap();
        enroll.put("courseCode", "MATH101");
        enroll.put("semester", "2025-GUZ");
        service.enroll(id, enroll);
        service.gradeEnrollment(id, "MATH101", map("letterGrade", "CC"));

        Map stats = service.statistics();
        assertEquals(new Integer(1), stats.get("totalStudents"));
        assertEquals(new Double(2.0), stats.get("averageGpa"));
        assertEquals(new Integer(1), ((Map) stats.get("gradeDistribution")).get("CC"));
        assertEquals(new Integer(1), ((Map) stats.get("courseEnrollments")).get("MATH101"));
    }

    @Test(expected = NotFoundException.class)
    public void missingStudentIsReported() {
        service.getStudent(new Long(42));
    }
}
