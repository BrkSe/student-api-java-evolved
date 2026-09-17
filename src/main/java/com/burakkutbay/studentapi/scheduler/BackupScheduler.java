package com.burakkutbay.studentapi.scheduler;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.burakkutbay.studentapi.repository.StudentRepository;

/// Depoyu belirli aralıklarla diske yazar.
public final class BackupScheduler implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(BackupScheduler.class.getName());

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("backup-timer").daemon().factory());

    public void start(StudentRepository repository, Duration period) {
        executor.scheduleAtFixedRate(() -> {
            try {
                repository.flush();
                LOG.fine(() -> "Yedekleme tamamlandı, kayıt sayısı: " + repository.count());
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Yedekleme başarısız", e);
            }
        }, period.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
