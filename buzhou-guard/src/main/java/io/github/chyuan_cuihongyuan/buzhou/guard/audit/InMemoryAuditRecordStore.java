package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 审计记录有界环形内存存储（impl-39 / spec 13 §T64）：容量满即逐出最旧记录
 * （审计链最重的是「近期可验」，全量历史归 JDBC 实现持久化）。
 *
 * <p>与 impl-36 InMemory 有界化同一哲学：内存占用有上界、逐出可见（{@link #evicted()}
 * 计数）。默认容量 4096 条。
 */
public final class InMemoryAuditRecordStore implements AuditRecordStore {

    public static final int DEFAULT_CAPACITY = 4096;

    private final int capacity;
    private final Deque<AgentAuditRecord> ring;
    private long appended;
    private long evicted;

    public InMemoryAuditRecordStore() {
        this(DEFAULT_CAPACITY);
    }

    public InMemoryAuditRecordStore(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("审计环形容量必须为正（收到 " + capacity + "）");
        }
        this.capacity = capacity;
        this.ring = new ArrayDeque<>(Math.min(capacity, 1024));
    }

    @Override
    public synchronized void append(AgentAuditRecord record) {
        if (ring.size() == capacity) {
            ring.pollFirst();
            evicted++;
        }
        ring.addLast(record);
        appended++;
    }

    @Override
    public synchronized List<AgentAuditRecord> loadAll() {
        return new ArrayList<>(ring);
    }

    @Override
    public synchronized long count() {
        return ring.size();
    }

    /** 累计追加数（含已被逐出的）。 */
    public synchronized long appended() {
        return appended;
    }

    /** 因容量被逐出的记录数（逐出可见性）。 */
    public synchronized long evicted() {
        return evicted;
    }
}
