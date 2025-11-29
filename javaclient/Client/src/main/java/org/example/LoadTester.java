package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTester {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 9999;

    private static final String[] DICTIONARY = {
            "the", "of", "and", "to", "a", "in", "is", "it", "you", "that",
            "he", "was", "for", "on", "are", "with", "as", "I", "his", "they",
            "be", "at", "one", "have", "this", "from", "or", "had", "by", "hot",
            "word", "but", "what", "some", "we", "can", "out", "other", "were",
            "all", "there", "when", "up", "use", "your", "how", "said", "an",
            "java", "code", "computer", "system", "search", "engine", "server"
    };

    static class TestScenario {
        int users;
        int requestsPerUser;
        public TestScenario(int users, int requestsPerUser) {
            this.users = users;
            this.requestsPerUser = requestsPerUser;
        }
        public int totalRequests() { return users * requestsPerUser; }
    }

    public static void main(String[] args) throws InterruptedException {
        List<TestScenario> scenarios = new ArrayList<>();
        // Сценарії
        scenarios.add(new TestScenario(10, 10));    // Розминка
        scenarios.add(new TestScenario(50, 10));   // Середній
        scenarios.add(new TestScenario(1000, 3));  // Високий
        scenarios.add(new TestScenario(4000, 3));  // Стрес

        System.out.println("==========================================");
        System.out.println("AUTOMATED LOAD TESTING (with Latency)");
        System.out.println("Target: " + SERVER_IP + ":" + SERVER_PORT);
        System.out.println("==========================================\n");

        for (TestScenario scenario : scenarios) {
            runTest(scenario);
            System.out.println("Cooldown 5 seconds...\n");
            Thread.sleep(5000);
        }
    }

    private static void runTest(TestScenario scenario) throws InterruptedException {
        int totalReqs = scenario.totalRequests();
        System.out.printf(">>> RUNNING TEST: %d Users, %d Req/User (Total: %d)%n",
                scenario.users, scenario.requestsPerUser, totalReqs);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        // Змінна для суми часу всіх запитів (у мікросекундах)
        AtomicLong totalLatencyUs = new AtomicLong(0);

        long startTestTime = System.currentTimeMillis();
        ExecutorService pool = Executors.newFixedThreadPool(scenario.users);

        for (int i = 0; i < scenario.users; i++) {
            pool.submit(() -> {
                try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    in.readLine(); // Пропускаємо Welcome message
                    Random rand = new Random();

                    for (int j = 0; j < scenario.requestsPerUser; j++) {
                        String word = DICTIONARY[rand.nextInt(DICTIONARY.length)];

                        // --- ЗАМІР ЧАСУ ---
                        long reqStart = System.nanoTime();

                        out.println("SEARCH " + word);
                        String resp = in.readLine();

                        long reqEnd = System.nanoTime();
                        // ------------------

                        if (resp != null) {
                            success.incrementAndGet();
                            // Додаємо тривалість запиту (в мікросекундах) до загальної суми
                            totalLatencyUs.addAndGet((reqEnd - reqStart) / 1000);
                        } else {
                            fail.incrementAndGet();
                        }
                    }
                    out.println("EXIT");
                } catch (Exception e) {
                    fail.incrementAndGet();
                }
            });
        }

        pool.shutdown();
        boolean finished = pool.awaitTermination(300, TimeUnit.SECONDS);
        long endTestTime = System.currentTimeMillis();
        long durationMs = endTestTime - startTestTime;

        // Розрахунки
        double rps = (durationMs > 0) ? (double) success.get() / (durationMs / 1000.0) : 0;

        // Середній час відповіді = Загальний час / Кількість успішних
        double avgLatencyMs = 0;
        if (success.get() > 0) {
            avgLatencyMs = (double) totalLatencyUs.get() / success.get() / 1000.0; // переводимо мкс в мс
        }

        System.out.println("   [RESULT]");
        System.out.println("   Total Time:  " + durationMs + " ms");
        System.out.println("   Success:     " + success.get());
        System.out.println("   Failed:      " + fail.get());
        System.out.printf("   RPS:         %.2f req/sec%n", rps);
        System.out.printf("   Avg Latency: %.3f ms%n", avgLatencyMs); // Скільки чекав клієнт в середньому

        if (!finished) System.err.println("   WARNING: Test timed out!");
        System.out.println("------------------------------------------");
    }
}