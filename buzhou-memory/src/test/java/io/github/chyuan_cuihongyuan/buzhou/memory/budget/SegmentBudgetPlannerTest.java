package io.github.chyuan_cuihongyuan.buzhou.memory.budget;

import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SectionContent;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummarySection;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T23 段级预算渲染（docs/spec/11 memory，来源 Letta memory blocks）：
 * 注入视图渲染九段每段末尾 chars_current/chars_limit 页脚；超限段带精简告警（P3 最先削减）。
 */
class SegmentBudgetPlannerTest {

    private NineSectionSummary summaryOf(String userIntent, String messageLog) {
        EnumMap<SummarySection, SectionContent> sections = new EnumMap<>(SummarySection.class);
        sections.put(SummarySection.USER_INTENT, SectionContent.full(userIntent));
        sections.put(SummarySection.USER_MESSAGES_LOG, SectionContent.full(messageLog));
        return new NineSectionSummary(1, 3, sections);
    }

    @Test
    void rendersPerSectionCurrentAndLimitFooters() {
        NineSectionSummary summary = summaryOf("排查订单 ORD-7", "- 用户催促尽快");

        String rendered = SegmentBudgetPlanner.renderWithFooters(summary, 2000);

        // 头部预算提示（引导模型自削 P3）
        assertThat(rendered).startsWith(SegmentBudgetPlanner.BUDGET_HEADER);
        // P0 段页脚：当前/上限
        assertThat(rendered).contains("## USER_INTENT");
        assertThat(rendered).contains("（本段 1" + 0);
        assertThat(rendered).contains(" 字符）");
        // 上限按优先级拆分（P0 40%、P3 10%；2000 token ×4 = 8000 字符 → P0=3200、P3=800）
        assertThat(rendered).contains("/3200 字符）");
        assertThat(rendered).contains("/800 字符）");
        // 正文保真
        assertThat(rendered).contains("排查订单 ORD-7");
    }

    @Test
    void overLimitSectionCarriesTrimWarning() {
        String hugeLog = "- 用户消息\n".repeat(200); // 1600 字符 > P3 上限 800
        NineSectionSummary summary = summaryOf("排查订单 ORD-7", hugeLog);

        String rendered = SegmentBudgetPlanner.renderWithFooters(summary, 2000);

        // 超限段显式告警（评测式断言的锚点：P3 被点名削减、P0 保持完整）
        assertThat(rendered).contains("已超限，请优先精简本段内容");
        assertThat(rendered).contains("USER_MESSAGES_LOG");
        // P0 未超限 → 无告警标记挂在 USER_INTENT 段
        int intentFooter = rendered.indexOf("USER_INTENT");
        assertThat(rendered.indexOf("已超限", intentFooter))
                .isGreaterThan(rendered.indexOf("USER_MESSAGES_LOG"));
    }

    @Test
    void charLimitFollowsPriorityShares() {
        assertThat(SegmentBudgetPlanner.charLimitFor(SummarySection.USER_INTENT, 2000))
                .isEqualTo(3200);
        assertThat(SegmentBudgetPlanner.charLimitFor(SummarySection.PENDING_TASKS, 2000))
                .isEqualTo(2400);
        assertThat(SegmentBudgetPlanner.charLimitFor(SummarySection.PROBLEM_SOLVING, 2000))
                .isEqualTo(1600);
        assertThat(SegmentBudgetPlanner.charLimitFor(SummarySection.USER_MESSAGES_LOG, 2000))
                .isEqualTo(800);
        // 极小预算也有下限（防零上限）
        assertThat(SegmentBudgetPlanner.charLimitFor(SummarySection.USER_MESSAGES_LOG, 1))
                .isGreaterThanOrEqualTo(200);
    }
}
