package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.function.DoubleSupplier;

/**
 * 工具金丝雀装饰器（spec 324 / T639，Istio 权重分流 + Flagger canary
 * 回滚借鉴）：同一定义的两实现按 weight% 分流真跑——canary 样本 ≥
 * minSamples 且错误率劣化 ≥ tolerance（百分点）→ <b>一次性粘性自动回滚</b>
 * （此后全走 stable，不再自动回升——晋升是人的决定）。per 臂成败计数
 * （结构化错误标记语义同 131：执行失败与校验失败均计败）；回滚判定在
 * 每次调用后顺带评估（事实驱动无定时）。装饰器族无 yml（169 先例——
 * 宿主 wrap）。
 *
 * <p>spec 1606 / T2363：monitor 只护路由决策与计数（内存级），工具执行在锁外——
 * 锁内远程调用会钉住虚拟线程载体（Netty「不阻塞事件循环」铁律同源），且把并行
 * 工具调用无谓串行化。
 */
public final class CanaryToolCallback implements ToolCallback {

    static final String ROLLED_BACK_COUNTER = "buzhou.tool.canary.rolled-back";

    /** 观测：权重与两臂样本/错误/回滚态。 */
    public record View(int weightPercent, long stableCalls, long stableErrors,
            long canaryCalls, long canaryErrors, boolean rolledBack) {
    }

    private final ToolCallback stable;
    private final ToolCallback canary;
    private final int weightPercent;
    private final int minSamples;
    private final double tolerancePoints; // 错误率劣化容差（百分点）
    private final DoubleSupplier random;

    private long stableCalls;
    private long stableErrors;
    private long canaryCalls;
    private long canaryErrors;
    private volatile boolean rolledBack;

    private CanaryToolCallback(ToolCallback stable, ToolCallback canary,
            int weightPercent, int minSamples, double tolerancePoints,
            DoubleSupplier random) {
        if (stable == null || canary == null) {
            throw new IllegalArgumentException("stable/canary 必须非空");
        }
        String stableName = stable.getToolDefinition().name();
        if (!stableName.equals(canary.getToolDefinition().name())) {
            throw new IllegalArgumentException("同名工具才可灰度（stable="
                    + stableName + "，canary=" + canary.getToolDefinition().name() + "）");
        }
        if (weightPercent < 0 || weightPercent > 100) {
            throw new IllegalArgumentException("weightPercent ∈ [0,100]（当前 " + weightPercent + "）");
        }
        if (minSamples < 1) {
            throw new IllegalArgumentException("minSamples >= 1（当前 " + minSamples + "）");
        }
        if (tolerancePoints < 0) {
            throw new IllegalArgumentException("tolerancePoints >= 0（当前 " + tolerancePoints + "）");
        }
        if (random == null) {
            throw new IllegalArgumentException("random 必须非空");
        }
        this.stable = stable;
        this.canary = canary;
        this.weightPercent = weightPercent;
        this.minSamples = minSamples;
        this.tolerancePoints = tolerancePoints;
        this.random = random;
    }

    /**
     * @param stable      稳定版（回滚后的全量走向）
     * @param canary      金丝雀版（定义须与 stable 同名）
     * @param weightPercent 金丝雀流量百分比 [0,100]
     * @param minSamples  回滚判定的 canary 最小样本（防小样本噪声）
     * @param tolerancePoints 错误率劣化容差（百分点，如 10 = 差 10pp 才回滚）
     * @param random      概率源 [0,1)——注入以测试确定性
     */
    public static CanaryToolCallback wrap(ToolCallback stable, ToolCallback canary,
            int weightPercent, int minSamples, double tolerancePoints,
            DoubleSupplier random) {
        return new CanaryToolCallback(stable, canary, weightPercent, minSamples,
                tolerancePoints, random);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return stable.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return route(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return route(toolInput, toolContext);
    }

    private String route(String toolInput, ToolContext toolContext) {
        // spec 1606 / T2363：三段式——路由决策（锁内，内存级）→ 工具执行（锁外，
        // 任意工具 HTTP/MCP 远程为常态：执行时长不得钉住虚拟线程载体，也不串行化
        // 并行工具调用）→ 计数与回滚评估（锁内）。计数原子性与回滚最终一致不变；
        // 唯一弱化是回滚判定可能晚一次调用触发（并发交错下）——事实驱动的评估语义不受影响。
        final boolean toCanary;
        synchronized (this) {
            toCanary = !rolledBack && weightPercent > 0
                    && random.getAsDouble() * 100 < weightPercent;
        }
        if (toCanary) {
            String result = toolContext == null
                    ? canary.call(toolInput) : canary.call(toolInput, toolContext);
            synchronized (this) {
                canaryCalls++;
                if (ToolFeedbackType.isErrorFeedback(result)) {
                    canaryErrors++;
                }
                evaluateRollback();
            }
            return result;
        }
        String result = toolContext == null
                ? stable.call(toolInput) : stable.call(toolInput, toolContext);
        synchronized (this) {
            stableCalls++;
            if (ToolFeedbackType.isErrorFeedback(result)) {
                stableErrors++;
            }
        }
        return result;
    }

    /** 劣化判定：canary 样本足且错误率差 ≥ 容差（百分点）→ 一次粘性回滚（调用方持锁）。 */
    private void evaluateRollback() {
        if (rolledBack || canaryCalls < minSamples) {
            return;
        }
        double canaryRate = (double) canaryErrors / canaryCalls;
        double stableRate = stableCalls == 0 ? 0.0 : (double) stableErrors / stableCalls;
        if ((canaryRate - stableRate) * 100 >= tolerancePoints) {
            rolledBack = true;
            BuzhouMetricsHolder.metrics().counter(ROLLED_BACK_COUNTER);
        }
    }

    /** 观测面。 */
    public synchronized View view() {
        return new View(weightPercent, stableCalls, stableErrors,
                canaryCalls, canaryErrors, rolledBack);
    }

    /** 是否已自动回滚（粘性——人工重新 wrap 才能再灰度）。 */
    public boolean rolledBack() {
        return rolledBack;
    }
}
