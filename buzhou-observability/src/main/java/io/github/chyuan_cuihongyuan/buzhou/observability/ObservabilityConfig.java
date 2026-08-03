package io.github.chyuan_cuihongyuan.buzhou.observability;

import java.time.Duration;
import java.util.List;

/**
 * 可观测采集配置（spec 03 配置项表，前缀 {@code buzhou.observability.*}）。
 *
 * <p>纳入四层覆盖体系（默认 &lt; yml &lt; 绑定级 &lt; 工具级）。OTel/dashboard 配置项不在本票。
 *
 * @param enabled           总开关；关闭后 advisor/包装层短路直通
 * @param batchSize         异步落库批大小（默认 200）
 * @param flushInterval     异步落库 flush 间隔（默认 1s）
 * @param flushTimeout      close/flush 等待 drain 的最长时长（默认 5s）
 * @param queueCapacity     内存队列容量；满则背压（默认 10000）
 * @param thinkingCapture   思维链采集开关
 * @param thinkingExtraKeys 厂商适配表扩展：metadata key 列表
 * @param thinkingMaxChars  单条思维链超长截断阈值（默认 32768）
 * @param includeStacktrace ERROR Event 是否记录堆栈
 * @param snapshotCapture   注入快照落库开关
 * @param micrometerEnabled Micrometer 双写开关
 */
public record ObservabilityConfig(
        boolean enabled,
        int batchSize,
        Duration flushInterval,
        Duration flushTimeout,
        int queueCapacity,
        boolean thinkingCapture,
        List<String> thinkingExtraKeys,
        int thinkingMaxChars,
        boolean includeStacktrace,
        boolean snapshotCapture,
        boolean micrometerEnabled) {

    public ObservabilityConfig {
        thinkingExtraKeys = thinkingExtraKeys == null ? List.of() : List.copyOf(thinkingExtraKeys);
    }

    public static ObservabilityConfig defaults() {
        return new ObservabilityConfig(true, 200, Duration.ofSeconds(1), Duration.ofSeconds(5),
                10000, true, List.of(), 32768, true, true, true);
    }

    /** 测试用：批大小 1 + flush 间隔 10ms，几乎即时落库，便于断言。 */
    public static ObservabilityConfig testDefaults() {
        return new ObservabilityConfig(true, 1, Duration.ofMillis(10), Duration.ofSeconds(5),
                10000, true, List.of(), 32768, true, true, false);
    }
}
