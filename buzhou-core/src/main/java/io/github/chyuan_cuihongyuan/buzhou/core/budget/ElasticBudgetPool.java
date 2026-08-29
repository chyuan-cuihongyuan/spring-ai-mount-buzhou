package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 弹性预算池（spec 157 / T515，Spark AQE 动态再分配借鉴）：总容量 C + 各会话
 * 基础配额（Σbase ≤ C——保底）；超 base 借用只吃 surplus
 * （C − Σ max(base, held)）——<b>基础配额永不被借走</b>；借走不召回
 * （在飞借用不中途斩）。池级单锁（小临界区，诚实边界入档）。
 */
public final class ElasticBudgetPool {

    /** 观测行：持有量 / 基础配额 / 本会话累计借用。 */
    public record Row(long held, long base, long borrowedTotal) {
    }

    private static final class SessionBudget {
        long held;
        long borrowedTotal;

        SessionBudget(long base) {
            this.base = base;
        }

        final long base;
    }

    private final long capacity;
    private final Map<String, SessionBudget> sessions = new LinkedHashMap<>();
    private final AtomicLong borrowed = new AtomicLong();
    private final AtomicLong denied = new AtomicLong();

    public ElasticBudgetPool(long capacity, Map<String, Long> baseQuotas) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity 必须为正（当前 " + capacity + "）");
        }
        long sumBases = 0;
        if (baseQuotas != null) {
            for (Map.Entry<String, Long> e : baseQuotas.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank()
                        || e.getValue() == null || e.getValue() < 0) {
                    throw new IllegalArgumentException("基础配额表非法：" + e.getKey());
                }
                sumBases += e.getValue();
            }
        }
        if (sumBases > capacity) {
            throw new IllegalArgumentException("Σbase（" + sumBases + "）超容量（" + capacity
                    + "）——配额表保底口径不成立，不带病上线");
        }
        this.capacity = capacity;
        if (baseQuotas != null) {
            baseQuotas.forEach((id, base) -> sessions.put(id, new SessionBudget(base)));
        }
    }

    /**
     * 获取：held+amount ≤ base 保底恒可用；否则仅当 amount ≤ 可借 surplus
     * （C − Σ max(base, held)——护住所有会话 base）。
     */
    public synchronized boolean tryAcquire(String sessionId, long amount) {
        if (sessionId == null || amount <= 0) {
            throw new IllegalArgumentException("sessionId 非空、amount 为正");
        }
        SessionBudget budget = sessions.computeIfAbsent(sessionId, k -> new SessionBudget(0));
        if (budget.held + amount <= budget.base) {
            budget.held += amount;
            return true; // 保底路径
        }
        long reserved = sessions.values().stream()
                .mapToLong(b -> Math.max(b.base, b.held))
                .sum();
        long borrowable = capacity - reserved;
        if (budget.held + amount <= budget.base + borrowable) {
            budget.held += amount;
            long over = budget.held - budget.base;
            if (over > 0) {
                borrowed.incrementAndGet();
                budget.borrowedTotal++;
            }
            return true;
        }
        denied.incrementAndGet();
        return false;
    }

    /** 归还（held 递减 surplus 回升；不可归负）。 */
    public synchronized void release(String sessionId, long amount) {
        SessionBudget budget = sessions.get(sessionId);
        if (budget == null || amount <= 0) {
            return;
        }
        budget.held = Math.max(0, budget.held - amount);
    }

    /** 剩余可借 surplus（C − Σ max(base, held)）。 */
    public synchronized long surplus() {
        long reserved = sessions.values().stream()
                .mapToLong(b -> Math.max(b.base, b.held))
                .sum();
        return capacity - reserved;
    }

    /** 会话持有量。 */
    public synchronized long heldOf(String sessionId) {
        SessionBudget budget = sessions.get(sessionId);
        return budget == null ? 0 : budget.held;
    }

    /** 观测快照（稳定序）。 */
    public synchronized Map<String, Row> snapshot() {
        Map<String, Row> out = new TreeMap<>();
        sessions.forEach((id, b) -> out.put(id, new Row(b.held, b.base, b.borrowedTotal)));
        return out;
    }

    public long borrowedCount() {
        return borrowed.get();
    }

    public long deniedCount() {
        return denied.get();
    }

    public long capacity() {
        return capacity;
    }
}
