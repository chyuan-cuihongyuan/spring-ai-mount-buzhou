package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * 会话年龄分桶直方（L 会话 1700 系 R8 = effort #1707 / spec 1707 /
 * 票 T2615 + T2616 / impl 1307）——Prometheus histogram 思想：会话存活
 * 年龄（自创建至今）的分桶普查，与 {@link IdleDurationHistogram}
 * （空闲时长）对称互补——「活多久」与「闲多久」是两个治理轴。
 *
 * <p>实例面线程安全（AtomicLongArray）：默认边界 1h/1d/7d → 4 桶
 * （fresh &lt;1h / 当日 &lt;1d / 当周 &lt;7d / 更老）；负值忽略；
 * eldest 记录最老年龄供「最老活会话」哨戒。
 *
 * @since 1.0.0
 */
public final class SessionAgeHistogram {

    /** 默认桶边界（毫秒）：1h/1d/7d → 4 桶。 */
    public static final long[] DEFAULT_BOUNDS = {
            3_600_000L, 86_400_000L, 604_800_000L};

    private final long[] bounds;
    private final AtomicLongArray buckets;
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong eldest = new AtomicLong();

    /** 默认边界。 */
    public SessionAgeHistogram() {
        this(DEFAULT_BOUNDS);
    }

    /** 自定义升序边界（n 边界 n+1 桶；null/空 = 单桶）。 */
    public SessionAgeHistogram(long[] bounds) {
        this.bounds = bounds == null ? new long[0] : bounds.clone();
        this.buckets = new AtomicLongArray(this.bounds.length + 1);
    }

    /** 记录一个会话年龄（负值忽略）。 */
    public void record(long ageMillis) {
        if (ageMillis < 0) {
            return;
        }
        total.incrementAndGet();
        eldest.getAndUpdate(prev -> Math.max(prev, ageMillis));
        buckets.incrementAndGet(bucketOf(ageMillis));
    }

    private int bucketOf(long ageMillis) {
        for (int i = 0; i < bounds.length; i++) {
            if (ageMillis < bounds[i]) {
                return i;
            }
        }
        return bounds.length;
    }

    /** 桶计数只读（长度 = 边界数+1）。 */
    public long[] bucketCounts() {
        long[] out = new long[buckets.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = buckets.get(i);
        }
        return out;
    }

    /** 已记录样本总数。 */
    public long total() {
        return total.get();
    }

    /** 最老会话年龄（毫秒；无样本 0）。 */
    public long eldestMillis() {
        return eldest.get();
    }
}
