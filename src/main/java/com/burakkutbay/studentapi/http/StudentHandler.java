package com.burakkutbay.studentapi.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.burakkutbay.studentapi.json.JsonException;
import com.burakkutbay.studentapi.json.JsonParser;
import com.burakkutbay.studentapi.security.ApiKeyService;
import com.burakkutbay.studentapi.service.ApiException;
import com.burakkutbay.studentapi.service.StudentService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/// `/api/students` altındaki tüm uç noktaları yönetir.
///
/// | Metot  | Yol                                                  |
/// |--------|------------------------------------------------------|
/// | GET    | `/api/students?department=&status=&sort=`            |
/// | POST   | `/api/students`                                      |
/// | GET    | `/api/students/{id}`                                 |
/// | PUT    | `/api/students/{id}`                                 |
/// | DELETE | `/api/students/{id}`                                 |
/// | POST   | `/api/students/{id}/enrollments`                     |
/// | PUT    | `/api/students/{id}/enrollments/{courseCode}/grade`  |
/// | GET    | `/api/students/{id}/gpa`                             |
/// | GET    | `/api/students/{id}/transcript`                      |
public final class StudentHandler implements HttpHandler {

    private static final Logger LOG = Logger.getLogger(StudentHandler.class.getName());

    public static final String CONTEXT = "/api/students";

    /// Handler'ın ürettiği tüm yanıt türleri; gönderim tek bir exhaustive `switch` ile yapılır.
    sealed interface Response {
        record Json(int status, Object body) implements Response {}
        record Created(Object body, String location) implements Response {}
        record Text(String body) implements Response {}
        record NoContent() implements Response {}
        record Error(int status, String message, List<String> errors) implements Response {}
    }

    private final StudentService studentService;
    private final ApiKeyService apiKeyService;

    public StudentHandler(StudentService studentService, ApiKeyService apiKeyService) {
        this.studentService = studentService;
        this.apiKeyService = apiKeyService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var context = RequestContext.of(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
        try {
            ScopedValue.where(RequestContext.CURRENT, context).call(() -> {
                handleInScope(exchange, context);
                return null;
            });
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void handleInScope(HttpExchange exchange, RequestContext ctx) throws IOException {
        long start = System.nanoTime();
        var response = resolve(exchange, ctx);
        int status = send(exchange, response);
        LOG.info(() -> "%s%s %s -> %d (%d ms)".formatted(RequestContext.logPrefix(), ctx.method(), ctx.path(), status,
                (System.nanoTime() - start) / 1_000_000));
    }

    private Response resolve(HttpExchange exchange, RequestContext ctx) {
        try {
            var segments = Arrays.stream(ctx.path().substring(CONTEXT.length()).split("/"))
                    .filter(Predicate.not(String::isEmpty))
                    .toList();
            if (!ctx.method().equals("GET")
                    && !apiKeyService.isValid(exchange.getRequestHeaders().getFirst("X-API-Key"))) {
                return new Response.Error(401, "Geçersiz veya eksik API anahtarı", null);
            }
            return route(exchange, ctx, segments);
        } catch (ApiException e) {
            return switch (e) {
                case ApiException.Validation v -> new Response.Error(400, v.getMessage(), v.errors());
                case ApiException.NotFound n -> new Response.Error(404, n.getMessage(), null);
                case ApiException.Conflict c -> new Response.Error(409, c.getMessage(), null);
            };
        } catch (JsonException e) {
            return new Response.Error(400, "Geçersiz JSON: " + e.getMessage(), null);
        } catch (NumberFormatException _) {
            return new Response.Error(400, "Geçersiz öğrenci id", null);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, RequestContext.logPrefix() + "Beklenmeyen hata: " + ctx.method() + " " + ctx.path(), e);
            return new Response.Error(500, "Beklenmeyen bir hata oluştu", null);
        }
    }

    private Response route(HttpExchange exchange, RequestContext ctx, List<String> segments) throws IOException {
        var method = ctx.method();
        return switch (segments.size()) {
            case 0 -> switch (method) {
                case "GET" -> {
                    var query = HttpUtils.parseQuery(exchange.getRequestURI().getRawQuery());
                    yield new Response.Json(200, studentService.listStudents(
                            query.get("department"), query.get("status"), query.get("sort")));
                }
                case "POST" -> {
                    var created = studentService.createStudent(JsonParser.parseObject(HttpUtils.readBody(exchange)));
                    yield new Response.Created(created, CONTEXT + "/" + created.id());
                }
                default -> methodNotAllowed(method);
            };
            case 1 -> {
                long id = Long.parseLong(segments.getFirst());
                yield switch (method) {
                    case "GET" -> new Response.Json(200, studentService.getStudent(id));
                    case "PUT" -> new Response.Json(200,
                            studentService.updateStudent(id, JsonParser.parseObject(HttpUtils.readBody(exchange))));
                    case "DELETE" -> {
                        studentService.deleteStudent(id);
                        yield new Response.NoContent();
                    }
                    default -> methodNotAllowed(method);
                };
            }
            case 2 -> {
                long id = Long.parseLong(segments.getFirst());
                yield switch (method + " " + segments.getLast()) {
                    case "POST enrollments" -> new Response.Json(201,
                            studentService.enroll(id, JsonParser.parseObject(HttpUtils.readBody(exchange))));
                    case "GET gpa" -> new Response.Json(200, studentService.gpaSummary(id));
                    case "GET transcript" -> new Response.Text(studentService.transcript(id));
                    default -> notFound(ctx);
                };
            }
            case 4 -> method.equals("PUT") && segments.get(1).equals("enrollments") && segments.getLast().equals("grade")
                    ? new Response.Json(200, studentService.gradeEnrollment(Long.parseLong(segments.getFirst()),
                            segments.get(2), JsonParser.parseObject(HttpUtils.readBody(exchange))))
                    : notFound(ctx);
            default -> notFound(ctx);
        };
    }

    private static int send(HttpExchange exchange, Response response) throws IOException {
        return switch (response) {
            case Response.Json(int status, Object body) -> {
                HttpUtils.sendJson(exchange, status, body);
                yield status;
            }
            case Response.Created(Object body, String location) -> {
                exchange.getResponseHeaders().set("Location", location);
                HttpUtils.sendJson(exchange, 201, body);
                yield 201;
            }
            case Response.Text(String body) -> {
                HttpUtils.sendText(exchange, 200, body);
                yield 200;
            }
            case Response.NoContent() -> {
                HttpUtils.sendNoContent(exchange);
                yield 204;
            }
            case Response.Error(int status, String message, List<String> errors) -> {
                HttpUtils.sendError(exchange, status, message, errors);
                yield status;
            }
        };
    }

    private static Response methodNotAllowed(String method) {
        return new Response.Error(405, "Desteklenmeyen metot: " + method, null);
    }

    private static Response notFound(RequestContext ctx) {
        return new Response.Error(404, "Kaynak bulunamadı: " + ctx.path(), null);
    }
}
