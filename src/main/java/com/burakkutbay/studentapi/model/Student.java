package com.burakkutbay.studentapi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.burakkutbay.studentapi.json.JsonSerializable;
import com.burakkutbay.studentapi.util.DateUtils;

public class Student implements Serializable, JsonSerializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String studentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private Date birthDate;
    private String department;
    private int status = StudentStatus.ACTIVE;
    private List enrollments = new ArrayList();

    public Student() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(List enrollments) {
        this.enrollments = enrollments;
    }

    public void addEnrollment(Enrollment enrollment) {
        enrollments.add(enrollment);
    }

    public Map toMap() {
        Map map = new LinkedHashMap();
        map.put("id", id);
        map.put("studentNumber", studentNumber);
        map.put("firstName", firstName);
        map.put("lastName", lastName);
        map.put("fullName", getFullName());
        map.put("email", email);
        map.put("birthDate", DateUtils.format(birthDate));
        map.put("age", birthDate == null ? null : new Integer(DateUtils.calculateAge(birthDate)));
        map.put("department", department);
        map.put("status", StudentStatusUtil.toLabel(status));
        List enrollmentMaps = new ArrayList();
        for (Iterator it = enrollments.iterator(); it.hasNext();) {
            Enrollment enrollment = (Enrollment) it.next();
            enrollmentMaps.add(enrollment.toMap());
        }
        map.put("enrollments", enrollmentMaps);
        return map;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Student other = (Student) o;
        return id != null ? id.equals(other.id) : other.id == null;
    }

    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public String toString() {
        return "Student{id=" + id + ", studentNumber='" + studentNumber + "', firstName='" + firstName
                + "', lastName='" + lastName + "', email='" + email + "', birthDate=" + birthDate
                + ", department='" + department + "', status=" + status + ", enrollments=" + enrollments + "}";
    }
}
