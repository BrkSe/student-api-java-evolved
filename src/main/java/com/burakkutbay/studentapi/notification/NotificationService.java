package com.burakkutbay.studentapi.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Öğrencilere e-posta bildirimlerini arka planda gönderir.
 */
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    private final LinkedList queue = new LinkedList();

    private final List sentLog = Collections.synchronizedList(new ArrayList());

    private volatile boolean running = false;

    private Thread worker;

    public void start() {
        running = true;
        worker = new Thread(new Runnable() {
            public void run() {
                processLoop();
            }
        }, "notification-worker");
        worker.setDaemon(true);
        worker.start();
    }

    public void enqueue(String email, String subject, String body) {
        synchronized (queue) {
            queue.addLast(new String[] {email, subject, body});
            queue.notifyAll();
        }
    }

    private void processLoop() {
        while (running) {
            String[] message;
            synchronized (queue) {
                while (queue.isEmpty() && running) {
                    try {
                        queue.wait(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (!running) {
                    return;
                }
                message = (String[]) queue.removeFirst();
            }
            send(message);
        }
    }

    private void send(String[] message) {
        try {
            // SMTP gecikmesini taklit et
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // yoksay
        }
        LOG.info("E-posta gönderildi -> " + message[0] + " | " + message[1]);
        sentLog.add(message[0] + " | " + message[1]);
    }

    public int pendingCount() {
        synchronized (queue) {
            return queue.size();
        }
    }

    public List getSentLog() {
        return sentLog;
    }

    public void stop() {
        running = false;
        synchronized (queue) {
            queue.notifyAll();
        }
    }
}
