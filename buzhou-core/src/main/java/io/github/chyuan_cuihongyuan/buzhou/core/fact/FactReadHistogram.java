package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事实读热分桶（L 会话 1700 系 R25 = effort #1724 / spec 1724 /
 * 票 T2649 + T2650 / impl 1324）——Redis LFU 8-bit 频次直方思想：事实
 * （{@link Fact}）被读取的频次分布回答「少数热点事实承载多数读取还是
 * 均匀冷读」——冷/温/热/灼四档分桶，缓存与衰减（FactDecayPolicy）调参
 * 的直接依据。
 *
 * <p>实例面线程安全：逐键计数有界（默认 512 键，超出并 {@code _overflow_}
 * 桶——基数纪律）；census() 按档分桶（cold=1 次 / warm=2–4 / hot=5–16 /
 * blazing&gt;16）。纯读面 opt-in，不改 FactStore。
 *
 * @since 1.0.0
 */
public final class FactReadHistogram {

    /** 默认键基数上限。 */
    public static final int DEFAULT_MAX_KEYS = 512;

    private final int maxKeys;
    private final Map<String, AtomicLong> reads = new LinkedHashMap<>();
    private final AtomicLong totalReads = new AtomicLong();

    /** 默认容量。 */
    public FactReadHistogram() {
        this(DEFAULT_MAX_KEYS);
    }

    /** 自定义键基数上限（&lt;1 按默认）。 */
    public FactReadHistogram(int maxKeys) {
        this.maxKeys = maxKeys < 1 ? DEFAULT_MAX_KEYS : maxKeys;
    }

    /** 记一次事实读取（null/空键归 _anonymous_ 桶）。 */
    public void record(String factKey) {
        totalReads.incrementAndGet();
        String bucket = factKey == null || factKey.isBlank() ? "_anonymous_" : factKey;
        AtomicLong counter = reads.get(bucket);
        if (counter == null) {
            if (reads.size() >= maxKeys) {
                bucket = "_overflow_";
                counter = reads.get(bucket);
            }
            if (counter == null) {
                counter = reads.computeIfAbsent(bucket, k -> new AtomicLong());
            }
        }
        counter.incrementAndGet();
    }

    /**
     * @param totalReads   读取总数
     * @param distinctKeys 计数中的不同键数（含 overflow/anonymous 桶）
     * @param cold         冷档键数（恰好 1 次）
     * @param warm         温档键数（2–4 次）
     * @param hot          热档键数（5–16 次）
     * @param blazing      灼热键数（&gt;16 次）
     */
    public record HeatCensus(long totalReads, int distinctKeys,
                             int cold, int warm, int hot, int blazing) {
    }

    /** 普查快照。 */
    public HeatCensus census() {
        int cold = 0;
        int warm = 0;
        int hot = 0;
        int blazing = 0;
        for (AtomicLong counter : reads.values()) {
            long n = counter.get();
            if (n <= 1) {
                cold++;
            } else if (n <= 4) {
                warm++;
            } else if (n <= 16) {
                hot++;
            } else {
                blazing++;
            }
        }
        return new HeatCensus(totalReads.get(), reads.size(), cold, warm, hot, blazing);
    }
}
