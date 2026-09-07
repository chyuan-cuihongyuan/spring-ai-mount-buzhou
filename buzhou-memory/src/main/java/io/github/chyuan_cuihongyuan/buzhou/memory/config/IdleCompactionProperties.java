package io.github.chyuan_cuihongyuan.buzhou.memory.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 空闲会话后台压缩装配属性（spec 310 / T612，前缀
 * {@code buzhou.memory.idle-compaction}）。默认关——依赖（摘要模型 + 会话索引）
  * 齐备且显式开启才装配。
 *
 * @param enabled       开关（默认 false）
 * @param idleThreshold 空闲阈值（默认 1h——lastActiveAt 早于该时长即候选）
 * @param interval      sweep 周期（默认 10m）
 * @param maxPerSweep   每轮压缩上限（默认 8——防千会话同时空闲起压缩风暴）
 */
@ConfigurationProperties(prefix = "buzhou.memory.idle-compaction")
public record IdleCompactionProperties(
        Boolean enabled,
        Duration idleThreshold,
        Duration interval,
        Integer maxPerSweep) {

    public IdleCompactionProperties {
        idleThreshold = idleThreshold == null ? Duration.ofHours(1) : idleThreshold;
        interval = interval == null ? Duration.ofMinutes(10) : interval;
        maxPerSweep = maxPerSweep == null ? 8 : maxPerSweep;
        if (idleThreshold.isZero() || idleThreshold.isNegative()) {
            throw configError("idle-threshold", idleThreshold.toString(), "正时长，如 1h");
        }
        if (interval.isZero() || interval.isNegative()) {
            throw configError("interval", interval.toString(), "正时长，如 10m");
        }
        if (maxPerSweep < 1) {
            throw configError("max-per-sweep", String.valueOf(maxPerSweep), ">= 1（默认 8）");
        }
    }

    /** 生效开关（显式开启）。 */
    public boolean effectiveEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    private static BuzhouConfigurationException configError(String key, String value, String hint) {
        return new BuzhouConfigurationException(
                "buzhou.memory.idle-compaction." + key + "（" + value + "）非法", hint);
    }
}
