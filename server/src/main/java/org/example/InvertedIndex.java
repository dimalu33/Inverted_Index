package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;


class DocNode {
    int docId;
    int[] positions;
    int count;       // Реальна кількість записаних позицій

    public DocNode(int docId, int initialPos) {
        this.docId = docId;
        this.positions = new int[4]; // Старт з малого розміру
        this.positions[0] = initialPos;
        this.count = 1;
    }

    public void addPosition(int pos) {
        if (count == positions.length) {
            positions = Arrays.copyOf(positions, positions.length * 3/2);
        }
        positions[count++] = pos;
    }
}

class IndexNode {
    int termId;
    List<DocNode> docs;
    IndexNode next;     // Для колізій

    public IndexNode(int termId) {
        this.termId = termId;
        this.docs = new ArrayList<>(1);
        this.next = null;
    }
}

public class InvertedIndex {

    private final int BUCKET_SIZE = 500009;
    private final IndexNode[] buckets;

    // Блокування ReadWriteLock
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public InvertedIndex() {
        this.buckets = new IndexNode[BUCKET_SIZE];
    }

    private int getBucketIndex(int termId) {
        return Math.abs(termId) % BUCKET_SIZE;
    }

    /**
     * PUT (Локальний, без блокувань).
     */
    public void put(int termId, int docId, int pos) {
        int idx = getBucketIndex(termId);
        IndexNode current = buckets[idx];

        if (current == null) {
            IndexNode newNode = new IndexNode(termId);
            newNode.docs.add(new DocNode(docId, pos));
            buckets[idx] = newNode;
            return;
        }

        while (true) {
            if (current.termId == termId) {
                // Знайшли слово - додаємо позицію
                addPosToNode(current, docId, pos);
                return;
            }
            if (current.next == null) break;
            current = current.next;
        }

        // додаємо нове слово в кінець ланцюжка якщо його там не було
        IndexNode newNode = new IndexNode(termId);
        newNode.docs.add(new DocNode(docId, pos));
        current.next = newNode;
    }

    private void addPosToNode(IndexNode node, int docId, int pos) {
        if (!node.docs.isEmpty()) { //якщо є, то обов'язково має бути останнім
            DocNode lastDoc = node.docs.get(node.docs.size() - 1);
            if (lastDoc.docId == docId) {
                lastDoc.addPosition(pos);
                return;
            }
        }
        // Якщо документ новий
        node.docs.add(new DocNode(docId, pos));
    }

    /**
     * GET (Пошук). Використовує ReadLock (багато читачів одночасно).
     */
    public List<DocNode> get(int termId) {
        lock.readLock().lock();
        try {
            int idx = getBucketIndex(termId);
            IndexNode current = buckets[idx];
            while (current != null) {
                if (current.termId == termId) {
                    return current.docs;
                }
                current = current.next;
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * MERGE (Злиття). Використовує WriteLock
     */
    public void merge(InvertedIndex localIndex) {
        lock.writeLock().lock();
        try {
            for (IndexNode node : localIndex.buckets) {
                IndexNode current = node;
                while (current != null) {
                    for (DocNode localDoc : current.docs) {

                        for (int i = 0; i < localDoc.count; i++) {
                            this.put(current.termId, localDoc.docId, localDoc.positions[i]);
                        }
                    }
                    current = current.next;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}