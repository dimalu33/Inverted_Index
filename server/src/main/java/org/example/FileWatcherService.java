package org.example;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Фоновий процес, що слідкує за папкою.
 */
public class FileWatcherService implements Runnable {
    private final String folderPath;
    private final CustomThreadPool threadPool;
    private final TermDictionary dictionary;
    private final InvertedIndex globalIndex;
    private final AtomicInteger docIdCounter;

    // Set для запам'ятовування вже оброблених файлів
    private final Set<String> processedFiles = new HashSet<>();
    private volatile boolean isRunning = true;

    public FileWatcherService(String folderPath, CustomThreadPool threadPool,
                              TermDictionary dictionary, InvertedIndex globalIndex) {
        this.folderPath = folderPath;
        this.threadPool = threadPool;
        this.dictionary = dictionary;
        this.globalIndex = globalIndex;
        this.docIdCounter = new AtomicInteger(1);
    }

    public void stopService() {
        isRunning = false;
    }

    @Override
    public void run() {
        System.out.println("File Watcher started on: " + folderPath);

        while (isRunning) {
            try {
                Files.walk(Paths.get(folderPath))
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            if (!processedFiles.contains(fileName)) {
                                System.out.println("[NEW FILE FOUND] -> " + fileName);
                                processedFiles.add(fileName);

                                int newDocId = docIdCounter.getAndIncrement();
                                threadPool.submit(new FileIndexerTask(path, newDocId, dictionary, globalIndex));
                            }
                        });

                Thread.sleep(5000);

            } catch (IOException e) {
                System.err.println("Error accessing folder: " + e.getMessage());
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}