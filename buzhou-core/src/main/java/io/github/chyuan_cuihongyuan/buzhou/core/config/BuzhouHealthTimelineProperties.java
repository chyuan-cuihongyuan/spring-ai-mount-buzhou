package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 健康时间线 yml 面（spec 405 / T702，PagerDuty incident timeline 借鉴）：
 * {@code buzhou.health.timeline.{enabled=false, interval=15s, capacity=256,
 * export-path}}。export-path 可选（声明即逐变迁落盘 JSONL）。
 *
 * <p>spec 643 / T936：{@code export-max-bytes / export-max-history} 细调
 * JSONL 轮转档位（键缺席 = 默认 64MB×3；显式 ≤0 = 关——无界 escape hatch）。
 */
@ConfigurationProperties(prefix = "buzhou.health.timeline")
public record BuzhouHealthTimelineProperties(Boolean enabled, Duration interval,
        Integer capacity, String exportPath, Long exportMaxBytes, Integer exportMaxHistory) {

    /** spec 643 之前的 4 参调用方（轮转细调缺席 = 默认档）。 */
    public BuzhouHealthTimelineProperties(Boolean enabled, Duration interval,
            Integer capacity, String exportPath) {
        this(enabled, interval, capacity, exportPath, null, null);
    }

    /** 多构造器场景：显式指定规范构造器为绑定构造器（便捷构造不参与绑定）。 */
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public BuzhouHealthTimelineProperties {
        interval = (interval == null || interval.isZero() || interval.isNegative())
                ? Duration.ofSeconds(15) : interval;
        capacity = (capacity == null || capacity <= 0) ? 256 : capacity;
        exportPath = exportPath == null || exportPath.isBlank() ? null : exportPath;
    }

    /** 轮转大小档（键缺席 → RollingJsonlWriter 默认 64MB；显式 ≤0 = 关）。 */
    public long effectiveExportMaxBytes() {
        return exportMaxBytes == null
                ? io.github.chyuan_cuihongyuan.buzhou.core.fs.RollingJsonlWriter.DEFAULT_MAX_BYTES
                : exportMaxBytes;
    }

    /** 轮转代数档（键缺席 → 默认 3 代；显式 ≤0 = 关）。 */
    public int effectiveExportMaxHistory() {
        return exportMaxHistory == null
                ? io.github.chyuan_cuihongyuan.buzhou.core.fs.RollingJsonlWriter.DEFAULT_MAX_HISTORY
                : exportMaxHistory;
    }
}
