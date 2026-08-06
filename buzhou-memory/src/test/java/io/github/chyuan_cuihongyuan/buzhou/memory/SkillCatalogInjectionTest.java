package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SkillCatalogRenderer;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetInput;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 技能清单（Skill Catalog）注入测试（spec 04，ticket 14）。
 *
 * <p>覆盖：清单下一轮以 {@code <system-reminder>} 块出现在注入视图、无摘要模型时仍注入、
 * 清单 token 计入 {@code BudgetInput.systemPrompt} 系统侧固定扣除。
 */
class SkillCatalogInjectionTest {

    @Test
    void catalogInjectedAsSystemReminderBlock() {
        InjectionViewProcessor ivp = newProcessor(sessionId -> Optional.of(
                "## 可用技能（Skill Catalog）\n- code-review: 代码评审清单"));
        List<BuzhouMessage> view = ivp.process("s1",
                List.of(userMsg(1, "q"), asstMsg(1, "a")), 2);

        assertThat(view).anyMatch(m -> m.metadata().containsKey("skill-catalog"));
        BuzhouMessage catalogBlock = view.stream()
                .filter(m -> m.metadata().containsKey("skill-catalog"))
                .findFirst().orElseThrow();
        assertThat(catalogBlock.content()).contains("可用技能")
                .contains("code-review: 代码评审清单");
        // 块在近期原文之前、为 SYSTEM 角色
        assertThat(catalogBlock.role()).isEqualTo(Role.SYSTEM);
    }

    @Test
    void emptyCatalogNotInjected() {
        InjectionViewProcessor ivp = newProcessor(sessionId -> Optional.empty());
        List<BuzhouMessage> view = ivp.process("s1",
                List.of(userMsg(1, "q"), asstMsg(1, "a")), 2);

        assertThat(view).noneMatch(m -> m.metadata().containsKey("skill-catalog"));
    }

    @Test
    void catalogInjectedEvenWithoutSummaryModel() {
        InjectionViewProcessor ivp = newProcessor(sessionId -> Optional.of(
                "## 可用技能\n- sql-tuning: 慢 SQL 诊断"), null);
        List<BuzhouMessage> view = ivp.process("s1",
                List.of(userMsg(1, "q"), asstMsg(1, "a")), 2);

        assertThat(view).anyMatch(m -> m.metadata().containsKey("skill-catalog")
                && m.content().contains("sql-tuning"));
    }

    @Test
    void catalogTokensCountedAsSystemSideBudgetDeduction() {
        // spec 04：清单 token 计 BudgetInput.systemPrompt 固定扣除（与事实块同口径）
        CapturingBudgetCalculator capturing = new CapturingBudgetCalculator();
        DefaultMicroCompactor compactor = new DefaultMicroCompactor(new DefaultCompletedTurnDetector());
        InjectionViewProcessor ivp = new InjectionViewProcessor(compactor,
                t -> MicroCompactionPolicy.defaults(), 1, capturing,
                new SummaryStoreBridge(new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore()),
                new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3),
                new StubSummaryModel(), "stub", 1, null, 4000);
        ivp.setSkillCatalogRenderer(sessionId -> Optional.of("## 可用技能\n- 入账技能: 计入预算"));
        ivp.process("s1", List.of(userMsg(1, "q"), asstMsg(1, "a")), 2);

        assertThat(capturing.lastInput).isNotNull();
        assertThat(capturing.lastInput.systemPrompt()).contains("入账技能");
    }

    /** 捕获 BudgetInput 的计算器（验证清单 token 入账路径）。 */
    static class CapturingBudgetCalculator extends DefaultBudgetCalculator {
        volatile BudgetInput lastInput;

        CapturingBudgetCalculator() {
            super(new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver(Map.of()),
                    new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator());
        }

        @Override
        public io.github.chyuan_cuihongyuan.buzhou.memory.budget.BudgetReport evaluate(BudgetInput input) {
            this.lastInput = input;
            return super.evaluate(input);
        }
    }

    private InjectionViewProcessor newProcessor(SkillCatalogRenderer renderer) {
        return newProcessor(renderer, new StubSummaryModel());
    }

    private InjectionViewProcessor newProcessor(SkillCatalogRenderer renderer, ChatModel summaryModel) {
        DefaultMicroCompactor compactor = new DefaultMicroCompactor(new DefaultCompletedTurnDetector());
        InjectionViewProcessor ivp = new InjectionViewProcessor(compactor,
                t -> MicroCompactionPolicy.defaults(), 1,
                new DefaultBudgetCalculator(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver(Map.of()),
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator()),
                new SummaryStoreBridge(new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore()),
                new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3),
                summaryModel, "stub", 1, null, 4000);
        ivp.setSkillCatalogRenderer(renderer);
        return ivp;
    }

    private static BuzhouMessage userMsg(int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 0,
                Role.USER, content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private static BuzhouMessage asstMsg(int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), "s1", turn, 0,
                Role.ASSISTANT, content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** Stub ChatModel：返回固定九段摘要文本（让 summaryGenerator 能解析）。 */
    static class StubSummaryModel implements ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            String text = """
                    ## USER_INTENT（用户核心诉求）
                    测试意图

                    ## CURRENT_STATE（当前工作现场）
                    测试现场

                    ## NEXT_STEP（下一步）
                    无
                    """;
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
