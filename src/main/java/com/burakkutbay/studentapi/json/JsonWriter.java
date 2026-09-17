package com.burakkutbay.studentapi.json;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;

/// Nesneleri JSON metnine dönüştürür.
public final class JsonWriter {

    private static final HexFormat HEX = HexFormat.of();

    private JsonWriter() {
    }

    public static String toJson(Object value) {
        var sb = new StringBuilder();
        write(sb, value);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object value) {
        switch (value) {
            case null -> sb.append("null");
            case String s -> writeString(sb, s);
            case Double d when d.isNaN() || d.isInfinite() -> sb.append("null");
            case Float f when f.isNaN() || f.isInfinite() -> sb.append("null");
            case Double d -> sb.append(d.doubleValue());
            case Float f -> sb.append(f.doubleValue());
            case Number n -> sb.append(n);
            case Boolean b -> sb.append(b.booleanValue());
            case Map<?, ?> map -> writeMap(sb, map);
            case Collection<?> collection -> writeCollection(sb, collection);
            case LocalDate date -> writeString(sb, date.toString());
            case Enum<?> constant -> writeString(sb, constant.name());
            case JsonSerializable serializable -> write(sb, serializable.toMap());
            default -> writeString(sb, value.toString());
        }
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeString(sb, String.valueOf(entry.getKey()));
            sb.append(':');
            write(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeCollection(StringBuilder sb, Collection<?> collection) {
        sb.append('[');
        boolean first = true;
        for (var item : collection) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            write(sb, item);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append("\\u").append(HEX.toHexDigits(c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
