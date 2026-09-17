package com.burakkutbay.studentapi.repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.burakkutbay.studentapi.model.Enrollment;
import com.burakkutbay.studentapi.model.Student;
import com.burakkutbay.studentapi.model.StudentStatusUtil;
import com.burakkutbay.studentapi.util.DateUtils;

/**
 * Öğrencileri noktalı virgülle ayrılmış bir CSV dosyasında saklar.
 * Kolonlar: id;studentNumber;firstName;lastName;email;birthDate;department;status;enrollments
 * Ders kayıtları: KOD:DÖNEM:NOT|KOD:DÖNEM:NOT
 */
public class FileStudentRepository extends InMemoryStudentRepository {

    private static final Logger LOG = Logger.getLogger(FileStudentRepository.class.getName());

    private static final String HEADER = "id;studentNumber;firstName;lastName;email;birthDate;department;status;enrollments";

    private File file;

    public void init(Properties properties) throws IOException {
        String path = properties.getProperty("repository.file", "data/students.csv");
        file = new File(path);
        if (file.exists()) {
            load();
        } else {
            LOG.warning("Veri dosyası bulunamadı, boş depo ile başlanıyor: " + file.getAbsolutePath());
        }
    }

    private void load() throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 || line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }
                try {
                    Student student = parseLine(line);
                    save(student);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Satır " + lineNumber + " okunamadı: " + line, e);
                }
            }
            LOG.info(count() + " öğrenci yüklendi: " + file.getPath());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // yut
                }
            }
        }
    }

    private Student parseLine(String line) throws ParseException {
        String[] parts = line.split(";", -1);
        if (parts.length != 9) {
            throw new IllegalArgumentException("Beklenen kolon sayısı 9, bulunan " + parts.length);
        }
        Student student = new Student();
        student.setId(Long.valueOf(parts[0].trim()));
        student.setStudentNumber(parts[1].trim());
        student.setFirstName(parts[2].trim());
        student.setLastName(parts[3].trim());
        student.setEmail(parts[4].trim());
        student.setBirthDate(DateUtils.parse(parts[5]));
        student.setDepartment(parts[6].trim());
        student.setStatus(StudentStatusUtil.fromLabel(parts[7]));
        if (parts[8].trim().length() > 0) {
            String[] enrollmentParts = parts[8].split("\\|");
            for (int i = 0; i < enrollmentParts.length; i++) {
                String[] fields = enrollmentParts[i].split(":", -1);
                String grade = fields[2].trim().length() == 0 ? null : fields[2].trim();
                student.addEnrollment(new Enrollment(fields[0].trim(), fields[1].trim(), grade));
            }
        }
        return student;
    }

    public synchronized void flush() throws IOException {
        if (file == null) {
            return;
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(HEADER);
            writer.newLine();
            List all = findAll();
            for (int i = 0; i < all.size(); i++) {
                Student s = (Student) all.get(i);
                StringBuffer enrollments = new StringBuffer();
                for (Iterator it = s.getEnrollments().iterator(); it.hasNext();) {
                    Enrollment e = (Enrollment) it.next();
                    if (enrollments.length() > 0) {
                        enrollments.append('|');
                    }
                    enrollments.append(e.getCourseCode()).append(':').append(e.getSemester()).append(':')
                            .append(e.getLetterGrade() == null ? "" : e.getLetterGrade());
                }
                writer.write(s.getId() + ";" + s.getStudentNumber() + ";" + s.getFirstName() + ";"
                        + s.getLastName() + ";" + s.getEmail() + ";" + DateUtils.format(s.getBirthDate()) + ";"
                        + s.getDepartment() + ";" + StudentStatusUtil.toLabel(s.getStatus()) + ";" + enrollments);
                writer.newLine();
            }
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // yut
                }
            }
        }
    }
}
