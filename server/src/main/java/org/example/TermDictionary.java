package org.example;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class TermDictionary {

    private final ConcurrentHashMap<String, Integer> wordToId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> idToWord = new ConcurrentHashMap<>();

    private final AtomicInteger counter = new AtomicInteger(1);


    public int getOrAddId(String word) {
        return wordToId.computeIfAbsent(word, k -> { //якщо ми отримали номер, лямбда не почнеться
            int newId = counter.incrementAndGet();
            idToWord.put(newId, k);
            return newId;
        });
    }


    public String getWord(int id) {
        return idToWord.get(id);
    }

    public int size() {
        return wordToId.size();
    }
}