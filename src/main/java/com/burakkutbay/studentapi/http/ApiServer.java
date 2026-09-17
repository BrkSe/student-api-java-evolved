package com.burakkutbay.studentapi.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.burakkutbay.studentapi.repository.StudentRepository;
import com.burakkutbay.studentapi.security.ApiKeyService;
import com.burakkutbay.studentapi.service.CourseCatalog;
import com.burakkutbay.studentapi.service.StudentService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ApiServer {

    private final HttpServer server;
    private final ExecutorService executor;

    public ApiServer(int port, int threads, final StudentRepository repository, final StudentService studentService,
                     ApiKeyService apiKeyService) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        executor = Executors.newFixedThreadPool(threads);
        server.setExecutor(executor);

        server.createContext(StudentHandler.CONTEXT, new StudentHandler(studentService, apiKeyService));

        server.createContext("/api/health", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                Map body = new LinkedHashMap();
                body.put("status", "UP");
                body.put("students", new Integer(repository.count()));
                HttpUtils.sendJson(exchange, 200, body);
            }
        });

        server.createContext("/api/courses", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                if (!exchange.getRequestMethod().equals("GET")) {
                    HttpUtils.sendError(exchange, 405, "Desteklenmeyen metot", null);
                    return;
                }
                HttpUtils.sendJson(exchange, 200, CourseCatalog.findAll());
            }
        });

        server.createContext("/api/stats", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    HttpUtils.sendJson(exchange, 200, studentService.statistics());
                } catch (RuntimeException e) {
                    HttpUtils.sendError(exchange, 500, "İstatistik hesaplanamadı", null);
                }
            }
        });
    }

    public void start() {
        server.start();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
        executor.shutdown();
    }
}
