package com.burakkutbay.studentapi.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.burakkutbay.studentapi.model.Student;

public class InMemoryStudentRepository implements StudentRepository {

    private final Map<Long, Student> students = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public List<Student> findAll() {
        return students.values().stream()
                .sorted(Comparator.comparingLong(Student::id))
                .toList();
    }

    @Override
    public Optional<Student> findById(long id) {
        return Optional.ofNullable(students.get(id));
    }

    @Override
    public Student save(Student student) {
        var stored = student.id() == null
                ? student.withId(sequence.incrementAndGet())
                : student;
        sequence.accumulateAndGet(stored.id(), Math::max);
        students.put(stored.id(), stored);
        return stored;
    }

    @Override
    public boolean delete(long id) {
        return students.remove(id) != null;
    }

    @Override
    public int count() {
        return students.size();
    }
}
