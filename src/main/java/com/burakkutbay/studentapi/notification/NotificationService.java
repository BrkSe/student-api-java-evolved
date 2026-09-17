package com.burakkutbay.studentapi.notification;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/// Öğrencilere e-posta bildirimlerini sanal bir thread üzerinde arka planda gönderir.
public final class NotificationService implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());
    private static final Duration SMTP_LATENCY = Duration.ofMillis(50);

    public record Notification(String email, String subject, String body) {
    }

    private final BlockingQueue<Notification> queue = new LinkedBlockingQueue<>();
    private final List<String> sentLog = new CopyOnWriteArrayList<>();
    private Thread worker;

    public synchronized void start() {
        if (worker == null) {
            worker = Thread.ofVirtual().name("notification-worker").start(this::processLoop);
        }
    }

    public void enqueue(String email, String subject, String body) {
        queue.add(new Notification(email, subject, body));
    }

    private void processLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                send(queue.take());
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private void send(Notification notification) throws InterruptedException {
        Thread.sleep(SMTP_LATENCY); // SMTP gecikmesini taklit et
        LOG.info(() -> "E-posta gönderildi -> " + notification.email() + " | " + notification.subject());
        sentLog.add(notification.email() + " | " + notification.subject());
    }

    public int pendingCount() {
        return queue.size();
    }

    public List<String> sentLog() {
        return List.copyOf(sentLog);
    }

    /// Kooperatif iptal: worker thread'i kesintiye uğratır.
    @Override
    public synchronized void close() {
        if (worker != null) {
            worker.interrupt();
        }
    }
}
