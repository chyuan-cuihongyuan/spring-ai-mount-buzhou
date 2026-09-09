package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 提示词使用统计 holder（spec 424 / T739，418 SecretHitStats 同构）：
 * (name, version) → resolve 次数。{@link #snapshot()} 出字典序行
 * （name→version——可复现）；不清零（趋势对比用两次快照差）。
 * {@code record} 由 {@link UsageTrackingPromptRegistry} 记账（包内缝）。
 */
public final class PromptUsageStats {

    /** 统计行（快照元素）。 */
    public record Row(String name, int version, long count) {
    }

    private static final class Key {
        final String name;
        final int version;

        Key(String name, int version) {
            this.name = name;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && version == k.version && name.equals(k.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, version);
        }
    }

    private final Map<Key, LongAdder> counts = new ConcurrentHashMap<>();

    /** 记一次使用（decorator 包内缝——宿主不直呼）。 */
    void record(String name, int version) {
        if (name == null || name.isBlank()) {
            return; // 防御：无名不记账
        }
        counts.computeIfAbsent(new Key(name, version), k -> new LongAdder()).increment();
    }

    /** 当前统计快照（name→version 字典序；空=零使用）。 */
    public List<Row> snapshot() {
        return counts.entrySet().stream()
                .map(e -> new Row(e.getKey().name, e.getKey().version, e.getValue().sum()))
                .sorted(java.util.Comparator.comparing(Row::name)
                        .thenComparingInt(Row::version))
                .toList();
    }
}
