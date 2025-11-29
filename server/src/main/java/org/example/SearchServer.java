package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchServer {
    private static final int PORT = 9999;
    private static final String DATA_DIR = "the_link";

    private static final ExecutorService clientPool = Executors.newCachedThreadPool();
    private static final TermDictionary dictionary = new TermDictionary();
    private static final InvertedIndex globalIndex = new InvertedIndex();

    private static final CustomThreadPool indexingPool = new CustomThreadPool(Runtime.getRuntime().availableProcessors());
    //private static final CustomThreadPool indexingPool = new CustomThreadPool(100);
    public static void main(String[] args) {
        log("System starting...");

        FileWatcherService watcher = new FileWatcherService(DATA_DIR, indexingPool, dictionary, globalIndex);
        new Thread(watcher).start();

        log("Indexing started. Waiting for completion...");

        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        indexingPool.waitUntilTasksFinished();

        log("Indexing COMPLETED. Dictionary size: " + dictionary.size());

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log("SUCCESS. Server is listening on port " + PORT);
            log("Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("New connection from: " + clientSocket.getInetAddress());
                clientPool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            log("CRITICAL ERROR: Could not start server on port " + PORT);
            log("Reason: " + e.getMessage());
        }
    }

    private static void log(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + time + "] SERVER: " + msg);
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String clientIP = socket.getInetAddress().toString();
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                out.println("WELCOME to Parallel Search Server v1.0. Ready.");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("SEARCH ")) {
                        String query = inputLine.substring(7).trim();
                        log("[" + clientIP + "] Query: '" + query + "'");

                        long start = System.nanoTime();
                        int termId = dictionary.getOrAddId(query.toLowerCase());
                        List<DocNode> results = globalIndex.get(termId);
                        long end = System.nanoTime();
                        long durationUs = (end - start) / 1000; // мікросекунди

                        if (results == null || results.isEmpty()) {
                            out.println("NOT_FOUND " + durationUs);
                        } else {
                            StringBuilder sb = new StringBuilder("FOUND ");
                            sb.append(durationUs).append(" ");
                            sb.append(results.size()).append(" ");

                            for (DocNode doc : results) {
                                sb.append(doc.docId).append(":").append(doc.count).append(":[");
                                int limit = Math.min(doc.count, 5);
                                for(int i=0; i<limit; i++) {
                                    sb.append(doc.positions[i]);
                                    if(i < limit-1) sb.append(",");
                                }
                                if(doc.count > 5) sb.append(",...");
                                sb.append("];");
                            }
                            out.println(sb.toString());
                        }

                    } else if (inputLine.equalsIgnoreCase("EXIT")) {
                        log("[" + clientIP + "] Client sent EXIT.");
                        break;
                    }
                }
            } catch (IOException e) {
                log("[" + clientIP + "] Connection lost abruptly.");
            } finally {
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }
}