package com.burakkutbay.studentapi;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.burakkutbay.studentapi.config.AppConfig;
import com.burakkutbay.studentapi.http.ApiServer;
import com.burakkutbay.studentapi.notification.NotificationService;
import com.burakkutbay.studentapi.repository.StudentRepository;
import com.burakkutbay.studentapi.scheduler.BackupScheduler;
import com.burakkutbay.studentapi.security.ApiKeyService;
import com.burakkutbay.studentapi.service.GpaCalculator;
import com.burakkutbay.studentapi.service.StudentService;

public class StudentApiApplication {

    private static final Logger LOG = Logger.getLogger(StudentApiApplication.class.getName());

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();

        final StudentRepository repository = config.createRepository();
        final NotificationService notificationService = new NotificationService();
        notificationService.start();

        StudentService studentService = new StudentService(repository, new GpaCalculator(), notificationService);
        ApiKeyService apiKeyService = new ApiKeyService(config.get("api.key", null));

        final BackupScheduler backupScheduler = new BackupScheduler();
        backupScheduler.start(repository, config.getInt("backup.period.millis", 60000));

        final ApiServer server = new ApiServer(config.getInt("server.port", 8080), config.getInt("server.threads", 10),
                repository, studentService, apiKeyService);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                LOG.info("Kapatılıyor...");
                server.stop();
                backupScheduler.stop();
                notificationService.stop();
                try {
                    repository.flush();
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Son yedekleme başarısız", e);
                }
            }
        }));

        LOG.info("Student API http://localhost:" + server.getPort() + " adresinde çalışıyor");
        LOG.info("API anahtarı: " + apiKeyService.maskedKey());
    }
}
