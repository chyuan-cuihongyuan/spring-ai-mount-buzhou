package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.DefaultFactStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummarySection;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SectionContent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 事实 Attachment 注入测试（spec 07 Hook→state→Attachment 闭环，ticket 13）。
 *
 * <p>覆盖：注册采集器后事实下一轮出现在注入视图（端到端）、ttl=1 一次性消费、ttl>1 累积注入、
 * 压缩后事实经 CURRENT_STATE 段保留。
 */
class FactInjectionTest {

    @Test
    void activeFactInjectedAsSystemReminderBlock() {
        InMemorySessionStateStore state = new InMemorySessionStateStore();
        FactStore factStore = new DefaultFactStore(state);
        // turn 1 采集一条事实（ttl=3，turn 2 仍 active）
        factStore.save("s1", new Fact(Fact.keyFor("risk", "table-1"),
                "高风险表已修改", "risk", 1, 3));
        AttachmentRenderer renderer = new TestAttachmentRenderer(factStore);

        InjectionViewProcessor ivp = newProcessor(renderer);
        List<BuzhouMessage> stored = List.of(
                userMsg(1, "修改表"), asstMsg(1, "已修改"));
        List<BuzhouMessage> view = ivp.process("s1", stored, 2);

        // 含事实 <system-reminder> 块
        assertThat(view).anyMatch(m -> m.metadata().containsKey("facts"));
        BuzhouMessage factBlock = view.stream().filter(m -> m.metadata().containsKey("facts"))
                .findFirst().orElseThrow();
        assertThat(factBlock.content()).contains("高风险表已修改");
    }

    @Test
    void ttlOneFactExpiresNextNextTurn() {
        InMemorySessionStateStore state = new InMemorySessionStateStore();
        FactStore factStore = new DefaultFactStore(state);
        factStore.save("s1", new Fact(Fact.keyFor("p", "once"), "v", "p", 1, 1));
        AttachmentRenderer renderer = new TestAttachmentRenderer(factStore);
        InjectionViewProcessor ivp = newProcessor(renderer);

        // turn 1（采集轮）：active (1-1=0 < 1)
        assertThat(renderer.render("s1", 1)).isPresent();
        // turn 2：expired (2-1=1, not < 1)
        assertThat(renderer.render("s1", 2)).isEmpty();
    }

    @Test
    void ttlMultipleAccumulatesAcrossTurns() {
        InMemorySessionStateStore state = new InMemorySessionStateStore();
        FactStore factStore = new DefaultFactStore(state);
        factStore.save("s1", new Fact(Fact.keyFor("p", "f"), "持久事实", "p", 1, 5));
        AttachmentRenderer renderer = new TestAttachmentRenderer(factStore);

        // turn 1-5 均注入
        for (int t = 1; t <= 5; t++) {
            assertThat(renderer.render("s1", t)).isPresent();
        }
        // turn 6 过期
        assertThat(renderer.render("s1", 6)).isEmpty();
    }

    @Test
    void factPreservedInCurrentStateAfterCompaction() {
        // 直接验证 appendCurrentState 把事实追加到 CURRENT_STATE 段（P0 不丢，压缩后保留）
        EnumMap<SummarySection, SectionContent> sections = new EnumMap<>(SummarySection.class);
        sections.put(SummarySection.CURRENT_STATE, SectionContent.full("原始现场"));
        NineSectionSummary summary = new NineSectionSummary(1, 1, sections);

        NineSectionSummary enriched = summary.appendCurrentState("[已采集事实]\n死保事实");
        assertThat(enriched.render()).contains("死保事实");
        assertThat(enriched.render()).contains("原始现场");
        // CURRENT_STATE 段仍存在且含两段
        SectionContent cs = enriched.sections().get(SummarySection.CURRENT_STATE);
        assertThat(cs.body()).contains("原始现场");
        assertThat(cs.body()).contains("死保事实");
    }

    private InjectionViewProcessor newProcessor(AttachmentRenderer renderer) {
        DefaultMicroCompactor compactor = new DefaultMicroCompactor(new DefaultCompletedTurnDetector());
        InjectionViewProcessor ivp = new InjectionViewProcessor(compactor,
                t -> MicroCompactionPolicy.defaults(), 1,
                new DefaultBudgetCalculator(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver(Map.of()),
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator()),
                new io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore()),
                new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3),
                new StubSummaryModel(), "stub", 1, null);
        ivp.setAttachmentRenderer(renderer);
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

    /** 测试用 AttachmentRenderer：渲染所有 active facts 为 "producer: value" 行。 */
    static class TestAttachmentRenderer implements AttachmentRenderer {
        private final FactStore factStore;

        TestAttachmentRenderer(FactStore factStore) {
            this.factStore = factStore;
        }

        @Override
        public Optional<String> render(String sessionId, int currentTurn) {
            List<Fact> active = factStore.activeFacts(sessionId, currentTurn);
            if (active.isEmpty()) {
                return Optional.empty();
            }
            StringBuilder sb = new StringBuilder("已采集事实：\n");
            for (Fact f : active) {
                sb.append("- ").append(f.producer()).append(": ").append(f.value()).append("\n");
            }
            return Optional.of(sb.toString().strip());
        }
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
