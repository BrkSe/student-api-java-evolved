package com.burakkutbay.studentapi.json;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import com.burakkutbay.studentapi.util.DateUtils;

/**
 * Nesneleri JSON metnine dönüştürür.
 */
public final class JsonWriter {

    private JsonWriter() {
    }

    public static String toJson(Object value) {
        StringBuffer sb = new StringBuffer();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuffer sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                sb.append("null");
            } else {
                sb.append(String.valueOf(d));
            }
        } else if (value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof Boolean) {
            sb.append(((Boolean) value).booleanValue() ? "true" : "false");
        } else if (value instanceof Map) {
            Map map = (Map) value;
            sb.append('{');
            Iterator it = map.entrySet().iterator();
            boolean first = true;
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, String.valueOf(entry.getKey()));
                sb.append(':');
                write(sb, entry.getValue());
            }
            sb.append('}');
        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            sb.append('[');
            Iterator it = collection.iterator();
            boolean first = true;
            while (it.hasNext()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(sb, it.next());
            }
            sb.append(']');
        } else if (value instanceof Date) {
            writeString(sb, DateUtils.format((Date) value));
        } else if (value instanceof JsonSerializable) {
            write(sb, ((JsonSerializable) value).toMap());
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeString(StringBuffer sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20) {
                String hex = Integer.toHexString(c);
                sb.append("\\u");
                for (int j = hex.length(); j < 4; j++) {
                    sb.append('0');
                }
                sb.append(hex);
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }
}
