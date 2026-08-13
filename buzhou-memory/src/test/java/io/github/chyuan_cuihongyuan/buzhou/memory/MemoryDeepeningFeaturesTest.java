package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionInterrupts;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.memory.episodic.EpisodeLedger;
import io.github.chyuan_cuihongyuan.buzhou.memory.evaluation.CompactionFidelityEval;
import io.github.chyuan_cuihongyuan.buzhou.memory.recall.RecallSearch;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SectionContent;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummarySection;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import org.junit.jupiter.api.Test;
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
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-15/14/08/26 聚合验收：向量 recall 四模（降级语义）、保真 eval、
 * interrupt/resume 按 toolCallId 注入、episodic few-shot。
 */
class MemoryDeepeningFeaturesTest {

    /** 字符二元组向量化（中文无分词场景的确定性语义近似：bigram 重叠可比较）。 */
    private static final io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider BOW =
            text -> {
                float[] vector = new float[4096];
                String squeezed = text == null ? "" : text.replaceAll("\\s+", "");
                for (int i = 0; i + 2 <= squeezed.length(); i++) {
                    vector[Math.floorMod(squeezed.substring(i, i + 2).hashCode(), 4096)] += 1f;
                }
                return vector;
            };

    // ---- impl-15：recall 四模 ----

    @Test
    void recallSearchFourModesWithExplicitDegradation() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "recall-" + UUID.randomUUID();
        List<BuzhouMessage> history = new ArrayList<>();
        history.add(toolMsg(sessionId, 1, "tc-1", "订单 ORD-1 已发货，物流单号 SF-999"));
        history.add(toolMsg(sessionId, 2, "tc-2", "退款政策：七天无理由退货"));
        history.add(toolMsg(sessionId, 3, "tc-3", "订单 ORD-2 派送中"));
        stores.messageStore().append(sessionId, history);

        RecallSearch withVectors = new RecallSearch(BOW);
        RecallSearch noVectors = new RecallSearch(null);

        // TEXT：词元命中
        var textHits = withVectors.search(history,
                new RecallSearch.Query(RecallSearch.Mode.TEXT, "物流单号", null, null, 5));
        assertThat(textHits).hasSize(1);
        assertThat(textHits.getFirst().message().content()).contains("SF-999");
        // TIME：轮次倒序
        var timeHits = withVectors.search(history,
                new RecallSearch.Query(RecallSearch.Mode.TIME, null, null, null, 5));
        assertThat(timeHits.stream().map(RecallSearch.Hit::turn))
                .containsExactly(3, 2, 1);
        // EMBEDDING：语义近邻（词包重叠）+ 未注入显式降级
        assertThat(withVectors.vectorReady()).isTrue();
        assertThat(withVectors.search(history, new RecallSearch.Query(
                RecallSearch.Mode.EMBEDDING, "退货 退款 政策", null, null, 1))
                .getFirst().message().content()).contains("七天无理由");
        assertThat(noVectors.vectorReady()).isFalse();
        assertThat(noVectors.search(history, new RecallSearch.Query(
                RecallSearch.Mode.EMBEDDING, "任何", null, null, 1))).isEmpty();
        // HYBRID：双轨融合非空
        assertThat(withVectors.search(history, new RecallSearch.Query(
                RecallSearch.Mode.HYBRID, "订单 发货", null, null, 2))).isNotEmpty();
    }

    // ---- impl-14：保真 eval ----

    @Test
    void fidelityEvalMeasuresRetentionAndListsMisses() {
        EnumMap<SummarySection, SectionContent> sections = new EnumMap<>(SummarySection.class);
        for (SummarySection section : SummarySection.values()) {
            sections.put(section, new SectionContent("（初始）", SectionContent.Form.FULL, List.of()));
        }
        sections.put(SummarySection.KEY_ARTIFACTS,
                new SectionContent("关键产物：流水号 PAY-9；物流单号 SF-999",
                        SectionContent.Form.FULL, List.of()));
        NineSectionSummary summary = new NineSectionSummary(1, 5, sections);

        var result = new CompactionFidelityEval().evaluate(summary, List.of(
                new CompactionFidelityEval.Probe("流水号是多少？", "PAY-9"),
                new CompactionFidelityEval.Probe("物流单号是多少？", "SF-999"),
                new CompactionFidelityEval.Probe("优惠券码是多少？", "COUPON-XYZ")));
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.retained()).isEqualTo(2);
        assertThat(result.fidelityRate()).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.misses()).containsExactly("优惠券码是多少？");
    }

    // ---- impl-08：interrupt/resume 按 toolCallId ----

    @Test
    void resumeInjectsToolResponseByCallIdWithoutReplayingTurn() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "intr-" + UUID.randomUUID();
        // 挂起现场：assistant 发出 tc-9 调用、无应答（如 HITL 挂起/中断）
        stores.messageStore().append(sessionId, List.of(
                msg(sessionId, 3, 0, Role.USER, "删除这些记录"),
                new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 3, 1, Role.ASSISTANT,
                        "", List.of(new io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord(
                        "tc-9", "delete_records", "{}")),
                        null, null, null, Map.of(), Instant.now())));

        // pending 推导：恰好 tc-9 挂起
        var pending = SessionInterrupts.pending(stores.messageStore(), sessionId);
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().toolCallId()).isEqualTo("tc-9");

        // resume：按 toolCallId 精确注入（幂等：重复/未知 id 返回 false）
        assertThat(SessionInterrupts.resumeWith(stores.messageStore(), sessionId,
                "tc-9", "人工已确认，执行完成：删除 3 条")).isTrue();
        assertThat(SessionInterrupts.resumeWith(stores.messageStore(), sessionId,
                "tc-9", "再次注入")).isFalse();
        assertThat(SessionInterrupts.resumeWith(stores.messageStore(), sessionId,
                "tc-unknown", "x")).isFalse();
        assertThat(SessionInterrupts.pending(stores.messageStore(), sessionId)).isEmpty();

        // 下一轮模型直接见到注入的应答（不重放 Turn 前段——模型收到的历史即含答案）
        List<String> prompts = new CopyOnWriteArrayList<>();
        ChatModel model = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                prompts.add(prompt.getInstructions().toString());
                return new ChatResponse(List.of(new Generation(
                        new org.springframework.ai.chat.messages.AssistantMessage("恢复完成"))));
            }
        };
        AgentRuntime runtime = Buzhou.runtime(model, stores);
        AgentSession session = runtime.spawn("intr-app", "agent", sessionId);
        session.chat("继续");
        session.close();
        assertThat(prompts.getFirst()).contains("人工已确认，执行完成：删除 3 条");
        assertThat(prompts.getFirst()).doesNotContain("执行被中断");
    }

    // ---- impl-26：episodic few-shot ----

    @Test
    void episodicLedgerRecordsAndRecallsSimilarGoals() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "epi-" + UUID.randomUUID();
        EpisodeLedger ledger = new EpisodeLedger(stores.sessionStateStore(), BOW);
        ledger.record(sessionId, "排查订单物流延误", "get_order_status→read_range", "success");
        ledger.record(sessionId, "处理退款申请", "get_refund_policy→approve_refund", "success");
        ledger.record(sessionId, "配置数据库备份", "run_backup→verify", "success");

        // 同类目标召回 top-1 = 语义最近的过往经验
        var examples = ledger.recallExamples(sessionId, "查一下物流为什么慢", 1);
        assertThat(examples).hasSize(1);
        assertThat(examples.getFirst().goal()).contains("物流");
        // few-shot 注入块（按预算截断）与无关目标零命中
        assertThat(ledger.fewShotBlock(sessionId, "排查物流延误", 2, 400)).isPresent()
                .hasValueSatisfying(block -> assertThat(block).contains("[过往成功示例"));
        assertThat(ledger.fewShotBlock(sessionId, "完全无关的目标词组", 2, 400)).isEmpty();
        // 无 provider 时为显式 no-op
        assertThat(new EpisodeLedger(stores.sessionStateStore(), null)
                .recallExamples(sessionId, "任何", 3)).isEmpty();
    }

    private static BuzhouMessage toolMsg(String sessionId, int turn, String callId, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 2, Role.TOOL,
                content, List.of(), callId, null, null, Map.of("toolName", "query"), Instant.now());
    }

    private static BuzhouMessage msg(String sessionId, int turn, int seq, Role role, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, seq, role,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }
}
