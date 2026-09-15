package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 错误首见签名台账（L 会话 1700 系 R22 = effort #1721 / spec 1721 /
 * 票 T2643 + T2644 / impl 1321）——Sentry new-issue 追踪思想：错误签名
 * 的「首见时刻」是回归探测的第一信号——老签名反复出现是已知病，新签名
 * 出现才是新病；{@link ErrorSignatures} 管签名规范化，本面管新颖性。
 *
 * <p>实例面线程安全：签名集合有界（默认 256，FIFO 逐出最旧——长跑下
 * 老签名被逐出后会再次「首见」，诚实入档）；`record` 返回是否首见。
 *
 * @since 1.0.0
 */
public final class ErrorNoveltyLedger {

    /** 默认签名容量。 */
    public static final int DEFAULT_CAPACITY = 256;

    private final int capacity;
    private final Set<String> seen;
    private final Object lock = new Object();
    private long newCount;
    private long repeatCount;

    /** 默认容量。 */
    public ErrorNoveltyLedger() {
        this(DEFAULT_CAPACITY);
    }

    /** 自定义容量（&lt;1 按默认）。 */
    public ErrorNoveltyLedger(int capacity) {
        this.capacity = capacity < 1 ? DEFAULT_CAPACITY : capacity;
        this.seen = new LinkedHashSet<>();
    }

    /**
     * 记一个签名。
     *
     * @return true = 首见（新病）；false = 重复（已知病）
     */
    public boolean record(String signature) {
        String key = signature == null || signature.isBlank() ? "_blank_" : signature;
        synchronized (lock) {
            boolean isNew = seen.add(key);
            if (isNew) {
                newCount++;
                while (seen.size() > capacity) {
                    java.util.Iterator<String> it = seen.iterator();
                    it.next();
                    it.remove();
                }
            } else {
                repeatCount++;
            }
            return isNew;
        }
    }

    /**
     * @param seenDistinct 台账内不同签名数（≤容量）
     * @param newCount     首见累计
     * @param repeatCount  重复累计
     * @param noveltyRatio 新见占比 newCount/(new+repeat)；无样本哨兵 −1
     */
    public record NoveltyReport(int seenDistinct, long newCount,
                                long repeatCount, double noveltyRatio) {
    }

    /** 快照。 */
    public NoveltyReport report() {
        synchronized (lock) {
            long total = newCount + repeatCount;
            double ratio = total == 0 ? -1d : (double) newCount / total;
            return new NoveltyReport(seen.size(), newCount, repeatCount, ratio);
        }
    }
}
