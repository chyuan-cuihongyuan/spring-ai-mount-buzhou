package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.Instant;

/**
 * impl-37 / spec 13 §stores-6：保留策略族配置（前缀 {@code buzhou.retention}）。
 *
 * <p>归一化沿用 null/非正 → 默认惯例（JSR-303 启动校验属切片 42）。
 *
 * @param enabled              sweeper 后台调度自启动开关（默认 true；bean 恒在——关闭时
 *                             各策略仍可手动 {@code RetentionSweeper.sweepOnce()} 触发）
 * @param sweepInterval        周期间隔；默认 PT1H
 * @param sessionRetention     会话保留期（锚点=closedAt）；默认 PT72H
 * @param sessionNotBefore     改短不追溯保护：此时刻之前封闭的会话不受当前保留期约束；
 *                             null = 追溯（默认）
 * @param observabilityTtl     观测数据 TTL；默认 PT7D
 * @param observabilityBatchSize 观测 TTL 单周期批删限量；默认 500（null 交给值对象归一）
 * @param summaryKeepVersions  摘要每会话保留版本数；默认 3
 * @param toolCallLogRetention 工具调用日志保留窗口；默认 PT7D
 * @param runCompletedRetention COMPLETED run 保留窗口；默认 PT24H
 * @param trigger              批删限量公式（base/scaleFactor/cap/hardFloor）
 */
@ConfigurationProperties(prefix = "buzhou.retention")
public record BuzhouRetentionProperties(
        Boolean enabled,
        Duration sweepInterval,
        Duration sessionRetention,
        Instant sessionNotBefore,
        Duration observabilityTtl,
        Integer observabilityBatchSize,
        Integer summaryKeepVersions,
        Duration toolCallLogRetention,
        Duration runCompletedRetention,
        Trigger trigger) {

    public BuzhouRetentionProperties {
        enabled = enabled == null || enabled;
        sweepInterval = sweepInterval == null || sweepInterval.isZero() || sweepInterval.isNegative()
                ? Duration.ofHours(1) : sweepInterval;
        sessionRetention = sessionRetention == null
                || sessionRetention.isZero() || sessionRetention.isNegative()
                ? io.github.chyuan_cuihongyuan.buzhou.core.retention.SessionHistoryPolicy.DEFAULT_RETENTION
                : sessionRetention;
        observabilityTtl = observabilityTtl == null
                || observabilityTtl.isZero() || observabilityTtl.isNegative()
                ? io.github.chyuan_cuihongyuan.buzhou.core.retention.ObservabilityTtl.DEFAULT_TTL
                : observabilityTtl;
        summaryKeepVersions = summaryKeepVersions == null || summaryKeepVersions < 1 ? 3 : summaryKeepVersions;
        toolCallLogRetention = toolCallLogRetention == null
                || toolCallLogRetention.isZero() || toolCallLogRetention.isNegative()
                ? Duration.ofDays(7) : toolCallLogRetention;
        runCompletedRetention = runCompletedRetention == null
                || runCompletedRetention.isZero() || runCompletedRetention.isNegative()
                ? Duration.ofHours(24) : runCompletedRetention;
        trigger = trigger == null ? new Trigger(null, null, null, null) : trigger;
    }

    /** 批删限量公式（PG autovacuum 阈值四件套形状）。 */
    public record Trigger(Integer base, Double scaleFactor, Integer cap, Integer hardFloor) {
    }
}
