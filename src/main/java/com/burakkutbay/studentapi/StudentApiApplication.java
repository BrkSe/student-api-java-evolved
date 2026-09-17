package com.burakkutbay.studentapi;

import java.io.IOException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.burakkutbay.studentapi.config.AppConfig;
import com.burakkutbay.studentapi.http.ApiServer;
import com.burakkutbay.studentapi.notification.NotificationService;
import com.burakkutbay.studentapi.scheduler.BackupScheduler;
import com.burakkutbay.studentapi.security.ApiKeyService;
import com.burakkutbay.studentapi.service.GpaCalculator;
import com.burakkutbay.studentapi.service.StudentService;

public final class StudentApiApplication {

    private static final Logger LOG = Logger.getLogger(StudentApiApplication.class.getName());

    public static void main(String[] args) throws Exception {
        var config = AppConfig.load();

        var repository = config.createRepository();
        var notifications = new NotificationService();
        notifications.start();

        var studentService = new StudentService(repository, new GpaCalculator(), notifications);
        var apiKeyService = new ApiKeyService(config.get("api.key", null));

        var backupScheduler = new BackupScheduler();
        backupScheduler.start(repository, config.getDuration("backup.period.millis", Duration.ofMinutes(1)));

        var server = new ApiServer(config.getInt("server.port", 8080), repository, studentService, apiKeyService);
        server.start();

        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().name("shutdown").unstarted(() -> {
            LOG.info("Kapatılıyor...");
            server.close();
            backupScheduler.close();
            notifications.close();
            try {
                repository.flush();
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Son yedekleme başarısız", e);
            }
        }));

        LOG.info(() -> "Student API http://localhost:" + server.port() + " adresinde çalışıyor");
        LOG.info(() -> "API anahtarı: " + apiKeyService.maskedKey());
    }
}
