package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * PII 通道×类型命中矩阵（L 会话 1700 系 R41 = effort #1740 / spec 1740 /
 * 票 T2681 + T2682 / impl 1340）——WAF 命中地图思想：PII 命中在哪个
 * 通道（输入/流式/导出）× 什么类型（手机号/邮箱/…）的二维分布——
 * 「哪条路在漏什么」一表定位脱敏策略盲区。
 * {@link PiiHitStats}（总量面）之外的结构维。
 *
 * <p>实例面线程安全：`record(channel, piiType)` 计数（行基数有界：
 * 通道为闭集 3、类型键超出 32 并 {@code _overflow_}）+`census()` 行列
 * 降序展平。纯读面 opt-in。
 *
 * @since 1.0.0
 */
public final class PiiChannelMatrix {

    /** PII 流经通道闭集。 */
    public enum Channel { INPUT, STREAM, EXPORT }

    /** 类型键基数上限。 */
    public static final int DEFAULT_MAX_TYPES = 32;

    private final int maxTypes;
    private final Object lock = new Object();
    private final Map<String, AtomicLong> cells = new LinkedHashMap<>();

    /** 默认基数上限。 */
    public PiiChannelMatrix() {
        this(DEFAULT_MAX_TYPES);
    }

    /** 自定义类型基数上限（&lt;1 按默认）。 */
    public PiiChannelMatrix(int maxTypes) {
        this.maxTypes = maxTypes < 1 ? DEFAULT_MAX_TYPES : maxTypes;
    }

    /** 记一次通道×类型命中（type 缺名归 _unknown_）。 */
    public void record(Channel channel, String piiType) {
        String type = piiType == null || piiType.isBlank() ? "_unknown_" : piiType;
        String row = channel + ":" + type;
        synchronized (lock) {
            AtomicLong cell = cells.get(row);
            if (cell == null) {
                if (cells.size() >= maxTypes * Channel.values().length) {
                    row = "_overflow_";
                    cell = cells.get(row);
                }
                if (cell == null) {
                    cell = cells.computeIfAbsent(row, k -> new AtomicLong());
                }
            }
            cell.incrementAndGet();
        }
    }

    /** 矩阵展平快照（行 = 通道:类型，计数降序，unmodifiableMap 保序）。 */
    public Map<String, Long> census() {
        synchronized (lock) {
            List<Map.Entry<String, Long>> desc = cells.entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), e.getValue().get()))
                    .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toList());
            Map<String, Long> ordered = new LinkedHashMap<>();
            desc.forEach(e -> ordered.put(e.getKey(), e.getValue()));
            return java.util.Collections.unmodifiableMap(ordered);
        }
    }

    /** 命中总数。 */
    public long total() {
        synchronized (lock) {
            return cells.values().stream().mapToLong(AtomicLong::get).sum();
        }
    }
}
