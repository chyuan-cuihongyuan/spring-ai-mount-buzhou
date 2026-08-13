package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompactionCheckpoints;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-13 / T40 压缩前检查点与三档回滚：<b>视图处理器接缝</b>验证——
 * 折叠前保存检查点；回滚后注入视图恢复为压缩前原文窗口（同 Turn 一致、下一 Turn 失效）；
 * 摘要失效档跳过摘要注入、重新折叠后清除；档 3 清事实台账。
 *
 * <p>断言落点说明：回滚契约在本接缝（MemoryViewProcessor）验证——模型 prompt 的最终组装
 * 叠加 Spring AI advisor 内部历史，不属本切片断言范围（测试哲学：只测自有接缝的外部行为）。
 */
class CompactionCheckpointTest {

    private static final String NINE_SECTIONS = """
            ## USER_INTENT
            排查订单
            ## CURRENT_STATE
            第 28 步
            ## NEXT_STEP
            查网关
            ## PENDING_TASKS
            无
            ## ERRORS_FIXES
            无
            ## KEY_ARTIFACTS
            PAY-9
            ## PROBLEM_SOLVING
            定位
            ## TECHNICAL_CONCEPTS
            状态机
            ## USER_MESSAGES_LOG
            若干
            """;

    private List<BuzhouMessage> bigHistory(String sessionId, int turns) {
        List<BuzhouMessage> history = new ArrayList<>();
        String big = "x".repeat(3000);
        for (int turn = 1; turn <= turns; turn++) {
            history.add(msg(sessionId, turn, 0, Role.USER, "第 " + turn + " 步"));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-" + turn, "query", "{}")),
                    null, null, null, Map.of(), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 2,
                    Role.TOOL, big, List.of(), "tc-" + turn, null, null,
                    Map.of("toolName", "query"), Instant.now()));
            history.add(msg(sessionId, turn, 3, Role.ASSISTANT, "完成 " + turn));
        }
        return history;
    }

    private static BuzhouMessage msg(String sessionId, int turn, int seq, Role role, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, seq, role,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 直接构造 IVP（自有接缝）：小窗口触发折叠 + 检查点。 */
    private InjectionViewProcessor processor(BuzhouStores stores, ChatModel summaryModel) {
        DefaultBudgetCalculator budgetCalculator = new DefaultBudgetCalculator(
                new TableContextWindowResolver(Map.of("tiny", 13000)),
                new CharHeuristicTokenEstimator());
        InjectionViewProcessor ivp = new InjectionViewProcessor(
                new DefaultMicroCompactor(new DefaultCompletedTurnDetector()),
                name -> MicroCompactionPolicy.defaults(), 1,
                budgetCalculator, new SummaryStoreBridge(stores.summaryStore()),
                new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3), summaryModel,
                "tiny", 2, null, 4000);
        ivp.setSessionStateStore(stores.sessionStateStore());
        ivp.setCheckpoints(new CompactionCheckpoints(stores.sessionStateStore()));
        return ivp;
    }

    private static ChatModel summaryModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage(NINE_SECTIONS))));
            }
        };
    }

    @Test
    void foldSavesCheckpointThenRollbackRestoresRawWindowPerTurn() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "ckpt-" + UUID.randomUUID();
        List<BuzhouMessage> history = bigHistory(sessionId, 30);
        InjectionViewProcessor ivp = processor(stores, summaryModel());

        // Turn 31 触发折叠：视图含摘要注入，检查点已保存
        List<BuzhouMessage> folded = ivp.process(sessionId, history, 31);
        assertThat(folded.stream().anyMatch(m -> m.role() == Role.SYSTEM
                && m.content() != null && m.content().contains("USER_INTENT"))).isTrue();
        CompactionCheckpoints checkpoints = new CompactionCheckpoints(stores.sessionStateStore());
        assertThat(checkpoints.latestWindow(sessionId)).isPresent();
        assertThat(checkpoints.latestWindow(sessionId).get()).isNotEmpty();

        // 回滚档 1：同 Turn 的<b>每次</b>视图生成都恢复为压缩前原文窗口（无摘要合成消息）
        assertThat(checkpoints.rollback(sessionId,
                CompactionCheckpoints.RollbackLevel.MESSAGES_ONLY)).isTrue();
        List<BuzhouMessage> restored1 = ivp.process(sessionId, history, 31);
        List<BuzhouMessage> restored2 = ivp.process(sessionId, history, 31);
        assertThat(restored1).hasSize(history.size());
        assertThat(restored2).hasSize(history.size());
        assertThat(restored1.stream().noneMatch(m -> m.role() == Role.SYSTEM
                && m.content() != null && m.content().contains("USER_INTENT"))).isTrue();
        // 恢复的是<b>原文</b>：大工具结果未占位、无 evidence 指针
        assertThat(restored1.stream().anyMatch(m -> m.role() == Role.TOOL
                && m.content() != null && m.content().contains("x".repeat(200)))).isTrue();
        assertThat(restored1.stream().noneMatch(m -> m.content() != null
                && m.content().contains("evidence-id="))).isTrue();

        // 下一 Turn：回滚自动失效，恢复正常压缩链路（摘要注入回归）
        List<BuzhouMessage> next = ivp.process(sessionId, history, 32);
        assertThat(next.stream().anyMatch(m -> m.role() == Role.SYSTEM
                && m.content() != null && m.content().contains("USER_INTENT"))).isTrue();
    }

    @Test
    void summaryInvalidationLevelHidesSummaryUntilNewFoldClears() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "ckpt-inv-" + UUID.randomUUID();
        List<BuzhouMessage> history = bigHistory(sessionId, 30);
        InjectionViewProcessor ivp = processor(stores, summaryModel());
        ivp.process(sessionId, history, 31); // 折叠 + 摘要落库

        // 回滚档 2：摘要失效 → 视图跳过摘要注入
        CompactionCheckpoints checkpoints = new CompactionCheckpoints(stores.sessionStateStore());
        assertThat(checkpoints.rollback(sessionId,
                CompactionCheckpoints.RollbackLevel.PLUS_SUMMARY_INVALIDATION)).isTrue();
        List<BuzhouMessage> restored = ivp.process(sessionId, history, 31);
        assertThat(restored.stream().noneMatch(m -> m.role() == Role.SYSTEM
                && m.content() != null && m.content().contains("USER_INTENT"))).isTrue();
        assertThat(CompactionCheckpoints.summaryInvalidated(
                stores.sessionStateStore(), sessionId)).isTrue();

        // 下一 Turn 正常链路：重新折叠成功 → 失效标记被清除（新一轮摘要生效）
        List<BuzhouMessage> next = ivp.process(sessionId, history, 32);
        assertThat(next.stream().anyMatch(m -> m.role() == Role.SYSTEM
                && m.content() != null && m.content().contains("USER_INTENT"))).isTrue();
        assertThat(CompactionCheckpoints.summaryInvalidated(
                stores.sessionStateStore(), sessionId)).isFalse();
    }

    @Test
    void factLedgerLevelClearsFactKeysOnly() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "ckpt-ledger-" + UUID.randomUUID();
        stores.sessionStateStore().put(sessionId,
                new StateEntry("fact.order", "ORD-1", "hook", 1, null, Instant.now()));
        stores.sessionStateStore().put(sessionId,
                new StateEntry("memory.checkpoint.latest",
                        "{\"cutoffTurn\":5,\"window\":[{\"id\":\"m1\",\"sessionId\":\""
                                + sessionId + "\",\"turnSeq\":1,\"seqInTurn\":0,\"role\":\"USER\","
                                + "\"content\":\"c\",\"toolCalls\":[],\"toolCallId\":null,"
                                + "\"reasoningContent\":null,\"reasoningSignature\":null,"
                                + "\"metadata\":{},\"createdAt\":\"2026-08-14T00:00:00Z\"}]}",
                        "CompactionCheckpoints", 5, null, Instant.now()));
        stores.sessionStateStore().put(sessionId,
                new StateEntry("taint.context", "UNTRUSTED:x", "hook", 1, null, Instant.now()));

        CompactionCheckpoints checkpoints = new CompactionCheckpoints(stores.sessionStateStore());
        assertThat(checkpoints.rollback(sessionId,
                CompactionCheckpoints.RollbackLevel.WITH_FACT_LEDGER)).isTrue();
        // fact.* 清除；非 fact 键保留；无检查点时回滚拒绝
        assertThat(stores.sessionStateStore().get(sessionId, "fact.order")).isEmpty();
        assertThat(stores.sessionStateStore().get(sessionId, "taint.context")).isPresent();
        assertThat(checkpoints.rollback("no-checkpoint-session",
                CompactionCheckpoints.RollbackLevel.MESSAGES_ONLY)).isFalse();
    }
}
