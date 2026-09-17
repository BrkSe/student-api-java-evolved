package com.burakkutbay.studentapi.repository;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.burakkutbay.studentapi.model.Enrollment;
import com.burakkutbay.studentapi.model.LetterGrade;
import com.burakkutbay.studentapi.model.Student;
import com.burakkutbay.studentapi.model.StudentStatus;
import com.burakkutbay.studentapi.util.Dates;

/// Öğrencileri noktalı virgülle ayrılmış bir CSV dosyasında saklar.
///
/// Kolonlar: `id;studentNumber;firstName;lastName;email;birthDate;department;status;enrollments`
/// Ders kayıtları: `KOD:DÖNEM:NOT|KOD:DÖNEM:NOT`
public class FileStudentRepository extends InMemoryStudentRepository {

    private static final Logger LOG = Logger.getLogger(FileStudentRepository.class.getName());

    private static final String HEADER = "id;studentNumber;firstName;lastName;email;birthDate;department;status;enrollments";

    private Path file;

    @Override
    public void init(Properties properties) throws IOException {
        file = Path.of(properties.getProperty("repository.file", "data/students.csv"));
        if (Files.exists(file)) {
            load();
        } else {
            LOG.warning(() -> "Veri dosyası bulunamadı, boş depo ile başlanıyor: " + file.toAbsolutePath());
        }
    }

    private void load() throws IOException {
        var lines = Files.readAllLines(file, UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            var line = lines.get(i);
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            try {
                save(parseLine(line));
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Satır " + (i + 1) + " okunamadı: " + line, e);
            }
        }
        LOG.info(() -> count() + " öğrenci yüklendi: " + file);
    }

    private static Student parseLine(String line) {
        var parts = line.split(";", -1);
        if (parts.length != 9) {
            throw new IllegalArgumentException("Beklenen kolon sayısı 9, bulunan " + parts.length);
        }
        var enrollments = parts[8].isBlank()
                ? List.<Enrollment>of()
                : Arrays.stream(parts[8].split("\\|")).map(FileStudentRepository::parseEnrollment).toList();
        return new Student(
                Long.valueOf(parts[0].strip()),
                parts[1].strip(),
                parts[2].strip(),
                parts[3].strip(),
                parts[4].strip(),
                Dates.parse(parts[5]),
                parts[6].strip(),
                StudentStatus.fromLabel(parts[7])
                        .orElseThrow(() -> new IllegalArgumentException("Geçersiz durum: " + parts[7])),
                enrollments);
    }

    private static Enrollment parseEnrollment(String value) {
        var fields = value.split(":", -1);
        var grade = fields[2].isBlank()
                ? null
                : LetterGrade.parse(fields[2]).orElseThrow(() -> new IllegalArgumentException("Geçersiz not: " + fields[2]));
        return new Enrollment(fields[0].strip(), fields[1].strip(), grade);
    }

    @Override
    public synchronized void flush() throws IOException {
        if (file == null) {
            return;
        }
        var lines = Stream.concat(Stream.of(HEADER), findAll().stream().map(FileStudentRepository::toLine)).toList();
        Files.write(file, lines, UTF_8);
    }

    private static String toLine(Student s) {
        var enrollments = s.enrollments().stream()
                .map(e -> e.courseCode() + ":" + e.semester() + ":" + (e.isGraded() ? e.letterGrade().name() : ""))
                .collect(Collectors.joining("|"));
        return String.join(";", String.valueOf(s.id()), s.studentNumber(), s.firstName(), s.lastName(), s.email(),
                s.birthDate().toString(), s.department(), s.status().name(), enrollments);
    }
}
