package com.burakkutbay.studentapi.model;

public final class StudentStatusUtil {

    public static final int UNKNOWN = -1;

    public static final int[] ALL = new int[] {
            StudentStatus.ACTIVE, StudentStatus.GRADUATED, StudentStatus.SUSPENDED, StudentStatus.WITHDRAWN
    };

    private StudentStatusUtil() {
    }

    public static String toLabel(int status) {
        String label;
        switch (status) {
            case StudentStatus.ACTIVE:
                label = "ACTIVE";
                break;
            case StudentStatus.GRADUATED:
                label = "GRADUATED";
                break;
            case StudentStatus.SUSPENDED:
                label = "SUSPENDED";
                break;
            case StudentStatus.WITHDRAWN:
                label = "WITHDRAWN";
                break;
            default:
                label = "UNKNOWN";
        }
        return label;
    }

    public static int fromLabel(String label) {
        if (label == null) {
            return UNKNOWN;
        }
        String l = label.trim();
        if (l.equalsIgnoreCase("ACTIVE")) {
            return StudentStatus.ACTIVE;
        } else if (l.equalsIgnoreCase("GRADUATED")) {
            return StudentStatus.GRADUATED;
        } else if (l.equalsIgnoreCase("SUSPENDED")) {
            return StudentStatus.SUSPENDED;
        } else if (l.equalsIgnoreCase("WITHDRAWN")) {
            return StudentStatus.WITHDRAWN;
        }
        return UNKNOWN;
    }
}
