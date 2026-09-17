package com.burakkutbay.studentapi.service;

import static com.burakkutbay.studentapi.util.StringUtils.capitalize;
import static com.burakkutbay.studentapi.util.StringUtils.isBlank;
import static java.util.Objects.requireNonNullElse;

import java.text.Collator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SequencedMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.burakkutbay.studentapi.model.Course;
import com.burakkutbay.studentapi.model.Enrollment;
import com.burakkutbay.studentapi.model.LetterGrade;
import com.burakkutbay.studentapi.model.Student;
import com.burakkutbay.studentapi.model.StudentStatus;
import com.burakkutbay.studentapi.notification.NotificationService;
import com.burakkutbay.studentapi.repository.StudentRepository;
import com.burakkutbay.studentapi.service.ApiException.Conflict;
import com.burakkutbay.studentapi.service.ApiException.NotFound;
import com.burakkutbay.studentapi.service.ApiException.Validation;
import com.burakkutbay.studentapi.util.Dates;
import com.burakkutbay.studentapi.util.EmailValidator;
import com.burakkutbay.studentapi.util.StudentNumbers;

public final class StudentService {

    public static final int MIN_AGE = 16;
    public static final int MAX_CREDITS_PER_SEMESTER = 20;

    private static final Pattern SEMESTER = Pattern.compile("\\d{4}-(GUZ|BAHAR|YAZ)");
    private static final Locale TURKISH = Locale.of("tr", "TR");

    private final StudentRepository repository;
    private final GpaCalculator gpaCalculator;
    private final NotificationService notifications;
    private final Clock clock;

    public StudentService(StudentRepository repository, GpaCalculator gpaCalculator, NotificationService notifications) {
        this(repository, gpaCalculator, notifications, Clock.systemDefaultZone());
    }

    public StudentService(StudentRepository repository, GpaCalculator gpaCalculator, NotificationService notifications,
                          Clock clock) {
        this.repository = repository;
        this.gpaCalculator = gpaCalculator;
        this.notifications = notifications;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ sorgular

    public List<Student> listStudents(String department, String statusLabel, String sortBy) {
        StudentStatus status = isBlank(statusLabel)
                ? null
                : StudentStatus.fromLabel(statusLabel).orElseThrow(() -> new Validation("Geçersiz durum: " + statusLabel));

        Comparator<Student> order = switch (isBlank(sortBy) ? "id" : sortBy) {
            case "id" -> Comparator.comparingLong(Student::id);
            case "name" -> {
                var collator = Collator.getInstance(TURKISH);
                yield Comparator.comparing(Student::lastName, collator).thenComparing(Student::firstName, collator);
            }
            case "gpa" -> Comparator.comparingDouble((Student s) -> gpaOf(s).orElse(-1.0)).reversed();
            case "age" -> Comparator.comparing(Student::birthDate).reversed();
            default -> throw new Validation("Geçersiz sıralama alanı: " + sortBy);
        };

        return repository.findAll().stream()
                .filter(s -> isBlank(department) || department.strip().equalsIgnoreCase(s.department()))
                .filter(s -> status == null || s.status() == status)
                .sorted(order)
                .toList();
    }

    public Student getStudent(long id) {
        return repository.findById(id).orElseThrow(() -> new NotFound("Öğrenci bulunamadı: " + id));
    }

    // ------------------------------------------------------------------ komutlar

    public Student createStudent(Map<String, Object> body) {
        var errors = new ArrayList<String>();

        var firstName = getString(body, "firstName");
        var lastName = getString(body, "lastName");
        var email = getString(body, "email");
        var birthDateText = getString(body, "birthDate");
        var department = getString(body, "department");
        var statusText = getString(body, "status");

        if (isBlank(firstName)) {
            errors.add("firstName zorunludur");
        }
        if (isBlank(lastName)) {
            errors.add("lastName zorunludur");
        }
        if (isBlank(email)) {
            errors.add("email zorunludur");
        } else if (!EmailValidator.isValid(email)) {
            errors.add("email geçersiz");
        } else if (emailInUse(email, null)) {
            errors.add("email zaten kayıtlı");
        }
        LocalDate birthDate = null;
        if (isBlank(birthDateText)) {
            errors.add("birthDate zorunludur");
        } else {
            birthDate = validateBirthDate(birthDateText, errors);
        }
        if (isBlank(department)) {
            errors.add("department zorunludur");
        }
        var status = StudentStatus.ACTIVE;
        if (!isBlank(statusText)) {
            var parsed = StudentStatus.fromLabel(statusText);
            if (parsed.isEmpty()) {
                errors.add("status geçersiz");
            } else {
                status = parsed.get();
            }
        }
        if (!errors.isEmpty()) {
            throw new Validation(errors);
        }

        var saved = repository.save(new Student(null, null, capitalize(firstName), capitalize(lastName),
                email.strip().toLowerCase(Locale.ROOT), birthDate, department.strip(), status, List.of()));
        var numbered = repository.save(saved.withStudentNumber(StudentNumbers.of(Year.now(clock), saved.id())));
        notifications.enqueue(numbered.email(), "Kaydınız oluşturuldu",
                "Merhaba %s, öğrenci numaranız: %s".formatted(numbered.firstName(), numbered.studentNumber()));
        return numbered;
    }

    public Student updateStudent(long id, Map<String, Object> body) {
        var student = getStudent(id);
        var errors = new ArrayList<String>();

        String firstName = null;
        String lastName = null;
        String email = null;
        LocalDate birthDate = null;
        String department = null;
        StudentStatus status = null;

        if (body.containsKey("firstName")) {
            firstName = getString(body, "firstName");
            if (isBlank(firstName)) {
                errors.add("firstName boş olamaz");
            }
        }
        if (body.containsKey("lastName")) {
            lastName = getString(body, "lastName");
            if (isBlank(lastName)) {
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
            birthDate = validateBirthDate(getString(body, "birthDate"), errors);
        }
        if (body.containsKey("department")) {
            department = getString(body, "department");
            if (isBlank(department)) {
                errors.add("department boş olamaz");
            }
        }
        if (body.containsKey("status")) {
            status = StudentStatus.fromLabel(getString(body, "status")).orElse(null);
            if (status == null) {
                errors.add("status geçersiz");
            }
        }
        if (!errors.isEmpty()) {
            throw new Validation(errors);
        }

        var updated = new Student(
                student.id(),
                student.studentNumber(),
                firstName == null ? student.firstName() : capitalize(firstName),
                lastName == null ? student.lastName() : capitalize(lastName),
                email == null ? student.email() : email.strip().toLowerCase(Locale.ROOT),
                requireNonNullElse(birthDate, student.birthDate()),
                department == null ? student.department() : department.strip(),
                requireNonNullElse(status, student.status()),
                student.enrollments());

        boolean graduating = updated.status() == StudentStatus.GRADUATED && student.status() != StudentStatus.GRADUATED;
        if (graduating) {
            var gpa = gpaOf(updated);
            if (gpa.isEmpty() || gpa.getAsDouble() < 2.0) {
                throw new Conflict("Mezuniyet için GNO en az 2.00 olmalıdır");
            }
        }
        var saved = repository.save(updated);
        if (graduating) {
            notifications.enqueue(saved.email(), "Tebrikler!",
                    "Sevgili %s, mezuniyetiniz onaylandı.".formatted(saved.firstName()));
        }
        return saved;
    }

    public void deleteStudent(long id) {
        var student = getStudent(id);
        if (student.enrollments().stream().anyMatch(Enrollment::isGraded)) {
            throw new Conflict("Notu girilmiş dersi olan öğrenci silinemez, durumunu WITHDRAWN olarak güncelleyin");
        }
        repository.delete(id);
    }

    public Enrollment enroll(long id, Map<String, Object> body) {
        var student = getStudent(id);
        var courseCode = getString(body, "courseCode");
        var semesterText = getString(body, "semester");

        var errors = new ArrayList<String>();
        Course course = null;
        if (isBlank(courseCode)) {
            errors.add("courseCode zorunludur");
        } else {
            course = CourseCatalog.findByCode(courseCode).orElse(null);
            if (course == null) {
                errors.add("Ders bulunamadı: " + courseCode);
            }
        }
        if (isBlank(semesterText)) {
            errors.add("semester zorunludur");
        } else if (!SEMESTER.matcher(semesterText.strip()).matches()) {
            errors.add("semester YYYY-GUZ, YYYY-BAHAR veya YYYY-YAZ formatında olmalıdır");
        }
        if (!errors.isEmpty()) {
            throw new Validation(errors);
        }
        var semester = semesterText.strip();

        if (student.status() != StudentStatus.ACTIVE) {
            throw new Conflict("Sadece aktif öğrenciler derse kaydolabilir, mevcut durum: " + student.status());
        }

        // Sıra önemli: ilk eşleşen kayıt hangi hatanın döneceğini belirler, bu yüzden erken çıkışlı döngü.
        for (var existing : student.enrollments()) {
            if (existing.courseCode().equals(course.code())) {
                if (!existing.isGraded()) {
                    throw new Conflict("Öğrenci bu derse zaten kayıtlı: " + course.code());
                }
                if (existing.letterGrade().isPassing()) {
                    throw new Conflict("Öğrenci bu dersi zaten geçmiş: " + course.code());
                }
            }
        }
        int semesterCredits = student.enrollments().stream()
                .filter(e -> e.semester().equals(semester))
                .flatMap(e -> CourseCatalog.findByCode(e.courseCode()).stream())
                .mapToInt(Course::credits)
                .sum();
        int requested = semesterCredits + course.credits();
        if (requested > MAX_CREDITS_PER_SEMESTER) {
            throw new Conflict("Dönem kredi limiti aşıldı: " + requested + " > " + MAX_CREDITS_PER_SEMESTER);
        }

        var enrollment = new Enrollment(course.code(), semester, null);
        var enrollments = new ArrayList<>(student.enrollments());
        enrollments.add(enrollment);
        repository.save(student.withEnrollments(enrollments));
        return enrollment;
    }

    public Enrollment gradeEnrollment(long id, String courseCode, Map<String, Object> body) {
        var student = getStudent(id);
        var grade = LetterGrade.parse(getString(body, "letterGrade")).orElseThrow(() ->
                new Validation("letterGrade şunlardan biri olmalıdır: AA, BA, BB, CB, CC, DC, DD, FD, FF"));

        var enrollments = new ArrayList<>(student.enrollments());
        int index = IntStream.range(0, enrollments.size())
                .filter(i -> enrollments.get(i).courseCode().equalsIgnoreCase(courseCode) && !enrollments.get(i).isGraded())
                .findFirst()
                .orElseThrow(() -> new NotFound("Notlandırılacak ders kaydı bulunamadı: " + courseCode));

        var graded = enrollments.get(index).withGrade(grade);
        enrollments.set(index, graded);
        repository.save(student.withEnrollments(enrollments));
        if (!grade.isPassing()) {
            notifications.enqueue(student.email(), "Ders sonucu",
                    "%s dersinden %s notu ile kaldınız.".formatted(graded.courseCode(), grade));
        }
        return graded;
    }

    // ------------------------------------------------------------------ raporlar

    public SequencedMap<String, Object> gpaSummary(long id) {
        var student = getStudent(id);
        var gpa = gpaOf(student);
        var result = new LinkedHashMap<String, Object>();
        result.put("studentId", student.id());
        result.put("studentNumber", student.studentNumber());
        result.put("gpa", gpa.isPresent() ? gpa.getAsDouble() : null);
        result.put("honor", gpaCalculator.honorLevel(gpa));
        result.put("completedCredits", gpaCalculator.completedCredits(student.enrollments()));
        result.put("gradedCourses", (int) student.enrollments().stream().filter(Enrollment::isGraded).count());
        return result;
    }

    public String transcript(long id) {
        var student = getStudent(id);
        var line = "=".repeat(60);
        var thinLine = "-".repeat(60);

        var sb = new StringBuilder("""
                %s
                TRANSKRİPT
                %s
                Öğrenci No : %s
                Ad Soyad   : %s
                Bölüm      : %s
                Durum      : %s
                %s
                """.formatted(line, line, student.studentNumber(), student.fullName(), student.department(),
                student.status(), thinLine));

        var bySemester = student.enrollments().stream()
                .collect(Collectors.groupingBy(Enrollment::semester, TreeMap::new, Collectors.toList()));
        if (bySemester.isEmpty()) {
            sb.append("Ders kaydı bulunmuyor.\n");
        }
        bySemester.forEach((semester, enrollments) -> {
            sb.append('[').append(semester).append("]\n");
            for (var enrollment : enrollments) {
                var course = CourseCatalog.findByCode(enrollment.courseCode());
                sb.append("  %-9s%-28s%-6s%s\n".formatted(
                        enrollment.courseCode(),
                        course.map(Course::name).orElse("?"),
                        course.map(c -> String.valueOf(c.credits())).orElse("?"),
                        enrollment.isGraded() ? enrollment.letterGrade() : "--"));
            }
        });

        var gpa = gpaOf(student);
        var gpaText = gpa.isPresent() ? String.format(Locale.US, "%.2f", gpa.getAsDouble()) : "-";
        sb.append(thinLine).append('\n')
                .append("GNO: %s (%s)\n".formatted(gpaText, gpaCalculator.honorLevel(gpa)))
                .append("Tamamlanan kredi: ").append(gpaCalculator.completedCredits(student.enrollments())).append('\n');
        return sb.toString();
    }

    public SequencedMap<String, Object> statistics() {
        var students = repository.findAll();

        var byStatus = new EnumMap<StudentStatus, Integer>(StudentStatus.class);
        for (var status : StudentStatus.values()) {
            byStatus.put(status, 0);
        }
        students.forEach(s -> byStatus.merge(s.status(), 1, Integer::sum));

        var byDepartment = students.stream()
                .collect(Collectors.groupingBy(Student::department, TreeMap::new, Collectors.summingInt(_ -> 1)));

        var courseEnrollments = students.stream()
                .flatMap(s -> s.enrollments().stream().map(Enrollment::courseCode).distinct())
                .collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.summingInt(_ -> 1)));

        var gradeDistribution = new EnumMap<LetterGrade, Integer>(LetterGrade.class);
        for (var grade : LetterGrade.values()) {
            gradeDistribution.put(grade, 0);
        }
        students.stream()
                .flatMap(s -> s.enrollments().stream())
                .filter(Enrollment::isGraded)
                .forEach(e -> gradeDistribution.merge(e.letterGrade(), 1, Integer::sum));

        record Ranked(Student student, double gpa) {
        }
        var ranked = students.stream()
                .<Ranked>mapMulti((s, sink) -> gpaOf(s).ifPresent(gpa -> sink.accept(new Ranked(s, gpa))))
                .toList();

        // Sıralı toplama: DoubleStream.average()'ın telafili toplaması yuvarlamada farklı sonuç verebilir.
        double gpaTotal = 0.0;
        for (var r : ranked) {
            gpaTotal += r.gpa();
        }
        var topStudents = ranked.stream()
                .sorted(Comparator.comparingDouble(Ranked::gpa).reversed())
                .limit(3)
                .map(r -> {
                    var item = new LinkedHashMap<String, Object>();
                    item.put("id", r.student().id());
                    item.put("fullName", r.student().fullName());
                    item.put("gpa", r.gpa());
                    return item;
                })
                .toList();

        var result = new LinkedHashMap<String, Object>();
        result.put("totalStudents", students.size());
        result.put("byStatus", byStatus);
        result.put("byDepartment", byDepartment);
        result.put("averageGpa", ranked.isEmpty() ? null : GpaCalculator.round(gpaTotal / ranked.size()));
        result.put("topStudents", topStudents);
        result.put("courseEnrollments", courseEnrollments);
        result.put("gradeDistribution", gradeDistribution);
        return result;
    }

    // ------------------------------------------------------------------ yardımcılar

    private OptionalDouble gpaOf(Student student) {
        return gpaCalculator.calculate(student.enrollments());
    }

    private LocalDate validateBirthDate(String text, List<String> errors) {
        try {
            var birthDate = Dates.parse(text);
            if (birthDate.until(LocalDate.now(clock)).getYears() < MIN_AGE) {
                errors.add("öğrenci en az " + MIN_AGE + " yaşında olmalıdır");
            }
            return birthDate;
        } catch (DateTimeParseException _) {
            errors.add("birthDate yyyy-MM-dd formatında olmalıdır");
            return null;
        }
    }

    private boolean emailInUse(String email, Long excludeId) {
        var normalized = email.strip();
        return repository.findAll().stream()
                .filter(s -> excludeId == null || !excludeId.equals(s.id()))
                .anyMatch(s -> s.email() != null && s.email().equalsIgnoreCase(normalized));
    }

    private static String getString(Map<String, Object> body, String key) {
        return switch (body.get(key)) {
            case null -> null;
            case String s -> s;
            case Object other -> other.toString();
        };
    }
}
