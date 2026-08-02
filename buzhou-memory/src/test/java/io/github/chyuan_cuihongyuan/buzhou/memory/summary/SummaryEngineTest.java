package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryEngineTest {

    private ChatModel scriptedSummaryModel(List<String> prompts, String response) {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                prompts.add(prompt.getInstructions().getFirst().getText());
                return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
            }
        };
    }

    private BuzhouMessage msg(String sessionId, int turn, Role role, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0, role,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    @Test
    void parsesNineSectionsAndStripsAnalysis() {
        String output = """
                <analysis>时间序复盘草稿</analysis>
                ## USER_INTENT
                排查订单 ORD-1 支付卡单
                ## CURRENT_STATE
                已查到支付流水缺失
                ## NEXT_STEP
                检查网关回调
                ## PENDING_TASKS
                无
                ## ERRORS_FIXES
                无
                ## KEY_ARTIFACTS
                流水号 PAY-9
                ## PROBLEM_SOLVING
                定位到网关
                ## TECHNICAL_CONCEPTS
                支付状态机
                ## USER_MESSAGES_LOG
                q1 q2
                """;
        DefaultSummaryGenerator generator = new DefaultSummaryGenerator();
        EnumMap<SummarySection, SectionContent> sections = generator.parse(output);

        assertThat(sections).hasSize(9);
        assertThat(sections.get(SummarySection.USER_INTENT).body()).contains("ORD-1");
        assertThat(sections.get(SummarySection.USER_INTENT).body()).doesNotContain("草稿");
    }

    @Test
    void mergePromptContainsPreviousSummaryForIncrementalUpdate() {
        List<String> prompts = new ArrayList<>();
        ChatModel model = scriptedSummaryModel(prompts, "## USER_INTENT\n意图\n");
        EnumMap<SummarySection, SectionContent> previous = new EnumMap<>(SummarySection.class);
        previous.put(SummarySection.USER_INTENT, SectionContent.full("旧意图"));
        previous.put(SummarySection.CURRENT_STATE, SectionContent.full("旧现场"));

        new DefaultSummaryGenerator().merge(
                new NineSectionSummary(1, 5, previous),
                List.of(msg("s", 6, Role.USER, "新进展")), 6, null, model);

        assertThat(prompts.getFirst()).contains("第 1 代").contains("旧意图").contains("新进展")
                .contains("合并更新");
    }

    @Test
    void degraderDropsP3FirstAndNeverTouchesP0() {
        EnumMap<SummarySection, SectionContent> sections = new EnumMap<>(SummarySection.class);
        sections.put(SummarySection.USER_INTENT, SectionContent.full("意图正文" + "x".repeat(100)));
        sections.put(SummarySection.USER_MESSAGES_LOG, SectionContent.full("x".repeat(500)));
        sections.put(SummarySection.PROBLEM_SOLVING, SectionContent.full("y".repeat(300)));
        NineSectionSummary summary = new NineSectionSummary(1, 5, sections);

        NineSectionSummary degraded = new DefaultSummaryDegrader().degradeToFit(
                summary, 80, new CharHeuristicTokenEstimator());

        assertThat(degraded.sections().get(SummarySection.USER_MESSAGES_LOG).form())
                .isEqualTo(SectionContent.Form.GIST);
        assertThat(degraded.sections().get(SummarySection.USER_INTENT).form())
                .isEqualTo(SectionContent.Form.FULL);
    }

    @Test
    void circuitBreakerOpensAfterThresholdFailures() {
        SummaryCircuitBreaker breaker = new SummaryCircuitBreaker(3);
        assertThat(breaker.allows("s")).isTrue();
        breaker.onFailure("s");
        breaker.onFailure("s");
        assertThat(breaker.allows("s")).isTrue();
        breaker.onFailure("s");
        assertThat(breaker.allows("s")).isFalse();
        breaker.onSuccess("s");
        assertThat(breaker.allows("s")).isTrue();
    }
}
