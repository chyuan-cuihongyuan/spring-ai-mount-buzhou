package io.github.chyuan_cuihongyuan.buzhou.resilience.capability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * 能力门决策审计读数（spec 700 / T1000，OPA Decision Logs 借鉴）：
 * 门每个 deny 决定环形留痕（有界+dropped 计数），admit 只计数（量级折中——
 * 逐条 admit 会刷掉 deny 留痕），per-model deny 聚合，snapshot() 不可变报告。
 *
 * <p>纯旁路读数：gate() 在 throw 前 record，拒绝行为零变化。内存有界、
 * 进程重启清零——审计连续性归日志/审计链族，本面只管「进程内最近窗口」。
 * 线程安全：synchronized（deny 频率低，锁竞争不构成瓶颈）。
 */
public final class CapabilityDecisionAudit {

    /** deny 环形容量（固定——读数面非行为面，不做 yml 键）。 */
    public static final int DEFAULT_CAPACITY = 64;

    /** 单条 deny 决定（模型 + 不达标能力 + 时刻 epoch ms）。 */
    public record Decision(String model, String capability, long atEpochMs) {
    }

    /** 不可变读数报告（recentDenies 最新在前；列表为防御拷贝）。 */
    public record Report(List<Decision> recentDenies, long denied, long admitted,
                         long dropped, int capacity, Map<String, Long> denyByModel,
                         Map<String, Long> denyByCapability) {
    }

    private final int capacity;
    private final Deque<Decision> recent = new ArrayDeque<>();
    private final Map<String, Long> denyByModel = new LinkedHashMap<>();
    private long denied;
    private long admitted;
    private long dropped;

    public CapabilityDecisionAudit() {
        this(DEFAULT_CAPACITY);
    }

    public CapabilityDecisionAudit(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity 必须为正，收到 " + capacity);
        }
        this.capacity = capacity;
    }

    /** 记录一次 deny 决定（gate() throw 前调用；null 字段 fail-fast）。 */
    public synchronized void recordDeny(String model, String capability) {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(capability, "capability");
        Decision decision = new Decision(model, capability, System.currentTimeMillis());
        if (recent.size() >= capacity) {
            recent.removeLast();
            dropped++;
        }
        recent.addFirst(decision);
        denied++;
        denyByModel.merge(model, 1L, Long::sum);
    }

    /** 记录一次 admit（门裁决放行；只计数不逐条）。 */
    public synchronized void recordAdmit(String model) {
        Objects.requireNonNull(model, "model");
        admitted++;
    }

    /** 不可变快照（最新在前；防御拷贝）。 */
    public synchronized Report snapshot() {
        Map<String, Long> byCapability = new LinkedHashMap<>();
        for (Decision decision : recent) {
            byCapability.merge(decision.capability(), 1L, Long::sum);
        }
        return new Report(List.copyOf(recent), denied, admitted, dropped, capacity,
                Map.copyOf(denyByModel), Map.copyOf(byCapability));
    }
}
