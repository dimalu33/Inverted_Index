package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9999;

    public static void main(String[] args) {
        System.out.println("=== Java Search Client Starting ===");

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            String welcome = in.readLine();
            System.out.println("\nSERVER SAYS: " + welcome);
            System.out.println("================================================");

            while (true) {
                System.out.print("\nSearch > ");
                String query = scanner.nextLine().trim();

                if (query.isEmpty()) continue;
                if (query.equalsIgnoreCase("quit")) {
                    out.println("EXIT");
                    break;
                }

                // Відправка
                out.println("SEARCH " + query);

                // Отримання
                String response = in.readLine();

                if (response == null) {
                    System.err.println("Server closed connection.");
                    break;
                }

                // Красивий вивід
                if (response.startsWith("NOT_FOUND")) {
                    String time = response.split(" ")[1];
                    System.out.println(" [x] Not found. (Took " + time + " us)");
                }
                else if (response.startsWith("FOUND")) {
                    String[] parts = response.split(" ", 4);
                    String time = parts[1];
                    String count = parts[2];
                    String rawData = parts.length > 3 ? parts[3] : "";

                    System.out.println(" [v] Found " + count + " documents in " + time + " us.");

                    String[] docs = rawData.split(";");

                    // Показуємо перші 3
                    System.out.println(" --- Top Results ---");
                    int preview = Math.min(docs.length, 3);
                    for (int i = 0; i < preview; i++) {
                        printDoc(docs[i]);
                    }

                    // Логіка розгортання списку
                    if (docs.length > 3) {
                        System.out.print(" Show full list (" + (docs.length - 3) + " more)? [y/n]: ");
                        String choice = scanner.nextLine();
                        if (choice.equalsIgnoreCase("y")) {
                            for (int i = 3; i < docs.length; i++) {
                                printDoc(docs[i]);
                            }
                        }
                    }
                } else {
                    System.out.println("Server response: " + response);
                }
            }

        } catch (IOException e) {
            System.err.println("Connection Error: " + e.getMessage());
            System.err.println("Check IP (" + SERVER_IP + ") and Firewall settings.");
        }
    }

    private static void printDoc(String raw) {
        // raw: docId:count:[pos1,pos2]
        try {
            String[] p = raw.split(":", 3);
            System.out.printf(" Doc #%-4s | Matches: %-3s | Pos: %s%n", p[0], p[1], p[2]);
        } catch (Exception e) {}
    }
}