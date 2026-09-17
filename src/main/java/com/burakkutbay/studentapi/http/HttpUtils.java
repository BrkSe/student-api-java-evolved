package com.burakkutbay.studentapi.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.burakkutbay.studentapi.json.JsonWriter;
import com.sun.net.httpserver.HttpExchange;

public final class HttpUtils {

    private HttpUtils() {
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        InputStream in = exchange.getRequestBody();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    public static Map parseQuery(String query) {
        Map result = new HashMap();
        if (query == null || query.length() == 0) {
            return result;
        }
        String[] pairs = query.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            int idx = pair.indexOf('=');
            try {
                if (idx > 0) {
                    result.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                            URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                } else if (pair.length() > 0) {
                    result.put(URLDecoder.decode(pair, "UTF-8"), "");
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 desteklenmiyor", e);
            }
        }
        return result;
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

    public static void sendError(HttpExchange exchange, int status, String message, List errors) throws IOException {
        Map body = new java.util.LinkedHashMap();
        body.put("status", new Integer(status));
        body.put("error", reasonPhrase(status));
        body.put("message", message);
        if (errors != null) {
            body.put("errors", errors);
        }
        sendJson(exchange, status, body);
    }

    private static void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream out = exchange.getResponseBody();
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }

    public static String reasonPhrase(int status) {
        switch (status) {
            case 200:
                return "OK";
            case 201:
                return "Created";
            case 400:
                return "Bad Request";
            case 401:
                return "Unauthorized";
            case 404:
                return "Not Found";
            case 405:
                return "Method Not Allowed";
            case 409:
                return "Conflict";
            case 500:
                return "Internal Server Error";
            default:
                return "Unknown";
        }
    }
}
