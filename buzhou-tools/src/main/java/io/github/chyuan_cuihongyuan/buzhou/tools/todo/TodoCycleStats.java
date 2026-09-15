package io.github.chyuan_cuihongyuan.buzhou.tools.todo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * todo 返工周期读面（L 会话 1700 系 R30 = effort #1729 / spec 1729 /
 * 票 T2659 + T2660 / impl 1329）——Jira reopened issues / GitHub issue
 * reopen 的返工遥测思想：todo 被关闭后又重开 = 返工信号——
 * {@link TodoStalenessAudit} 管陈旧，本面管「关了又开」的返工频率与
 * 最惨项排名。
 *
 * <p>实例面线程安全：逐项重开计数有界（默认 128 项，超出并
 * {@code _overflow_} 桶）；recordReopen(itemId) + census（返工项占比/
 * 总返工/单项最惨）。
 *
 * @since 1.0.0
 */
public final class TodoCycleStats {

    /** 默认项基数上限。 */
    public static final int DEFAULT_MAX_ITEMS = 128;

    private final int maxItems;
    private final Map<String, AtomicLong> reopens = new LinkedHashMap<>();

    /** 默认容量。 */
    public TodoCycleStats() {
        this(DEFAULT_MAX_ITEMS);
    }

    /** 自定义项基数上限（&lt;1 按默认）。 */
    public TodoCycleStats(int maxItems) {
        this.maxItems = maxItems < 1 ? DEFAULT_MAX_ITEMS : maxItems;
    }

    /** 记一次重开（null/空 id 归 _anonymous_）。 */
    public void recordReopen(String itemId) {
        String bucket = itemId == null || itemId.isBlank() ? "_anonymous_" : itemId;
        AtomicLong counter = reopens.get(bucket);
        if (counter == null) {
            if (reopens.size() >= maxItems) {
                bucket = "_overflow_";
                counter = reopens.get(bucket);
            }
            if (counter == null) {
                counter = reopens.computeIfAbsent(bucket, k -> new AtomicLong());
            }
        }
        counter.incrementAndGet();
    }

    /**
     * @param touchedItems   出现过重开的项数
     * @param totalReopens   重开总数
     * @param worstReopens   单项最多重开次数
     */
    public record CycleCensus(int touchedItems, long totalReopens, long worstReopens) {
    }

    /** 快照。 */
    public CycleCensus census() {
        long total = 0;
        long worst = 0;
        for (AtomicLong counter : reopens.values()) {
            long n = counter.get();
            total += n;
            worst = Math.max(worst, n);
        }
        return new CycleCensus(reopens.size(), total, worst);
    }
}
