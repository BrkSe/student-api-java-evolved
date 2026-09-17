package com.burakkutbay.studentapi.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.burakkutbay.studentapi.repository.StudentRepository;
import com.burakkutbay.studentapi.security.ApiKeyService;
import com.burakkutbay.studentapi.service.CourseCatalog;
import com.burakkutbay.studentapi.service.StudentService;
import com.sun.net.httpserver.HttpServer;

public final class ApiServer implements AutoCloseable {

    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ApiServer(int port, StudentRepository repository, StudentService studentService,
                     ApiKeyService apiKeyService) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        // Her istek kendi sanal thread'inde: bloklayan I/O için havuz boyutu ayarlamaya gerek yok.
        server.setExecutor(executor);

        server.createContext(StudentHandler.CONTEXT, new StudentHandler(studentService, apiKeyService));

        server.createContext("/api/health", exchange -> {
            var body = new LinkedHashMap<String, Object>();
            body.put("status", "UP");
            body.put("students", repository.count());
            HttpUtils.sendJson(exchange, 200, body);
        });

        server.createContext("/api/courses", exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                HttpUtils.sendError(exchange, 405, "Desteklenmeyen metot", null);
                return;
            }
            HttpUtils.sendJson(exchange, 200, CourseCatalog.findAll());
        });

        server.createContext("/api/stats", exchange -> {
            try {
                HttpUtils.sendJson(exchange, 200, studentService.statistics());
            } catch (RuntimeException _) {
                HttpUtils.sendError(exchange, 500, "İstatistik hesaplanamadı", null);
            }
        });
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdown();
    }
}
