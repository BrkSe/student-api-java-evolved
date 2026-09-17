package com.burakkutbay.studentapi.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.burakkutbay.studentapi.json.JsonWriter;
import com.sun.net.httpserver.HttpExchange;

public final class HttpUtils {

    private HttpUtils() {
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        try (var in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), UTF_8);
        }
    }

    public static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return Map.of();
        }
        return Arrays.stream(rawQuery.split("&"))
                .filter(Predicate.not(String::isEmpty))
                .map(pair -> {
                    int idx = pair.indexOf('=');
                    return idx > 0
                            ? Map.entry(decode(pair.substring(0, idx)), decode(pair.substring(idx + 1)))
                            : Map.entry(decode(pair), "");
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, last) -> last));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, UTF_8);
    }

    public static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        send(exchange, status, "application/json; charset=utf-8", JsonWriter.toJson(body));
    }

    public static void sendText(HttpExchange exchange, int status, String body) throws IOException {
        send(exchange, status, "text/plain; charset=utf-8", body);
    }

    public static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }

    public static void sendError(HttpExchange exchange, int status, String message, List<String> errors)
            throws IOException {
        var body = new LinkedHashMap<String, Object>();
        body.put("status", status);
        body.put("error", reasonPhrase(status));
        body.put("message", message);
        if (errors != null) {
            body.put("errors", errors);
        }
        sendJson(exchange, status, body);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        var bytes = body.getBytes(UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static String reasonPhrase(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };
    }
}
