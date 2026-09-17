package com.burakkutbay.studentapi.repository;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.burakkutbay.studentapi.model.Student;
import com.burakkutbay.studentapi.util.IdGenerator;

public class InMemoryStudentRepository implements StudentRepository {

    protected final Hashtable students = new Hashtable();

    public InMemoryStudentRepository() {
    }

    public void init(Properties properties) throws IOException {
        // bellek içi depo için başlatma gerekmiyor
    }

    public List findAll() {
        Vector result = new Vector();
        Enumeration e = students.elements();
        while (e.hasMoreElements()) {
            result.addElement(e.nextElement());
        }
        Collections.sort(result, new Comparator() {
            public int compare(Object o1, Object o2) {
                Student s1 = (Student) o1;
                Student s2 = (Student) o2;
                return s1.getId().compareTo(s2.getId());
            }
        });
        return result;
    }

    public Student findById(Long id) {
        if (id == null) {
            return null;
        }
        return (Student) students.get(id);
    }

    public synchronized Student save(Student student) {
        if (student.getId() == null) {
            student.setId(IdGenerator.getInstance().nextId());
        } else {
            IdGenerator.getInstance().ensureAbove(student.getId().longValue());
        }
        students.put(student.getId(), student);
        return student;
    }

    public synchronized boolean delete(Long id) {
        if (id == null) {
            return false;
        }
        return students.remove(id) != null;
    }

    public int count() {
        return students.size();
    }

    public void flush() throws IOException {
        // bellek içi depo için kalıcılık yok
    }
}
