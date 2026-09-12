package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 角色工具权限拒绝有界日志（spec 709 / T969，Redis ACL LOG 借鉴）：
 * <ul>
 *   <li><b>环形明细</b>（{@value #RING_CAPACITY} 条封顶）：{timestampMillis, role,
 *       toolName, reason}——最新在前读；reason ∈ {@link Reason#UNAUTHORIZED}（有角色
 *       无权限）/ {@link Reason#UNDEFINED_ROLE}（fail-closed 未定义）；</li>
 *   <li><b>聚合</b>：(role,tool) → 计数，{@value #AGGREGATE_CAP} 键封顶（超限
 *       {@code truncated=true}——基数有界纪律）。</li>
 * </ul>
 * 探针只读（无清零 API——ACL LOG RESET 语义留位）；快照不可变。
 */
public final class ToolDenialLog {

    /** 环形明细容量。 */
    public static final int RING_CAPACITY = 128;
    /** 聚合键封顶。 */
    public static final int AGGREGATE_CAP = 64;

    /** 拒绝原因。 */
    public enum Reason { UNAUTHORIZED, UNDEFINED_ROLE }

    /** 单条拒绝明细（不可变）。 */
    public record Entry(long timestampMillis, String role, String toolName, Reason reason) {
    }

    /** 聚合行（不可变）。 */
    public record DenialCount(String role, String toolName, long count) {
    }

    private static final class Pair {
        final String role;
        final String tool;

        Pair(String role, String tool) {
            this.role = role;
            this.tool = tool;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Pair p && p.role.equals(role) && p.tool.equals(tool);
        }

        @Override
        public int hashCode() {
            return role.hashCode() * 31 + tool.hashCode();
        }
    }

    private final Deque<Entry> ring = new ArrayDeque<>(RING_CAPACITY);
    private final ReentrantLock ringLock = new ReentrantLock();
    private final Map<Pair, AtomicLong> aggregate = new ConcurrentHashMap<>();
    private volatile boolean truncated;

    /** 记录一次拒绝（ring 满 = 最老被挤；聚合键超限置 truncated 不再记）。 */
    public void record(String role, String toolName, Reason reason, long timestampMillis) {
        ringLock.lock();
        try {
            if (ring.size() >= RING_CAPACITY) {
                ring.removeLast();
            }
            ring.addFirst(new Entry(timestampMillis, role, toolName, reason));
        } finally {
            ringLock.unlock();
        }
        Pair pair = new Pair(role, toolName);
        AtomicLong counter = aggregate.get(pair);
        if (counter == null) {
            if (aggregate.size() >= AGGREGATE_CAP) {
                truncated = true;
                return;
            }
            counter = aggregate.computeIfAbsent(pair, k -> new AtomicLong());
        }
        counter.incrementAndGet();
    }

    /** 最近拒绝明细（最新在前；不可变）。 */
    public List<Entry> entries() {
        ringLock.lock();
        try {
            return List.copyOf(new ArrayList<>(ring));
        } finally {
            ringLock.unlock();
        }
    }

    /** (role,tool) 拒绝计数快照（按计数降序；不可变；超限含 truncated 键）。 */
    public Map<String, Long> topDenials() {
        List<Map.Entry<Pair, Long>> sorted = new ArrayList<>();
        aggregate.forEach((pair, counter) -> sorted.add(Map.entry(pair, counter.get())));
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        Map<String, Long> out = new LinkedHashMap<>();
        for (Map.Entry<Pair, Long> e : sorted) {
            out.put(e.getKey().role + "->" + e.getKey().tool, e.getValue());
        }
        if (truncated) {
            out.put("_truncated", Long.valueOf(AGGREGATE_CAP));
        }
        return Map.copyOf(out);
    }

    /** 聚合是否触顶（有 (role,tool) 对未被计入）。 */
    public boolean truncated() {
        return truncated;
    }
}
