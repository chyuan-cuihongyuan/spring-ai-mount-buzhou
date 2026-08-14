package io.github.chyuan_cuihongyuan.buzhou.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * 可观测采集配置（spec 03 配置项表，前缀 {@code buzhou.observability.*}；impl-46 收口）。
 *
 * <p>纳入四层覆盖体系（默认 &lt; yml &lt; 绑定级 &lt; 工具级）。OTel/dashboard 配置项不在本票。
 * <b>fail-fast</b>：显式非法值（批大小 &lt; 1 / 负时长等）启动即失败（BuzhouConfigurationException 带修法）。
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
 * @param modelProvider     模型提供方显式声明（impl-46：替代模型名启发式判定
 *                          {@code gpt/o1/o3 contains}；未配置保留启发式）
 */
@ConfigurationProperties(prefix = "buzhou.observability")
@Validated
public record ObservabilityConfig(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("200") int batchSize,
        @DefaultValue("1s") Duration flushInterval,
        @DefaultValue("5s") Duration flushTimeout,
        @DefaultValue("10000") int queueCapacity,
        @DefaultValue("true") boolean thinkingCapture,
        List<String> thinkingExtraKeys,
        @DefaultValue("32768") int thinkingMaxChars,
        @DefaultValue("true") boolean includeStacktrace,
        @DefaultValue("true") boolean snapshotCapture,
        @DefaultValue("true") boolean micrometerEnabled,
        String modelProvider) {

    /** 多构造器场景下显式指定绑定构造器（11 参兼容构造仅供编程式使用）。 */
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public ObservabilityConfig {
        thinkingExtraKeys = thinkingExtraKeys == null ? List.of() : List.copyOf(thinkingExtraKeys);
        // impl-46 fail-fast：此前非法值静默进队列/线程参数，运行期才炸（drainTo(batch, batchSize-1) 负数等）
        if (batchSize < 1) {
            throw new BuzhouConfigurationException("buzhou.observability.batch-size（" + batchSize + "）必须 >= 1",
                    "设为正整数（默认 200）");
        }
        if (queueCapacity < 1) {
            throw new BuzhouConfigurationException("buzhou.observability.queue-capacity（" + queueCapacity + "）必须 >= 1",
                    "设为正整数（默认 10000）");
        }
        if (flushInterval == null || flushInterval.isNegative() || flushInterval.isZero()) {
            throw new BuzhouConfigurationException("buzhou.observability.flush-interval 必须为正时长",
                    "设为正时长（默认 1s）");
        }
        if (flushTimeout == null || flushTimeout.isNegative()) {
            throw new BuzhouConfigurationException("buzhou.observability.flush-timeout 必须为非负时长",
                    "设为非负时长（默认 5s）");
        }
        if (thinkingMaxChars < 1) {
            throw new BuzhouConfigurationException("buzhou.observability.thinking-max-chars（"
                    + thinkingMaxChars + "）必须 >= 1", "设为正整数（默认 32768）");
        }
    }

    /** 既有 11 参构造兼容（modelProvider = null 走启发式）。 */
    public ObservabilityConfig(boolean enabled, int batchSize, Duration flushInterval, Duration flushTimeout,
                               int queueCapacity, boolean thinkingCapture, List<String> thinkingExtraKeys,
                               int thinkingMaxChars, boolean includeStacktrace, boolean snapshotCapture,
                               boolean micrometerEnabled) {
        this(enabled, batchSize, flushInterval, flushTimeout, queueCapacity, thinkingCapture,
                thinkingExtraKeys, thinkingMaxChars, includeStacktrace, snapshotCapture,
                micrometerEnabled, null);
    }

    public static ObservabilityConfig defaults() {
        return new ObservabilityConfig(true, 200, Duration.ofSeconds(1), Duration.ofSeconds(5),
                10000, true, List.of(), 32768, true, true, true, null);
    }

    /** 测试用：批大小 1 + flush 间隔 10ms，几乎即时落库，便于断言。 */
    public static ObservabilityConfig testDefaults() {
        return new ObservabilityConfig(true, 1, Duration.ofMillis(10), Duration.ofSeconds(5),
                10000, true, List.of(), 32768, true, true, false, null);
    }
}
