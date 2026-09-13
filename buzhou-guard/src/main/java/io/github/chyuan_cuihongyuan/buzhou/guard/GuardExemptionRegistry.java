package io.github.chyuan_cuihongyuan.buzhou.guard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 护栏豁免登记面（spec 820 / T1141，ESLint suppressions 带过期借鉴）：
 * 对指定护栏机制 × 主体（工具名/会话 id/规则键——语义由调用方定义）的
 * <b>显式、有时限</b>豁免——「这条告警我看过、豁免到 T1」从口头变登记。
 *
 * <p>语义：{@link #exempt} 仅当存在未过期条目时 true（now ≥ until 即过期，
 * 惰性失效并计数）；{@link #grant} 同键覆盖（重新登记=续期）；条目封顶
 * {@value #MAX_ENTRIES}（满后拒绝新登记并置 truncated——有界纪律）。
 * 本类只做登记与判定——各护栏 hook 是否征询豁免由其自身接线（不改任何
 * hook 默认行为）。
 */
public final class GuardExemptionRegistry {

    /** 条目封顶。 */
    public static final int MAX_ENTRIES = 64;

    /** 单条豁免（不可变）。 */
    public record Exemption(String mechanism, String subject, long untilMillis, String reason) {
    }

    /** 不可变快照（仅含未过期条目，按 until 降序）。 */
    public record Snapshot(List<Exemption> active, long grantedTotal, long expiredTotal, boolean truncated) {
    }

    private static final class Entry {
        final Exemption exemption;

        Entry(Exemption exemption) {
            this.exemption = exemption;
        }
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong grantedTotal = new AtomicLong();
    private final AtomicLong expiredTotal = new AtomicLong();
    private volatile boolean truncated;

    /** 登记豁免（同键覆盖续期；untilMillis 必须 > 0；空白 mechanism/subject 忽略）。 */
    public boolean grant(String mechanism, String subject, long untilMillis, String reason) {
        if (mechanism == null || mechanism.isBlank() || subject == null || subject.isBlank()
                || untilMillis <= 0) {
            return false;
        }
        grantedTotal.incrementAndGet();
        String key = mechanism + "\u0000" + subject;
        if (!entries.containsKey(key) && entries.size() >= MAX_ENTRIES) {
            truncated = true;
            return false;
        }
        entries.put(key, new Entry(new Exemption(mechanism, subject, untilMillis,
                reason == null ? "" : reason)));
        return true;
    }

    /** 撤销（幂等；返回是否存在）。 */
    public boolean revoke(String mechanism, String subject) {
        if (mechanism == null || subject == null) {
            return false;
        }
        return entries.remove(key(mechanism, subject)) != null;
    }

    /**
     * 判定：存在未过期条目（now &lt; until）即 true；过期条目惰性移除并计数。
     */
    public boolean exempt(String mechanism, String subject, long nowMillis) {
        Entry entry = mechanism == null || subject == null ? null : entries.get(key(mechanism, subject));
        if (entry == null) {
            return false;
        }
        if (nowMillis >= entry.exemption.untilMillis()) {
            entries.remove(key(mechanism, subject), entry);
            expiredTotal.incrementAndGet();
            return false;
        }
        return true;
    }

    /** 只读快照（未过期条目按 until 降序——先到期的最后挣扎面）。 */
    public Snapshot snapshot(long nowMillis) {
        List<Exemption> active = new ArrayList<>();
        entries.values().forEach(e -> {
            if (nowMillis < e.exemption.untilMillis()) {
                active.add(e.exemption);
            }
        });
        active.sort(Comparator.comparingLong(Exemption::untilMillis).reversed());
        return new Snapshot(List.copyOf(active), grantedTotal.get(), expiredTotal.get(), truncated);
    }

    private static String key(String mechanism, String subject) {
        return mechanism + "\u0000" + subject;
    }
}
