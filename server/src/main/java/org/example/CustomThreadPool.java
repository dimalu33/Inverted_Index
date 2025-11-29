package org.example;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CustomThreadPool {
    private final int threadCount;
    private final Worker[] threads;
    private final BlockingQueue<Runnable> queue;
    private volatile boolean isRunning = true;

    public CustomThreadPool(int threadCount) {
        this.threadCount = threadCount;
        this.queue = new LinkedBlockingQueue<>();
        this.threads = new Worker[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Worker();
            threads[i].setName("Worker-" + i);
            threads[i].start();
        }
    }

    public void submit(Runnable task) {
        if (isRunning) {
            queue.offer(task);
        }
    }

    public void shutdown() {
        isRunning = false;
        for (Worker w : threads) {
            w.interrupt();
        }
    }

    public int getQueueSize() {
        return queue.size();
    }

    // Перевірка, чи є активні потоки
    public boolean isIdle() {
        if (!queue.isEmpty()) return false;
        for (Worker w : threads) {
            if (w.isWorking) return false;
        }
        return true;
    }

    public void waitUntilTasksFinished() {
        while (!isIdle()) {
            try { Thread.sleep(100); } catch (Exception e) {}
        }
    }

    private class Worker extends Thread {
        // чи зайнятий потік прямо зараз
        public volatile boolean isWorking = false;

        @Override
        public void run() {
            while (isRunning || !queue.isEmpty()) {
                try {
                    Runnable task = queue.take();
                    isWorking = true;
                    try {
                        task.run();
                    } catch (Throwable t) {
                        System.err.println("CRITICAL ERROR in Worker: " + t.getMessage());
                        t.printStackTrace();
                    } finally {
                        isWorking = false;
                    }
                } catch (InterruptedException e) {
                    if (!isRunning) break;
                }
            }
        }
    }
}