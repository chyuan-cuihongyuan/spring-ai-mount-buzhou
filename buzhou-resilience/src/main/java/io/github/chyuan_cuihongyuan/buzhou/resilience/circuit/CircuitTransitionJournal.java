package io.github.chyuan_cuihongyuan.buzhou.resilience.circuit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 断路器状态变迁事件流读数（spec 702 / T1004，Resilience4j CircuitBreakerEvent 思想）：
 * 进程级变迁环形留痕+per-model 聚合——变迁史不再散落各会话事件通道。
 *
 * <p>内嵌 {@link ModelCircuitBreaker}（transition() 旁路 record，恒开零配置）：
 * 跳闸是异常态、变迁频率低，有界内存成本可忽略。拒绝不记（已有 circuit-rejected
 * 计数+事件——镜像会造成双口径）。线程安全：synchronized（低频无争用）。
 */
public final class CircuitTransitionJournal {

    /** 变迁环形容量（固定——读数面非行为面）。 */
    public static final int DEFAULT_CAPACITY = 64;

    /** 单条变迁（to=OPEN 时 consecutiveTrips/openDurationMs 有值，其余 -1）。 */
    public record Transition(String model, String from, String to, long atEpochMs,
                             int consecutiveTrips, long openDurationMs) {
    }

    /** 不可变读数报告（recent 最新在前；map/list 防御拷贝）。 */
    public record Report(List<Transition> recent, long dropped, int capacity,
                         Map<String, Long> tripsByModel, Map<String, Long> recoveriesByModel,
                         Map<String, Long> halfOpensByModel) {
    }

    private final int capacity;
    private final Deque<Transition> recent = new ArrayDeque<>();
    private final Map<String, Long> trips = new LinkedHashMap<>();
    private final Map<String, Long> recoveries = new LinkedHashMap<>();
    private final Map<String, Long> halfOpens = new LinkedHashMap<>();
    private long dropped;

    public CircuitTransitionJournal() {
        this(DEFAULT_CAPACITY);
    }

    public CircuitTransitionJournal(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity 必须为正，收到 " + capacity);
        }
        this.capacity = capacity;
    }

    /** 记录一次状态变迁（null 字段 fail-fast）。 */
    public synchronized void record(String model, String from, String to, long atEpochMs,
                                    int consecutiveTrips, long openDurationMs) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (recent.size() >= capacity) {
            recent.removeLast();
            dropped++;
        }
        recent.addFirst(new Transition(model, from, to, atEpochMs, consecutiveTrips, openDurationMs));
        Map<String, Long> target;
        if (CircuitState.OPEN.name().equals(to)) {
            target = trips;
        } else if (CircuitState.CLOSED.name().equals(to)) {
            target = recoveries;
        } else if (CircuitState.HALF_OPEN.name().equals(to)) {
            target = halfOpens;
        } else {
            return; // 未知目标态只留痕不入聚合（枚举扩展前向兼容）
        }
        target.merge(model, 1L, Long::sum);
    }

    /** 不可变快照（最新在前；防御拷贝）。 */
    public synchronized Report snapshot() {
        return new Report(List.copyOf(recent), dropped, capacity,
                Map.copyOf(trips), Map.copyOf(recoveries), Map.copyOf(halfOpens));
    }
}
