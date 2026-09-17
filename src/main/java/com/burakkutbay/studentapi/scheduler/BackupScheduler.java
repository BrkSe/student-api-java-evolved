package com.burakkutbay.studentapi.scheduler;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.burakkutbay.studentapi.repository.StudentRepository;

/**
 * Depoyu belirli aralıklarla diske yazar.
 */
public class BackupScheduler {

    private static final Logger LOG = Logger.getLogger(BackupScheduler.class.getName());

    private Timer timer;

    public void start(final StudentRepository repository, long periodMillis) {
        timer = new Timer("backup-timer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    repository.flush();
                    LOG.fine("Yedekleme tamamlandı, kayıt sayısı: " + repository.count());
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Yedekleme başarısız", e);
                }
            }
        }, periodMillis, periodMillis);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
