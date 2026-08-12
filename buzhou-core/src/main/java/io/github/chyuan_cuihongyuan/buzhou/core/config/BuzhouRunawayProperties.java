package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * 死循环与失控检测装配属性（spec「死循环与失控检测」，前缀 {@code buzhou.runaway}）。
 *
 * <p>双窗口数值硬顶 + 软退出通道 + 确定性重复检测。检测器主体挂在既有 {@code BuzhouHook}
 * 切面（{@code beforeTurn}/{@code beforeModel}/{@code beforeTool}）+ 既有 {@code AttachmentRenderer}
 * 注入槽，不新增模块 / 不新增 SPI / 不引外部依赖。
 *
 * <p>字段统一用 boxed 类型，null = 「未配置 / 不限」（对齐 {@code BuzhouBackpressureProperties} 模板）。
 * <b>safe-by-default</b>：阈值默认 null = 不限，显式配置才生效——不设可能误伤生产的魔法默认值
 * （对齐 07 背压 safe-by-default）。
 *
 * <p><b>与花费失控的分工</b>：本机制管「行为失控」（步数 / 调用次数 / 时长），「花费失控」
 * （token 硬顶 / 预算）归 11 成本治理，两者正交。
 *
 * @param enabled            机制总开关（默认开，safe-by-default；关则完全旁路，等价现状）
 * @param perTurn            轮次级窗口（单轮最大步数 / 工具调用数 / wall-clock）
 * @param perSession         会话级窗口（会话生命周期累计步数 / 工具调用数，跨崩溃持久化）
 * @param perTool            按工具单独限额（key=工具名 glob 通配，value=单轮最多调用次数）
 * @param softThresholdRatio 软阈值比例（剩余预算占比低于此值时注入软退出提醒；默认 0.2=剩余&lt;20%；仅在有 per-turn.max-steps 时生效）
 * @param repetition         确定性重复检测（连续 N 次同工具同参数；M2，默认关）
 * @param escalatePolicy     失控处置升级策略（默认 emit-event；未来 hitl / 转人工）
 */
@ConfigurationProperties(prefix = "buzhou.runaway")
public record BuzhouRunawayProperties(
        Boolean enabled,
        PerTurn perTurn,
        PerSession perSession,
        Map<String, PerToolLimit> perTool,
        Double softThresholdRatio,
        Repetition repetition,
        String escalatePolicy) {

    /** 软阈值默认比例：剩余预算 &lt; 20% 时注入软退出提醒。 */
    public static final double DEFAULT_SOFT_THRESHOLD_RATIO = 0.2;

    public BuzhouRunawayProperties {
        enabled = enabled == null || enabled;
        // 阈值字段保持 null = 未配置，由检测器派生
    }

    /** 全默认（装配测试 / 兜底用；所有阈值 null = 不限，等价现状）。 */
    public static BuzhouRunawayProperties defaults() {
        return new BuzhouRunawayProperties(null, null, null, null, null, null, null);
    }

    /** 软阈值比例生效值（null 取默认 0.2）。 */
    public double effectiveSoftThresholdRatio() {
        return softThresholdRatio != null ? softThresholdRatio : DEFAULT_SOFT_THRESHOLD_RATIO;
    }

    /**
     * 轮次级窗口参数组。
     *
     * @param maxSteps    单轮最大思考步数（模型调用次数硬顶；null = 不限）
     * @param maxToolCalls 单轮最大工具调用次数（null = 不限）
     * @param wallClock   单轮 wall-clock 超时（步边界生效；诚实边界 = wallClock + 单步时长；null = 不限）
     */
    public record PerTurn(Integer maxSteps, Integer maxToolCalls, Duration wallClock) {
    }

    /**
     * 会话级窗口参数组（累计计数持久化在 SessionStateStore，跨崩溃保留）。
     *
     * @param maxSteps    会话生命周期累计步数上限（null = 不限）
     * @param maxToolCalls 会话生命周期累计工具调用数上限（null = 不限）
     */
    public record PerSession(Integer maxSteps, Integer maxToolCalls) {
    }

    /** 按工具限额（key=工具名 glob 通配，如 {@code expensive_*}）。 */
    public record PerToolLimit(Integer maxCalls) {
    }

    /**
     * 确定性重复检测参数组（M2）。
     *
     * @param consecutive 连续同工具同参数调用次数阈值（null = 关闭；开启后如 3）
     * @param action      触发后处置：{@code block}（阻断，默认）或 {@code flag-only}（仅告警不阻断）
     */
    public record Repetition(Integer consecutive, String action) {
    }
}
