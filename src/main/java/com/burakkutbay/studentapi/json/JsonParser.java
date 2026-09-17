package com.burakkutbay.studentapi.json;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Harici kütüphane kullanmayan basit JSON ayrıştırıcı.
///
/// Dönüş tipleri: `Map<String, Object>`, `List<Object>`, `String`, `Long`, `Double`, `Boolean` veya `null`.
public final class JsonParser {

    private final String text;
    private int pos;

    private JsonParser(String text) {
        if (text == null) {
            throw new JsonException("JSON metni boş olamaz");
        }
        this.text = text;
    }

    public static Object parse(String text) {
        var parser = new JsonParser(text);
        parser.skipWhitespace();
        var value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != parser.text.length()) {
            throw new JsonException("Beklenmeyen karakter, konum: " + parser.pos);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        if (parse(text) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new JsonException("JSON nesnesi bekleniyordu");
    }

    private Object readValue() {
        skipWhitespace();
        if (pos >= text.length()) {
            throw new JsonException("Beklenmeyen veri sonu");
        }
        char c = text.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> readNumber();
            default -> throw new JsonException("Beklenmeyen karakter '" + c + "', konum: " + pos);
        };
    }

    private Map<String, Object> readObject() {
        var result = new LinkedHashMap<String, Object>();
        pos++;
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw new JsonException("Anahtar bekleniyordu, konum: " + pos);
            }
            var key = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw new JsonException("':' bekleniyordu, konum: " + pos);
            }
            pos++;
            result.put(key, readValue());
            skipWhitespace();
            switch (peek()) {
                case ',' -> pos++;
                case '}' -> {
                    pos++;
                    return result;
                }
                default -> throw new JsonException("',' veya '}' bekleniyordu, konum: " + pos);
            }
        }
    }

    private List<Object> readArray() {
        var result = new ArrayList<>();
        pos++;
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            result.add(readValue());
            skipWhitespace();
            switch (peek()) {
                case ',' -> pos++;
                case ']' -> {
                    pos++;
                    return result;
                }
                default -> throw new JsonException("',' veya ']' bekleniyordu, konum: " + pos);
            }
        }
    }

    private String readString() {
        var sb = new StringBuilder();
        pos++;
        while (true) {
            if (pos >= text.length()) {
                throw new JsonException("Kapanmamış metin");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= text.length()) {
                throw new JsonException("Kapanmamış kaçış karakteri");
            }
            char escape = text.charAt(pos++);
            sb.append(switch (escape) {
                case '"' -> '"';
                case '\\' -> '\\';
                case '/' -> '/';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> readUnicodeEscape();
                default -> throw new JsonException("Geçersiz kaçış karakteri: \\" + escape);
            });
        }
    }

    private char readUnicodeEscape() {
        if (pos + 4 > text.length()) {
            throw new JsonException("Geçersiz unicode kaçışı");
        }
        var hex = text.substring(pos, pos + 4);
        if (!hex.chars().allMatch(HexFormat::isHexDigit)) {
            throw new JsonException("Geçersiz unicode kaçışı: " + hex);
        }
        pos += 4;
        return (char) HexFormat.fromHexDigits(hex);
    }

    private Object readNumber() {
        int start = pos;
        boolean decimal = false;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (Character.isDigit(c) || c == '-' || c == '+') {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E') {
                decimal = true;
                pos++;
            } else {
                break;
            }
        }
        var number = text.substring(start, pos);
        try {
            return decimal ? (Object) Double.parseDouble(number) : (Object) Long.parseLong(number);
        } catch (NumberFormatException _) {
            throw new JsonException("Geçersiz sayı: " + number);
        }
    }

    private Object readLiteral(String literal, Object value) {
        if (!text.startsWith(literal, pos)) {
            throw new JsonException("'" + literal + "' bekleniyordu, konum: " + pos);
        }
        pos += literal.length();
        return value;
    }

    private char peek() {
        return pos < text.length() ? text.charAt(pos) : 0;
    }

    private void skipWhitespace() {
        while (pos < text.length() && " \t\n\r".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
    }
}
