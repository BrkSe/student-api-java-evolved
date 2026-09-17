package com.burakkutbay.studentapi.repository;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.burakkutbay.studentapi.model.Student;

public interface StudentRepository {

    void init(Properties properties) throws IOException;

    List findAll();

    Student findById(Long id);

    Student save(Student student);

    boolean delete(Long id);

    int count();

    void flush() throws IOException;
}
