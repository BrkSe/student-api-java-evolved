package com.burakkutbay.studentapi.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.burakkutbay.studentapi.json.JsonSerializable;

public class Enrollment implements JsonSerializable {

    private String courseCode;
    private String semester;
    private String letterGrade;

    public Enrollment() {
    }

    public Enrollment(String courseCode, String semester, String letterGrade) {
        this.courseCode = courseCode;
        this.semester = semester;
        this.letterGrade = letterGrade;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getLetterGrade() {
        return letterGrade;
    }

    public void setLetterGrade(String letterGrade) {
        this.letterGrade = letterGrade;
    }

    public boolean isGraded() {
        return letterGrade != null;
    }

    public Map toMap() {
        Map map = new LinkedHashMap();
        map.put("courseCode", courseCode);
        map.put("semester", semester);
        map.put("letterGrade", letterGrade);
        return map;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Enrollment)) {
            return false;
        }
        Enrollment other = (Enrollment) o;
        if (courseCode != null ? !courseCode.equals(other.courseCode) : other.courseCode != null) {
            return false;
        }
        if (semester != null ? !semester.equals(other.semester) : other.semester != null) {
            return false;
        }
        return letterGrade != null ? letterGrade.equals(other.letterGrade) : other.letterGrade == null;
    }

    public int hashCode() {
        int result = courseCode != null ? courseCode.hashCode() : 0;
        result = 31 * result + (semester != null ? semester.hashCode() : 0);
        result = 31 * result + (letterGrade != null ? letterGrade.hashCode() : 0);
        return result;
    }

    public String toString() {
        return "Enrollment{courseCode='" + courseCode + "', semester='" + semester
                + "', letterGrade='" + letterGrade + "'}";
    }
}
