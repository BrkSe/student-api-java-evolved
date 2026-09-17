package com.burakkutbay.studentapi.service;

import java.text.Collator;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.burakkutbay.studentapi.model.Course;
import com.burakkutbay.studentapi.model.Enrollment;
import com.burakkutbay.studentapi.model.Student;
import com.burakkutbay.studentapi.model.StudentStatus;
import com.burakkutbay.studentapi.model.StudentStatusUtil;
import com.burakkutbay.studentapi.notification.NotificationService;
import com.burakkutbay.studentapi.repository.StudentRepository;
import com.burakkutbay.studentapi.util.DateUtils;
import com.burakkutbay.studentapi.util.EmailValidator;
import com.burakkutbay.studentapi.util.IdGenerator;
import com.burakkutbay.studentapi.util.StringUtils;

public class StudentService {

    public static final int MIN_AGE = 16;
    public static final int MAX_CREDITS_PER_SEMESTER = 20;

    private final StudentRepository repository;
    private final GpaCalculator gpaCalculator;
    private final NotificationService notificationService;

    public StudentService(StudentRepository repository, GpaCalculator gpaCalculator,
                          NotificationService notificationService) {
        this.repository = repository;
        this.gpaCalculator = gpaCalculator;
        this.notificationService = notificationService;
    }

    // ------------------------------------------------------------------ sorgular

    public List listStudents(String department, String statusLabel, String sortBy) {
        int status = StudentStatusUtil.UNKNOWN;
        if (!StringUtils.isBlank(statusLabel)) {
            status = StudentStatusUtil.fromLabel(statusLabel);
            if (status == StudentStatusUtil.UNKNOWN) {
                throw new ValidationException("Geçersiz durum: " + statusLabel);
            }
        }

        List all = repository.findAll();
        List filtered = new ArrayList();
        for (int i = 0; i < all.size(); i++) {
            Student student = (Student) all.get(i);
            if (!StringUtils.isBlank(department) && !department.trim().equalsIgnoreCase(student.getDepartment())) {
                continue;
            }
            if (status != StudentStatusUtil.UNKNOWN && student.getStatus() != status) {
                continue;
            }
            filtered.add(student);
        }

        Comparator comparator = null;
        if (StringUtils.isBlank(sortBy) || sortBy.equals("id")) {
            comparator = null;
        } else if (sortBy.equals("name")) {
            final Collator collator = Collator.getInstance(new Locale("tr", "TR"));
            comparator = new Comparator() {
                public int compare(Object o1, Object o2) {
                    Student s1 = (Student) o1;
                    Student s2 = (Student) o2;
                    int result = collator.compare(s1.getLastName(), s2.getLastName());
                    if (result == 0) {
                        result = collator.compare(s1.getFirstName(), s2.getFirstName());
                    }
                    return result;
                }
            };
        } else if (sortBy.equals("gpa")) {
            comparator = new Comparator() {
                public int compare(Object o1, Object o2) {
                    double gpa1 = gpaCalculator.calculate(((Student) o1).getEnrollments());
                    double gpa2 = gpaCalculator.calculate(((Student) o2).getEnrollments());
                    return Double.compare(gpa2, gpa1);
                }
            };
        } else if (sortBy.equals("age")) {
            comparator = new Comparator() {
                public int compare(Object o1, Object o2) {
                    Date d1 = ((Student) o1).getBirthDate();
                    Date d2 = ((Student) o2).getBirthDate();
                    return d2.compareTo(d1);
                }
            };
        } else {
            throw new ValidationException("Geçersiz sıralama alanı: " + sortBy);
        }
        if (comparator != null) {
            Collections.sort(filtered, comparator);
        }
        return filtered;
    }

    public Student getStudent(Long id) {
        Student student = repository.findById(id);
        if (student == null) {
            throw new NotFoundException("Öğrenci bulunamadı: " + id);
        }
        return student;
    }

    // ------------------------------------------------------------------ komutlar

    public Student createStudent(Map body) {
        List errors = new ArrayList();

        String firstName = getString(body, "firstName");
        String lastName = getString(body, "lastName");
        String email = getString(body, "email");
        String birthDateText = getString(body, "birthDate");
        String department = getString(body, "department");
        String statusText = getString(body, "status");

        if (StringUtils.isBlank(firstName)) {
            errors.add("firstName zorunludur");
        }
        if (StringUtils.isBlank(lastName)) {
            errors.add("lastName zorunludur");
        }
        if (StringUtils.isBlank(email)) {
            errors.add("email zorunludur");
        } else if (!EmailValidator.isValid(email)) {
            errors.add("email geçersiz");
        } else if (emailInUse(email, null)) {
            errors.add("email zaten kayıtlı");
        }
        Date birthDate = null;
        if (StringUtils.isBlank(birthDateText)) {
            errors.add("birthDate zorunludur");
        } else {
            try {
                birthDate = DateUtils.parse(birthDateText);
                if (DateUtils.calculateAge(birthDate) < MIN_AGE) {
                    errors.add("öğrenci en az " + MIN_AGE + " yaşında olmalıdır");
                }
            } catch (ParseException e) {
                errors.add("birthDate yyyy-MM-dd formatında olmalıdır");
            }
        }
        if (StringUtils.isBlank(department)) {
            errors.add("department zorunludur");
        }
        int status = StudentStatus.ACTIVE;
        if (!StringUtils.isBlank(statusText)) {
            status = StudentStatusUtil.fromLabel(statusText);
            if (status == StudentStatusUtil.UNKNOWN) {
                errors.add("status geçersiz");
            }
        }
        if (errors.size() > 0) {
            throw new ValidationException(errors);
        }

        Student student = new Student();
        student.setFirstName(StringUtils.capitalize(firstName));
        student.setLastName(StringUtils.capitalize(lastName));
        student.setEmail(email.trim().toLowerCase());
        student.setBirthDate(birthDate);
        student.setDepartment(department.trim());
        student.setStatus(status);

        Student saved = repository.save(student);
        saved.setStudentNumber(IdGenerator.studentNumber(DateUtils.currentYear(), saved.getId()));
        notificationService.enqueue(saved.getEmail(), "Kaydınız oluşturuldu",
                "Merhaba " + saved.getFirstName() + ", öğrenci numaranız: " + saved.getStudentNumber());
        return saved;
    }

    public Student updateStudent(Long id, Map body) {
        Student student = getStudent(id);
        List errors = new ArrayList();

        String firstName = null;
        String lastName = null;
        String email = null;
        Date birthDate = null;
        String department = null;
        int status = StudentStatusUtil.UNKNOWN;

        if (body.containsKey("firstName")) {
            firstName = getString(body, "firstName");
            if (StringUtils.isBlank(firstName)) {
                errors.add("firstName boş olamaz");
            }
        }
        if (body.containsKey("lastName")) {
            lastName = getString(body, "lastName");
            if (StringUtils.isBlank(lastName)) {
                errors.add("lastName boş olamaz");
            }
        }
        if (body.containsKey("email")) {
            email = getString(body, "email");
            if (!EmailValidator.isValid(email)) {
                errors.add("email geçersiz");
            } else if (emailInUse(email, id)) {
                errors.add("email zaten kayıtlı");
            }
        }
        if (body.containsKey("birthDate")) {
            try {
                birthDate = DateUtils.parse(getString(body, "birthDate"));
                if (DateUtils.calculateAge(birthDate) < MIN_AGE) {
                    errors.add("öğrenci en az " + MIN_AGE + " yaşında olmalıdır");
                }
            } catch (ParseException e) {
                errors.add("birthDate yyyy-MM-dd formatında olmalıdır");
            }
        }
        if (body.containsKey("department")) {
            department = getString(body, "department");
            if (StringUtils.isBlank(department)) {
                errors.add("department boş olamaz");
            }
        }
        if (body.containsKey("status")) {
            status = StudentStatusUtil.fromLabel(getString(body, "status"));
            if (status == StudentStatusUtil.UNKNOWN) {
                errors.add("status geçersiz");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        int previousStatus = student.getStatus();
        if (firstName != null) {
            student.setFirstName(StringUtils.capitalize(firstName));
        }
        if (lastName != null) {
            student.setLastName(StringUtils.capitalize(lastName));
        }
        if (email != null) {
            student.setEmail(email.trim().toLowerCase());
        }
        if (birthDate != null) {
            student.setBirthDate(birthDate);
        }
        if (department != null) {
            student.setDepartment(department.trim());
        }
        if (status != StudentStatusUtil.UNKNOWN) {
            if (status == StudentStatus.GRADUATED && previousStatus != StudentStatus.GRADUATED) {
                double gpa = gpaCalculator.calculate(student.getEnrollments());
                if (gpa == GpaCalculator.NO_GPA || gpa < 2.0) {
                    throw new ConflictException("Mezuniyet için GNO en az 2.00 olmalıdır");
                }
                notificationService.enqueue(student.getEmail(), "Tebrikler!",
                        "Sevgili " + student.getFirstName() + ", mezuniyetiniz onaylandı.");
            }
            student.setStatus(status);
        }
        return repository.save(student);
    }

    public void deleteStudent(Long id) {
        Student student = getStudent(id);
        for (Iterator it = student.getEnrollments().iterator(); it.hasNext();) {
            Enrollment enrollment = (Enrollment) it.next();
            if (enrollment.isGraded()) {
                throw new ConflictException(
                        "Notu girilmiş dersi olan öğrenci silinemez, durumunu WITHDRAWN olarak güncelleyin");
            }
        }
        repository.delete(id);
    }

    public Enrollment enroll(Long id, Map body) {
        Student student = getStudent(id);
        String courseCode = getString(body, "courseCode");
        String semester = getString(body, "semester");

        List errors = new ArrayList();
        Course course = null;
        if (StringUtils.isBlank(courseCode)) {
            errors.add("courseCode zorunludur");
        } else {
            course = CourseCatalog.findByCode(courseCode);
            if (course == null) {
                errors.add("Ders bulunamadı: " + courseCode);
            }
        }
        if (StringUtils.isBlank(semester)) {
            errors.add("semester zorunludur");
        } else if (!semester.trim().matches("\\d{4}-(GUZ|BAHAR|YAZ)")) {
            errors.add("semester YYYY-GUZ, YYYY-BAHAR veya YYYY-YAZ formatında olmalıdır");
        }
        if (errors.size() > 0) {
            throw new ValidationException(errors);
        }
        semester = semester.trim();

        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new ConflictException("Sadece aktif öğrenciler derse kaydolabilir, mevcut durum: "
                    + StudentStatusUtil.toLabel(student.getStatus()));
        }

        int semesterCredits = 0;
        for (int i = 0; i < student.getEnrollments().size(); i++) {
            Enrollment existing = (Enrollment) student.getEnrollments().get(i);
            if (existing.getCourseCode().equals(course.getCode())) {
                if (existing.getLetterGrade() == null) {
                    throw new ConflictException("Öğrenci bu derse zaten kayıtlı: " + course.getCode());
                }
                if (gpaCalculator.isPassing(existing.getLetterGrade())) {
                    throw new ConflictException("Öğrenci bu dersi zaten geçmiş: " + course.getCode());
                }
            }
            if (existing.getSemester().equals(semester)) {
                Course existingCourse = CourseCatalog.findByCode(existing.getCourseCode());
                if (existingCourse != null) {
                    semesterCredits += existingCourse.getCredits();
                }
            }
        }
        if (semesterCredits + course.getCredits() > MAX_CREDITS_PER_SEMESTER) {
            throw new ConflictException("Dönem kredi limiti aşıldı: " + (semesterCredits + course.getCredits())
                    + " > " + MAX_CREDITS_PER_SEMESTER);
        }

        Enrollment enrollment = new Enrollment(course.getCode(), semester, null);
        student.addEnrollment(enrollment);
        repository.save(student);
        return enrollment;
    }

    public Enrollment gradeEnrollment(Long id, String courseCode, Map body) {
        Student student = getStudent(id);
        String letterGrade = getString(body, "letterGrade");
        if (StringUtils.isBlank(letterGrade) || !gpaCalculator.isValidGrade(letterGrade)) {
            throw new ValidationException("letterGrade şunlardan biri olmalıdır: AA, BA, BB, CB, CC, DC, DD, FD, FF");
        }
        Enrollment target = null;
        for (Iterator it = student.getEnrollments().iterator(); it.hasNext();) {
            Enrollment enrollment = (Enrollment) it.next();
            if (enrollment.getCourseCode().equalsIgnoreCase(courseCode) && enrollment.getLetterGrade() == null) {
                target = enrollment;
                break;
            }
        }
        if (target == null) {
            throw new NotFoundException("Notlandırılacak ders kaydı bulunamadı: " + courseCode);
        }
        target.setLetterGrade(letterGrade.trim().toUpperCase());
        repository.save(student);
        if (!gpaCalculator.isPassing(target.getLetterGrade())) {
            notificationService.enqueue(student.getEmail(), "Ders sonucu",
                    target.getCourseCode() + " dersinden " + target.getLetterGrade() + " notu ile kaldınız.");
        }
        return target;
    }

    // ------------------------------------------------------------------ raporlar

    public Map gpaSummary(Long id) {
        Student student = getStudent(id);
        double gpa = gpaCalculator.calculate(student.getEnrollments());
        int gradedCourses = 0;
        for (int i = 0; i < student.getEnrollments().size(); i++) {
            if (((Enrollment) student.getEnrollments().get(i)).isGraded()) {
                gradedCourses++;
            }
        }
        Map result = new LinkedHashMap();
        result.put("studentId", student.getId());
        result.put("studentNumber", student.getStudentNumber());
        result.put("gpa", gpa == GpaCalculator.NO_GPA ? null : new Double(gpa));
        result.put("honor", gpaCalculator.honorLevel(gpa));
        result.put("completedCredits", new Integer(gpaCalculator.completedCredits(student.getEnrollments())));
        result.put("gradedCourses", new Integer(gradedCourses));
        return result;
    }

    public String transcript(Long id) {
        Student student = getStudent(id);
        String line = StringUtils.repeat("=", 60);
        String thinLine = StringUtils.repeat("-", 60);

        StringBuffer sb = new StringBuffer();
        sb.append(line).append("\n");
        sb.append("TRANSKRİPT").append("\n");
        sb.append(line).append("\n");
        sb.append("Öğrenci No : ").append(student.getStudentNumber()).append("\n");
        sb.append("Ad Soyad   : ").append(student.getFullName()).append("\n");
        sb.append("Bölüm      : ").append(student.getDepartment()).append("\n");
        sb.append("Durum      : ").append(StudentStatusUtil.toLabel(student.getStatus())).append("\n");
        sb.append(thinLine).append("\n");

        Map bySemester = new TreeMap();
        for (Iterator it = student.getEnrollments().iterator(); it.hasNext();) {
            Enrollment enrollment = (Enrollment) it.next();
            List list = (List) bySemester.get(enrollment.getSemester());
            if (list == null) {
                list = new ArrayList();
                bySemester.put(enrollment.getSemester(), list);
            }
            list.add(enrollment);
        }
        if (bySemester.isEmpty()) {
            sb.append("Ders kaydı bulunmuyor.").append("\n");
        }
        for (Iterator it = bySemester.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sb.append("[").append(entry.getKey()).append("]").append("\n");
            List list = (List) entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                Enrollment enrollment = (Enrollment) list.get(i);
                Course course = CourseCatalog.findByCode(enrollment.getCourseCode());
                String name = course == null ? "?" : course.getName();
                String credits = course == null ? "?" : String.valueOf(course.getCredits());
                String grade = enrollment.getLetterGrade() == null ? "--" : enrollment.getLetterGrade();
                sb.append("  ").append(StringUtils.padRight(enrollment.getCourseCode(), 9))
                        .append(StringUtils.padRight(name, 28))
                        .append(StringUtils.padRight(credits, 6))
                        .append(grade).append("\n");
            }
        }
        sb.append(thinLine).append("\n");
        double gpa = gpaCalculator.calculate(student.getEnrollments());
        String gpaText = gpa == GpaCalculator.NO_GPA ? "-" : String.format(Locale.US, "%.2f", new Object[] {new Double(gpa)});
        sb.append("GNO: ").append(gpaText).append(" (").append(gpaCalculator.honorLevel(gpa)).append(")").append("\n");
        sb.append("Tamamlanan kredi: ").append(gpaCalculator.completedCredits(student.getEnrollments())).append("\n");
        return sb.toString();
    }

    public Map statistics() {
        List all = repository.findAll();

        Map byStatus = new LinkedHashMap();
        for (int i = 0; i < StudentStatusUtil.ALL.length; i++) {
            byStatus.put(StudentStatusUtil.toLabel(StudentStatusUtil.ALL[i]), new Integer(0));
        }
        Map byDepartment = new TreeMap();
        Map courseCounts = new TreeMap();
        Map gradeDistribution = new LinkedHashMap();
        for (int i = 0; i < GpaCalculator.LETTER_GRADES.length; i++) {
            gradeDistribution.put(GpaCalculator.LETTER_GRADES[i], new Integer(0));
        }

        double gpaTotal = 0.0;
        int gpaCount = 0;
        final Map gpaByStudent = new LinkedHashMap();

        for (Iterator it = all.iterator(); it.hasNext();) {
            Student student = (Student) it.next();

            String statusLabel = StudentStatusUtil.toLabel(student.getStatus());
            Integer statusCount = (Integer) byStatus.get(statusLabel);
            byStatus.put(statusLabel, new Integer(statusCount.intValue() + 1));

            if (byDepartment.containsKey(student.getDepartment())) {
                Integer count = (Integer) byDepartment.get(student.getDepartment());
                byDepartment.put(student.getDepartment(), new Integer(count.intValue() + 1));
            } else {
                byDepartment.put(student.getDepartment(), new Integer(1));
            }

            Set seenCourses = new HashSet();
            for (Iterator eit = student.getEnrollments().iterator(); eit.hasNext();) {
                Enrollment enrollment = (Enrollment) eit.next();
                if (seenCourses.add(enrollment.getCourseCode())) {
                    Integer count = (Integer) courseCounts.get(enrollment.getCourseCode());
                    courseCounts.put(enrollment.getCourseCode(), new Integer(count == null ? 1 : count.intValue() + 1));
                }
                if (enrollment.getLetterGrade() != null) {
                    Integer count = (Integer) gradeDistribution.get(enrollment.getLetterGrade());
                    gradeDistribution.put(enrollment.getLetterGrade(), new Integer(count.intValue() + 1));
                }
            }

            double gpa = gpaCalculator.calculate(student.getEnrollments());
            if (gpa != GpaCalculator.NO_GPA) {
                gpaTotal += gpa;
                gpaCount++;
                gpaByStudent.put(student, new Double(gpa));
            }
        }

        List ranked = new ArrayList(gpaByStudent.keySet());
        Collections.sort(ranked, new Comparator() {
            public int compare(Object o1, Object o2) {
                Double g1 = (Double) gpaByStudent.get(o1);
                Double g2 = (Double) gpaByStudent.get(o2);
                return g2.compareTo(g1);
            }
        });
        List topStudents = new ArrayList();
        for (int i = 0; i < ranked.size() && i < 3; i++) {
            Student student = (Student) ranked.get(i);
            Map item = new LinkedHashMap();
            item.put("id", student.getId());
            item.put("fullName", student.getFullName());
            item.put("gpa", gpaByStudent.get(student));
            topStudents.add(item);
        }

        Map result = new LinkedHashMap();
        result.put("totalStudents", new Integer(all.size()));
        result.put("byStatus", byStatus);
        result.put("byDepartment", byDepartment);
        result.put("averageGpa", gpaCount == 0 ? null : new Double(GpaCalculator.round(gpaTotal / gpaCount)));
        result.put("topStudents", topStudents);
        result.put("courseEnrollments", courseCounts);
        result.put("gradeDistribution", gradeDistribution);
        return result;
    }

    // ------------------------------------------------------------------ yardımcılar

    private boolean emailInUse(String email, Long excludeId) {
        List all = repository.findAll();
        for (int i = 0; i < all.size(); i++) {
            Student student = (Student) all.get(i);
            if (excludeId != null && excludeId.equals(student.getId())) {
                continue;
            }
            if (student.getEmail() != null && student.getEmail().equalsIgnoreCase(email.trim())) {
                return true;
            }
        }
        return false;
    }

    private static String getString(Map body, String key) {
        Object value = body.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }
}
