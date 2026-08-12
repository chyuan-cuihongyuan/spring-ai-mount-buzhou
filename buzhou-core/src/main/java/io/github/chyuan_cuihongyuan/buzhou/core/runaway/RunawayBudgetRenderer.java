package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;

import java.util.Optional;

/**
 * 软退出提醒渲染器（spec「死循环与失控检测 · 软退出通道」）。
 *
 * <p>达软阈值时经既有 {@link AttachmentRenderer} 注入通道向模型注入「剩余步数预算」信号，
 * 让模型<b>主动收尾</b>而非被硬切。复用既有注入路径（被 {@code CompositeAttachmentRenderer}
 * 折进 {@code InjectionViewProcessor} 的 {@code <system-reminder>} 块，与事实块同位），不新增注入槽。
 *
 * <p><b>触发条件</b>：读 {@link RunawayCounters} 的轮次级步数计数（renderer 在 memory advisor(+400)
 * 运行、步数在 hook(+600) {@code beforeModel} 递增——本步读到「上一步末」计数，一步滞后，可接受；
 * 模型看到「进入本次调用时的预算」）。当 {@code remaining / limit < softThresholdRatio} 时渲染
 * 「剩余步数预算：N/M，请尽快收尾并给出结论」，否则返回空。
 *
 * <p><b>软阈值语义</b>：只注入信号、不递减计数、不阻断。软退出是「提示模型主动收尾」，不是惩罚；
 * 合法长任务不受影响（remaining 未跌破阈值即不注入）。仅在有 {@code per-turn.max-steps} 时生效。
 *
 * <p>注入字符计入既有 {@code buzhou.facts.max-inject-chars} 共享总量（与事实块共享同一份预算，
 * 由 {@code CompositeAttachmentRenderer} 粗粒度兜底截断）。
 */
public class RunawayBudgetRenderer implements AttachmentRenderer {

    private final BuzhouRunawayProperties props;
    private final RunawayCounters counters;

    public RunawayBudgetRenderer(BuzhouRunawayProperties props, RunawayCounters counters) {
        this.props = props;
        this.counters = counters;
    }

    @Override
    public Optional<String> render(String sessionId, int currentTurn) {
        if (!props.enabled()) {
            return Optional.empty();
        }
        Integer maxSteps = props.perTurn() != null ? props.perTurn().maxSteps() : null;
        if (maxSteps == null || maxSteps <= 0) {
            // 无步数硬顶时不注入（软退出仅在有 max-steps 时生效）
            return Optional.empty();
        }
        int used = counters.steps(sessionId);
        int remaining = maxSteps - used;
        if (remaining <= 0) {
            return Optional.empty();
        }
        double ratio = (double) remaining / maxSteps;
        if (ratio >= props.effectiveSoftThresholdRatio()) {
            return Optional.empty();
        }
        return Optional.of(formatReminder(remaining, maxSteps));
    }

    /** 软退出提醒文案模板（落 spec 文档同步）。 */
    static String formatReminder(int remaining, int limit) {
        return "剩余步数预算：" + remaining + "/" + limit + "，请尽快收尾并给出结论。";
    }
}
