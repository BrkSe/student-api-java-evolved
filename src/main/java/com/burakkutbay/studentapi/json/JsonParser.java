package com.burakkutbay.studentapi.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Harici kütüphane kullanmayan basit JSON ayrıştırıcı.
 * Dönüş tipleri: Map, List, String, Long, Double, Boolean veya null.
 */
public class JsonParser {

    private final String text;
    private int pos;

    private JsonParser(String text) {
        if (text == null) {
            throw new JsonException("JSON metni boş olamaz");
        }
        this.text = text;
        this.pos = 0;
    }

    public static Object parse(String text) {
        JsonParser parser = new JsonParser(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != parser.text.length()) {
            throw new JsonException("Beklenmeyen karakter, konum: " + parser.pos);
        }
        return value;
    }

    public static Map parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new JsonException("JSON nesnesi bekleniyordu");
        }
        return (Map) value;
    }

    private Object readValue() {
        skipWhitespace();
        if (pos >= text.length()) {
            throw new JsonException("Beklenmeyen veri sonu");
        }
        char c = text.charAt(pos);
        if (c == '{') {
            return readObject();
        } else if (c == '[') {
            return readArray();
        } else if (c == '"') {
            return readString();
        } else if (c == 't') {
            expect("true");
            return Boolean.TRUE;
        } else if (c == 'f') {
            expect("false");
            return Boolean.FALSE;
        } else if (c == 'n') {
            expect("null");
            return null;
        } else if (c == '-' || Character.isDigit(c)) {
            return readNumber();
        } else {
            throw new JsonException("Beklenmeyen karakter '" + c + "', konum: " + pos);
        }
    }

    private Map readObject() {
        Map result = new LinkedHashMap();
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
            String key = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw new JsonException("':' bekleniyordu, konum: " + pos);
            }
            pos++;
            Object value = readValue();
            result.put(key, value);
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == '}') {
                pos++;
                break;
            } else {
                throw new JsonException("',' veya '}' bekleniyordu, konum: " + pos);
            }
        }
        return result;
    }

    private List readArray() {
        List result = new ArrayList();
        pos++;
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            result.add(readValue());
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
            } else if (c == ']') {
                pos++;
                break;
            } else {
                throw new JsonException("',' veya ']' bekleniyordu, konum: " + pos);
            }
        }
        return result;
    }

    private String readString() {
        StringBuffer sb = new StringBuffer();
        pos++;
        while (true) {
            if (pos >= text.length()) {
                throw new JsonException("Kapanmamış metin");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c == '\\') {
                if (pos >= text.length()) {
                    throw new JsonException("Kapanmamış kaçış karakteri");
                }
                char e = text.charAt(pos++);
                switch (e) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        if (pos + 4 > text.length()) {
                            throw new JsonException("Geçersiz unicode kaçışı");
                        }
                        String hex = text.substring(pos, pos + 4);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException ex) {
                            throw new JsonException("Geçersiz unicode kaçışı: " + hex);
                        }
                        pos += 4;
                        break;
                    default:
                        throw new JsonException("Geçersiz kaçış karakteri: \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
        String number = text.substring(start, pos);
        try {
            if (decimal) {
                return new Double(Double.parseDouble(number));
            }
            return new Long(Long.parseLong(number));
        } catch (NumberFormatException e) {
            throw new JsonException("Geçersiz sayı: " + number);
        }
    }

    private void expect(String literal) {
        if (!text.startsWith(literal, pos)) {
            throw new JsonException("'" + literal + "' bekleniyordu, konum: " + pos);
        }
        pos += literal.length();
    }

    private char peek() {
        if (pos >= text.length()) {
            return 0;
        }
        return text.charAt(pos);
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }
}
