package com.burakkutbay.studentapi.repository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.burakkutbay.studentapi.model.Student;

public interface StudentRepository {

    default void init(Properties properties) throws IOException {
    }

    /// Kimliğe göre sıralı, değiştirilemez anlık görüntü.
    List<Student> findAll();

    Optional<Student> findById(long id);

    /// Kimliği olmayan öğrenciye yeni kimlik atar; kaydedilen örneği döndürür.
    Student save(Student student);

    boolean delete(long id);

    int count();

    default void flush() throws IOException {
    }
}
