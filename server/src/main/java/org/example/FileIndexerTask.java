package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileIndexerTask implements Runnable {
    private final Path filePath;
    private final int docId;
    private final TermDictionary dictionary;
    private final InvertedIndex globalIndex;

    public FileIndexerTask(Path filePath, int docId, TermDictionary dictionary, InvertedIndex globalIndex) {
        this.filePath = filePath;
        this.docId = docId;
        this.dictionary = dictionary;
        this.globalIndex = globalIndex;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        // System.out.println(" [" + threadName + "] START parsing: " + filePath.getFileName());

        long start = System.currentTimeMillis();
        int wordsCount = 0;

        try {
            InvertedIndex localIndex = new InvertedIndex();

            var decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.IGNORE)
                    .onUnmappableCharacter(CodingErrorAction.IGNORE);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(filePath), decoder))) {
                int ch;
                int globalPos = 0; // Лічильник позиції слова
                StringBuilder sb = new StringBuilder();

                while ((ch = reader.read()) != -1) {
                    char c = (char) ch;

                    if (Character.isLetterOrDigit(c)) {
                        sb.append(Character.toLowerCase(c));
                    } else {
                        if (sb.length() > 0) {
                            processWord(sb.toString(), localIndex, globalPos);
                            globalPos++;
                            wordsCount++;
                            sb.setLength(0); // Очищаємо буфер
                        }
                    }
                }

                // якщо файл закінчився не роздільником
                if (sb.length() > 0) {
                    processWord(sb.toString(), localIndex, globalPos);
                    wordsCount++;
                }
            }

            // Злиття
            globalIndex.merge(localIndex);

            long endTime = System.currentTimeMillis();
            System.out.println(" [" + threadName + "] DONE: " + filePath.getFileName() +
                    " (Words: " + wordsCount + ", Time: " + (endTime - start) + "ms)");

        } catch (IOException e) {
            System.err.println("Error processing " + filePath + ": " + e.getMessage());
        } catch (OutOfMemoryError e) {
            System.err.println(" [" + threadName + "] CRITICAL: Out Of Memory processing " + filePath.getFileName());
        }
    }

    private void processWord(String word, InvertedIndex localIndex, int pos) {
        int termId = dictionary.getOrAddId(word);
        localIndex.put(termId, docId, pos);
    }
}