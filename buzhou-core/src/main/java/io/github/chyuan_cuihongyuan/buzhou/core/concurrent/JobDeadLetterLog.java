package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 作业死信台账（spec 809 / T1119，sidekiq dead set 借鉴——失败作业停尸待勘）：
 * {@link DelayedJobQueue} 一次性语义下异常作业原本「吞+计数」即消失——本台账
 * 经可选观察者（{@link DelayedJobQueue#DelayedJobQueue(java.time.Clock,
 * java.util.function.BiConsumer)}）接收失败明细：环形留痕（封顶
 * {@value #RING_CAPACITY}，挤最老）+ 按作业键聚合（封顶 {@value #AGGREGATE_CAP}，
 * 超限 truncated 不再记）。snapshot 只读；不重投（重投是提交方域）。
 */
public final class JobDeadLetterLog {

    /** 环形明细容量。 */
    public static final int RING_CAPACITY = 64;
    /** 聚合键封顶。 */
    public static final int AGGREGATE_CAP = 64;

    /** 单条死信明细（errorType=异常类简名，message 截 {@value #MSG_MAX} 字符）。 */
    public record DeadJob(String jobKey, long atMillis, String errorType, String message) {
    }

    /** 按作业键聚合行。 */
    public record DeadCount(String jobKey, long count, String lastErrorType) {
    }

    /** 不可变快照。 */
    public record Snapshot(List<DeadJob> recent, List<DeadCount> byKey, long totalFailed, boolean truncated) {
    }

    /** 明细消息截断长度。 */
    public static final int MSG_MAX = 200;

    private final Deque<DeadJob> ring = new ArrayDeque<>(RING_CAPACITY);
    private final ReentrantLock ringLock = new ReentrantLock();
    private final Map<String, long[]> aggregate = new LinkedHashMap<>(); // key → {count}
    private final Map<String, String> lastErrorByKey = new LinkedHashMap<>();
    private volatile boolean truncated;
    private long totalFailed;

    /** 记录一次作业失败（观察者回调域——本方法本身不抛）。 */
    public void recordFailure(String jobKey, Throwable error, long atMillis) {
        if (jobKey == null) {
            jobKey = "";
        }
        String errorType = error == null ? "Unknown" : error.getClass().getSimpleName();
        String message = error == null || error.getMessage() == null ? ""
                : error.getMessage().length() <= MSG_MAX ? error.getMessage()
                : error.getMessage().substring(0, MSG_MAX);
        ringLock.lock();
        try {
            if (ring.size() >= RING_CAPACITY) {
                ring.pollFirst(); // 挤最老
            }
            ring.addLast(new DeadJob(jobKey, atMillis, errorType, message));
            totalFailed++;
            long[] agg = aggregate.get(jobKey);
            if (agg == null) {
                if (aggregate.size() >= AGGREGATE_CAP) {
                    truncated = true; // 键封顶：不再记新键（明细环仍在记）
                } else {
                    aggregate.put(jobKey, new long[]{1});
                    lastErrorByKey.put(jobKey, errorType);
                }
            } else {
                agg[0]++;
                lastErrorByKey.put(jobKey, errorType);
            }
        } finally {
            ringLock.unlock();
        }
    }

    /** 只读快照（recent 新→旧；byKey 按次数降序，封顶 {@value #AGGREGATE_CAP}）。 */
    public Snapshot snapshot() {
        ringLock.lock();
        try {
            List<DeadJob> recent = new ArrayList<>(ring);
            java.util.Collections.reverse(recent);
            List<DeadCount> byKey = new ArrayList<>();
            aggregate.forEach((key, agg) -> byKey.add(new DeadCount(key, agg[0], lastErrorByKey.get(key))));
            byKey.sort(java.util.Comparator.comparingLong(DeadCount::count).reversed());
            return new Snapshot(List.copyOf(recent), List.copyOf(byKey), totalFailed, truncated);
        } finally {
            ringLock.unlock();
        }
    }
}
