package com.burakkutbay.studentapi.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.burakkutbay.studentapi.json.JsonException;
import com.burakkutbay.studentapi.json.JsonParser;
import com.burakkutbay.studentapi.model.Enrollment;
import com.burakkutbay.studentapi.model.Student;
import com.burakkutbay.studentapi.security.ApiKeyService;
import com.burakkutbay.studentapi.service.ConflictException;
import com.burakkutbay.studentapi.service.NotFoundException;
import com.burakkutbay.studentapi.service.StudentService;
import com.burakkutbay.studentapi.service.ValidationException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * /api/students altındaki tüm uç noktaları yönetir.
 *
 * GET    /api/students?department=&status=&sort=
 * POST   /api/students
 * GET    /api/students/{id}
 * PUT    /api/students/{id}
 * DELETE /api/students/{id}
 * POST   /api/students/{id}/enrollments
 * PUT    /api/students/{id}/enrollments/{courseCode}/grade
 * GET    /api/students/{id}/gpa
 * GET    /api/students/{id}/transcript
 */
public class StudentHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(StudentHandler.class.getName());

    public static final String CONTEXT = "/api/students";

    private final StudentService studentService;
    private final ApiKeyService apiKeyService;

    public StudentHandler(StudentService studentService, ApiKeyService apiKeyService) {
        this.studentService = studentService;
        this.apiKeyService = apiKeyService;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        long start = System.currentTimeMillis();
        int status = 200;
        try {
            String[] parts = path.substring(CONTEXT.length()).split("/");
            List segments = new ArrayList();
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].length() > 0) {
                    segments.add(parts[i]);
                }
            }

            if (!method.equals("GET") && !apiKeyService.isValid(exchange.getRequestHeaders().getFirst("X-API-Key"))) {
                status = 401;
                HttpUtils.sendError(exchange, status, "Geçersiz veya eksik API anahtarı", null);
                return;
            }

            if (segments.size() == 0) {
                if (method.equals("GET")) {
                    Map query = HttpUtils.parseQuery(exchange.getRequestURI().getRawQuery());
                    List students = studentService.listStudents((String) query.get("department"),
                            (String) query.get("status"), (String) query.get("sort"));
                    HttpUtils.sendJson(exchange, status, students);
                } else if (method.equals("POST")) {
                    Map body = JsonParser.parseObject(HttpUtils.readBody(exchange));
                    Student created = studentService.createStudent(body);
                    status = 201;
                    exchange.getResponseHeaders().set("Location", CONTEXT + "/" + created.getId());
                    HttpUtils.sendJson(exchange, status, created);
                } else {
                    status = 405;
                    HttpUtils.sendError(exchange, status, "Desteklenmeyen metot: " + method, null);
                }
            } else if (segments.size() == 1) {
                Long id = Long.valueOf((String) segments.get(0));
                if (method.equals("GET")) {
                    HttpUtils.sendJson(exchange, status, studentService.getStudent(id));
                } else if (method.equals("PUT")) {
                    Map body = JsonParser.parseObject(HttpUtils.readBody(exchange));
                    HttpUtils.sendJson(exchange, status, studentService.updateStudent(id, body));
                } else if (method.equals("DELETE")) {
                    studentService.deleteStudent(id);
                    status = 204;
                    HttpUtils.sendNoContent(exchange);
                } else {
                    status = 405;
                    HttpUtils.sendError(exchange, status, "Desteklenmeyen metot: " + method, null);
                }
            } else if (segments.size() == 2) {
                Long id = Long.valueOf((String) segments.get(0));
                String action = (String) segments.get(1);
                if (action.equals("enrollments") && method.equals("POST")) {
                    Map body = JsonParser.parseObject(HttpUtils.readBody(exchange));
                    Enrollment enrollment = studentService.enroll(id, body);
                    status = 201;
                    HttpUtils.sendJson(exchange, status, enrollment);
                } else if (action.equals("gpa") && method.equals("GET")) {
                    HttpUtils.sendJson(exchange, status, studentService.gpaSummary(id));
                } else if (action.equals("transcript") && method.equals("GET")) {
                    HttpUtils.sendText(exchange, status, studentService.transcript(id));
                } else {
                    status = 404;
                    HttpUtils.sendError(exchange, status, "Kaynak bulunamadı: " + path, null);
                }
            } else if (segments.size() == 4 && segments.get(1).equals("enrollments")
                    && segments.get(3).equals("grade") && method.equals("PUT")) {
                Long id = Long.valueOf((String) segments.get(0));
                Map body = JsonParser.parseObject(HttpUtils.readBody(exchange));
                HttpUtils.sendJson(exchange, status,
                        studentService.gradeEnrollment(id, (String) segments.get(2), body));
            } else {
                status = 404;
                HttpUtils.sendError(exchange, status, "Kaynak bulunamadı: " + path, null);
            }
        } catch (ValidationException e) {
            status = 400;
            HttpUtils.sendError(exchange, status, e.getMessage(), e.getErrors());
        } catch (NotFoundException e) {
            status = 404;
            HttpUtils.sendError(exchange, status, e.getMessage(), null);
        } catch (ConflictException e) {
            status = 409;
            HttpUtils.sendError(exchange, status, e.getMessage(), null);
        } catch (JsonException e) {
            status = 400;
            HttpUtils.sendError(exchange, status, "Geçersiz JSON: " + e.getMessage(), null);
        } catch (NumberFormatException e) {
            status = 400;
            HttpUtils.sendError(exchange, status, "Geçersiz öğrenci id", null);
        } catch (Exception e) {
            status = 500;
            LOG.log(Level.SEVERE, "Beklenmeyen hata: " + method + " " + path, e);
            HttpUtils.sendError(exchange, status, "Beklenmeyen bir hata oluştu", null);
        } finally {
            LOG.info(method + " " + path + " -> " + status + " (" + (System.currentTimeMillis() - start) + " ms)");
        }
    }
}
