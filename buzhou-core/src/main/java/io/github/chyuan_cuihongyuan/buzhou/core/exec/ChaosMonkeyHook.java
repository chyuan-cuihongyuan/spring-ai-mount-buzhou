package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

/**
 * 工具混沌注入 hook（spec 322 / T635，Netflix Chaos Monkey 借鉴）：order 235
 * （熔断 240 前）按概率袭击——<b>延迟</b>（sleep 固定毫秒——慢工具演练：舱
 * acquire-timeout/泳道超时被真实验证）与<b>故障</b>（block 结构化错误标记
 * ——{@link ToolFeedbackType#isErrorFeedback} 命中，模型可读可改道，重试/
 * 熔断真实触发）。一次调用至多一种袭击；概率源可注入（测试确定性）；
 * include 清单空 = 全量；运行时 {@link #setEnabled} 启停（演练窗口）。
 */
public final class ChaosMonkeyHook implements BuzhouHook {

    public static final int ORDER = 235;
    static final String LATENCY_COUNTER = "buzhou.chaos.latency-injected";
    static final String FAULT_COUNTER = "buzhou.chaos.fault-injected";

    private final double latencyPercent;
    private final long latencyMillis;
    private final double exceptionPercent;
    private final Set<String> tools;
    private final DoubleSupplier random;
    private volatile boolean enabled;
    private final AtomicLong latencyInjected = new AtomicLong();
    private final AtomicLong faultsInjected = new AtomicLong();

    /**
     * @param latencyPercent   延迟袭击概率百分比 [0,100]（0 = 不袭）
     * @param latencyMillis    延迟毫秒（0 = 延迟档不生效）
     * @param exceptionPercent 故障袭击概率百分比 [0,100]
     * @param tools            include 清单（空 = 全量工具）
     * @param enabled          初始开关（装配 true；测试可关）
     * @param random           概率源 [0,1)——注入以测试确定性
     */
    public ChaosMonkeyHook(double latencyPercent, long latencyMillis,
            double exceptionPercent, Set<String> tools, boolean enabled,
            DoubleSupplier random) {
        if (latencyPercent < 0 || latencyPercent > 100) {
            throw new IllegalArgumentException("latencyPercent ∈ [0,100]（当前 " + latencyPercent + "）");
        }
        if (exceptionPercent < 0 || exceptionPercent > 100) {
            throw new IllegalArgumentException("exceptionPercent ∈ [0,100]（当前 " + exceptionPercent + "）");
        }
        if (latencyMillis < 0) {
            throw new IllegalArgumentException("latencyMillis >= 0（当前 " + latencyMillis + "）");
        }
        if (random == null) {
            throw new IllegalArgumentException("random 必须非空");
        }
        this.latencyPercent = latencyPercent;
        this.latencyMillis = latencyMillis;
        this.exceptionPercent = exceptionPercent;
        this.tools = tools == null ? Set.of() : Set.copyOf(tools);
        this.enabled = enabled;
        this.random = random;
    }

    @Override
    public String name() {
        return "ChaosMonkeyHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        if (!enabled || ctx == null || ctx.toolName() == null) {
            return HookResult.CONTINUE;
        }
        if (!tools.isEmpty() && !tools.contains(ctx.toolName())) {
            return HookResult.CONTINUE; // include 清单外不袭
        }
        if (latencyMillis > 0 && latencyPercent > 0
                && random.getAsDouble() * 100 < latencyPercent) {
            try {
                Thread.sleep(latencyMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latencyInjected.incrementAndGet();
            BuzhouMetricsHolder.metrics().counter(LATENCY_COUNTER);
            return HookResult.CONTINUE; // 至多一种袭击——慢完照常执行
        }
        if (exceptionPercent > 0 && random.getAsDouble() * 100 < exceptionPercent) {
            faultsInjected.incrementAndGet();
            BuzhouMetricsHolder.metrics().counter(FAULT_COUNTER);
            return HookResult.block(ToolFeedbackType.EXECUTION_FAILURE.marker()
                    + "\n工具：" + ctx.toolName()
                    + "\n原因：混沌注入（演练）——本次调用被人为失败");
        }
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        return HookResult.CONTINUE; // 混沌不篡改真实结果（结果篡改另立项）
    }

    /** 运行时启停（演练窗口——不重启）。 */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** 延迟袭击累计（观测面）。 */
    public long latencyInjectedCount() {
        return latencyInjected.get();
    }

    /** 故障袭击累计（观测面）。 */
    public long faultInjectedCount() {
        return faultsInjected.get();
    }
}
