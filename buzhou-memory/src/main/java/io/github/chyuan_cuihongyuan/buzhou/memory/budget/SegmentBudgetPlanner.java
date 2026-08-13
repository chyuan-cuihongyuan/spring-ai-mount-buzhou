package io.github.chyuan_cuihongyuan.buzhou.memory.budget;

import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SectionContent;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummarySection;

import java.util.Map;

/**
 * 段级预算规划与渲染（wayfinder T23 / docs/spec/11 memory，来源 Letta memory blocks）：
 * 把 Buzhou 既有的动态预算（全局 token）拆解为<b>每段字符预算</b>渲染给模型——
 * 注入视图在九段每段末尾渲染 {@code chars_current / chars_limit} 页脚，模型感知预算压力、
 * 主动削低优先级（P3）段，而非被动等强制压缩。
 *
 * <p>分配比例按段优先级：P0 40% / P1 30% / P2 20% / P3 10%（P0 死保最大份额）；
 * 字符预算 = token 预算 × 4（与 CharHeuristicTokenEstimator 启发式一致）。
 */
public final class SegmentBudgetPlanner {

    /** 头部预算提示（渲染一次，引导模型自削 P3）。 */
    public static final String BUDGET_HEADER = "[预算提示] 以下各段末尾标注「本段 当前/上限 字符」。"
            + "超限时请主动精简低优先级段（P3 用户消息清单最优先削减），P0 段（用户核心诉求/当前工作现场/下一步）保持完整。";

    private SegmentBudgetPlanner() {
    }

    /** 段字符上限（token 预算 ×4 ×优先级占比；至少 200 字符防零上限）。 */
    public static int charLimitFor(SummarySection section, int summaryTokenBudget) {
        int totalChars = Math.max(summaryTokenBudget, 250) * 4;
        int share = switch (section.priority()) {
            case 0 -> 40;
            case 1 -> 30;
            case 2 -> 20;
            default -> 10;
        };
        return Math.max(totalChars * share / 100, 200);
    }

    /** 渲染带预算页脚的摘要（每段末尾「本段 X/Y 字符」+ 超限告警）。 */
    public static String renderWithFooters(NineSectionSummary summary, int summaryTokenBudget) {
        StringBuilder sb = new StringBuilder(BUDGET_HEADER).append('\n');
        for (Map.Entry<SummarySection, SectionContent> entry : summary.sections().entrySet()) {
            if (entry.getValue() == null || entry.getValue().body().isBlank()) {
                continue;
            }
            String body = entry.getValue().render();
            sb.append("## ").append(entry.getKey().name())
                    .append("（").append(entry.getKey().displayName()).append("）\n")
                    .append(body).append('\n');
            int current = body.length();
            int limit = charLimitFor(entry.getKey(), summaryTokenBudget);
            sb.append("（本段 ").append(current).append('/').append(limit).append(" 字符");
            if (current > limit) {
                sb.append("；已超限，请优先精简本段内容");
            }
            sb.append("）\n");
        }
        return sb.toString();
    }
}
