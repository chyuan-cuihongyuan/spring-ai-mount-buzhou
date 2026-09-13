package io.github.chyuan_cuihongyuan.buzhou.mcp.breaker;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MCP 断路器变迁台账（spec 814 / T1129，702 CircuitTransitionJournal 同模式
 * 扩散到 MCP server 聚合熔断）：变迁环形留痕（封顶 {@value #RING_CAPACITY}，
 * 挤最老）+ per-server trips（→OPEN）/recovers（→CLOSED）聚合（封顶
 * {@value #AGGREGATE_CAP}，超限 truncated）——「最近哪台 server 在反复熔断、
 * 多久恢复」不再散落。快照不可变；只读无清零（ACL LOG RESET 语义留位）。
 */
public final class McpBreakerTransitionJournal {

    /** 环形明细容量。 */
    public static final int RING_CAPACITY = 64;
    /** 聚合键封顶。 */
    public static final int AGGREGATE_CAP = 32;

    /** 单条变迁明细。 */
    public record Transition(String server, String from, String to, long atEpochMs) {
    }

    /** per-server 聚合行。 */
    public record ServerCounts(String server, long trips, long recovers, long transitions) {
    }

    /** 不可变快照。 */
    public record Report(List<Transition> recent, List<ServerCounts> byServer, long dropped, int capacity) {
    }

    private final Deque<Transition> ring = new ArrayDeque<>(RING_CAPACITY);
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, long[]> counts = new LinkedHashMap<>(); // trips, recovers, transitions
    private volatile long dropped;

    /** 记录一次变迁（同态变迁忽略；server 空白忽略）。 */
    public void record(String server, String from, String to, long atEpochMs) {
        if (server == null || server.isBlank() || from == null || to == null || from.equals(to)) {
            return;
        }
        lock.lock();
        try {
            if (ring.size() >= RING_CAPACITY) {
                ring.pollFirst();
                dropped++;
            }
            ring.addLast(new Transition(server, from, to, atEpochMs));
            long[] agg = counts.get(server);
            if (agg == null) {
                if (counts.size() >= AGGREGATE_CAP) {
                    return; // 键封顶：明细仍在记，聚合不再收新键
                }
                agg = new long[3];
                counts.put(server, agg);
            }
            agg[2]++;
            if ("OPEN".equals(to)) {
                agg[0]++; // trip
            } else if ("CLOSED".equals(to)) {
                agg[1]++; // recover
            }
        } finally {
            lock.unlock();
        }
    }

    /** 只读快照（recent 新→旧；byServer 按 transitions 降序）。 */
    public Report snapshot() {
        lock.lock();
        try {
            List<Transition> recent = new ArrayList<>(ring);
            java.util.Collections.reverse(recent);
            List<ServerCounts> byServer = new ArrayList<>();
            counts.forEach((server, agg) -> byServer.add(new ServerCounts(server, agg[0], agg[1], agg[2])));
            byServer.sort(java.util.Comparator.comparingLong(ServerCounts::transitions).reversed());
            return new Report(List.copyOf(recent), List.copyOf(byServer), dropped, RING_CAPACITY);
        } finally {
            lock.unlock();
        }
    }
}
