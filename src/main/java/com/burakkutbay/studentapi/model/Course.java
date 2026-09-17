package com.burakkutbay.studentapi.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.burakkutbay.studentapi.json.JsonSerializable;

public class Course implements JsonSerializable {

    private String code;
    private String name;
    private int credits;
    private String department;

    public Course() {
    }

    public Course(String code, String name, int credits, String department) {
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.department = department;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Map toMap() {
        Map map = new LinkedHashMap();
        map.put("code", code);
        map.put("name", name);
        map.put("credits", new Integer(credits));
        map.put("department", department);
        return map;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Course other = (Course) o;
        if (credits != other.credits) {
            return false;
        }
        if (code != null ? !code.equals(other.code) : other.code != null) {
            return false;
        }
        if (name != null ? !name.equals(other.name) : other.name != null) {
            return false;
        }
        return department != null ? department.equals(other.department) : other.department == null;
    }

    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + credits;
        result = 31 * result + (department != null ? department.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "Course{code='" + code + "', name='" + name + "', credits=" + credits
                + ", department='" + department + "'}";
    }
}
