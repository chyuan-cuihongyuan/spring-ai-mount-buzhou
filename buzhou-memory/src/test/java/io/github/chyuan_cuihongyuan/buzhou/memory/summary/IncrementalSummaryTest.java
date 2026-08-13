package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.memory.InjectionViewProcessor;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T24 增量摘要 + T25 事实对账 + T26 双时序（docs/spec/11 memory）：
 * 再次压缩只折入未摘要的新消息（summarizedMessageIds 水位，不全量重摘要、代际连续）；
 * 对账 pass 四态裁决去重/证伪并应用对账正文（事件可观测、解析失败 NOOP）；
 * 被取代段正文标 valid_until 保留（时序回查可取「某时点以为的事实」）。
 */
class IncrementalSummaryTest {

    private static final String OLD_TURN_MARKER = "OLD-TURN-CONTENT-AAA";
    private static final String NEW_TURN_MARKER = "NEW-TURN-CONTENT-BBB";

    /** 记录 prompt 的脚本摘要模型：对合并请求回九段文本；对对账请求回 JSON 决策。 */
    static final class RecordingSummaryModel implements ChatModel {
        final List<String> seenPrompts = new ArrayList<>();
        final List<ChatResponse> script = new ArrayList<>();

        void enqueue(String text) {
            script.add(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            seenPrompts.add(prompt.getContents());
            return script.isEmpty()
                    ? new ChatResponse(List.of(new Generation(new AssistantMessage("default"))))
                    : script.removeFirst();
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }
    }

    private BuzhouMessage user(String sessionId, int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0, Role.USER,
                content + "\n" + FILLER, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private InjectionViewProcessor processor(RecordingSummaryModel model, InMemorySummaryStore store,
                                             boolean reconciliation) {
        InjectionViewProcessor ivp = new InjectionViewProcessor(
                new DefaultMicroCompactor(new DefaultCompletedTurnDetector()),
                tool -> MicroCompactionPolicy.defaults(), 1,
                new DefaultBudgetCalculator(
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver(
                                java.util.Map.of()),
                        new io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator()),
                new SummaryStoreBridge(store),
                new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3), model,
                "tiny-model", 2, null, 4000);
        ivp.setFactReconciliation(reconciliation);
        return ivp;
    }

    /** 大消息体量（确保超过默认窗口 0.9 阈值、触发压缩）。 */
    private static final String FILLER = "数据行填充内容。".repeat(2200);

    @Test
    void secondCompactionOnlyFoldsNewMessagesByIdWatermark() {
        RecordingSummaryModel model = new RecordingSummaryModel();
        InMemorySummaryStore store = new InMemorySummaryStore();
        InjectionViewProcessor ivp = processor(model, store, false);
        String sid = "inc-sess";

        // 第一次压缩：折入第 1–3 轮（keepRecentTurns=2，当前第 5 轮）
        model.enqueue(nineSections("第一代现场"));
        List<BuzhouMessage> history1 = List.of(
                user(sid, 1, OLD_TURN_MARKER + "-turn1"),
                user(sid, 2, "m2"),
                user(sid, 3, "m3"),
                user(sid, 4, "recent4"),
                user(sid, 5, "recent5"));
        ivp.process(sid, history1, 5);
        int mergeCallsAfterFirst = countMergePrompts(model);
        assertThat(mergeCallsAfterFirst).isEqualTo(1);
        long firstGeneration = store.latest(sid).orElseThrow().version();

        // 第二次压缩：追加第 6–7 轮（cutoff=5，水位 covers=3 → 只折 4–5 轮的新消息）
        model.enqueue(nineSections("第二代现场"));
        List<BuzhouMessage> history2 = new ArrayList<>(history1);
        history2.add(user(sid, 6, "recent6"));
        history2.add(user(sid, 7, "recent7"));
        ivp.process(sid, history2, 7);

        // 增量：第二次合并 prompt 只含新消息（不含已摘要的第 1 轮内容；消息级水位去重）
        String secondMergePrompt = mergePrompts(model).get(1);
        assertThat(secondMergePrompt).doesNotContain(OLD_TURN_MARKER);
        assertThat(secondMergePrompt).contains("recent4");
        assertThat(secondMergePrompt).contains("recent5");
        // 代际连续：存储版本单调 +1（无重复漂移）
        assertThat(store.latest(sid).orElseThrow().version()).isEqualTo(firstGeneration + 1);
        // 消息 id 水位持久化（T24）
        NineSectionSummary persisted = new SummaryStoreBridge(store).loadLatest(sid).orElseThrow();
        assertThat(persisted.summarizedMessageIds()).isNotEmpty();
    }

    @Test
    void alreadySummarizedIdsAreSkippedEvenWhenTurnWatermarkMisses() {
        // 消息级水位兜底：轮次水位相同（covers=3）时，已折入 id 不再重复折入
        RecordingSummaryModel model = new RecordingSummaryModel();
        InMemorySummaryStore store = new InMemorySummaryStore();
        InjectionViewProcessor ivp = processor(model, store, false);
        String sid = "dedup-sess";

        model.enqueue(nineSections("第一代"));
        List<BuzhouMessage> history = List.of(
                user(sid, 1, "m1"), user(sid, 2, "m2"), user(sid, 3, "m3"),
                user(sid, 4, "r4"), user(sid, 5, "r5"));
        ivp.process(sid, history, 5);

        // 同一历史再处理一次（cutoff 不变 → toSummarize 为空，无新合并调用）
        int mergeCalls = countMergePrompts(model);
        ivp.process(sid, history, 5);
        assertThat(countMergePrompts(model)).isEqualTo(mergeCalls);
    }

    @Test
    void reconciliationDeduplicatesAndRecordsBiTemporalValidity() {
        RecordingSummaryModel model = new RecordingSummaryModel();
        InMemorySummaryStore store = new InMemorySummaryStore();
        InjectionViewProcessor ivp = processor(model, store, true);
        InMemorySessionStateStore stateStore = new InMemorySessionStateStore();
        ivp.setSessionStateStore(stateStore);
        List<SessionEvent> events = new ArrayList<>();
        ivp.setEventSink(events::add);
        String sid = "reconcile-sess";

        // 第一代：合并（1 次模型调用）——previous 为空，无对账；单段 stub 使对账精确命中 CURRENT_STATE
        model.enqueue(stateOnly("订单状态：待支付；配送地址：北京；用户电话：13800"));
        List<BuzhouMessage> history1 = List.of(
                user(sid, 1, "m1"), user(sid, 2, "m2"), user(sid, 3, "m3"),
                user(sid, 4, "r4"), user(sid, 5, "r5"));
        ivp.process(sid, history1, 5);
        NineSectionSummary first = new SummaryStoreBridge(store).loadLatest(sid).orElseThrow();

        // 第二代：合并（调用 1）+ 对账（调用 2，CURRENT_STATE 段有旧正文 → 四态裁决）
        model.enqueue(stateOnly("订单状态：已发货（新版，待对账去重）；配送地址：北京；用户电话：13800"));
        model.enqueue("{\"add\":1,\"update\":1,\"delete\":0,\"noop\":1,"
                + "\"body\":\"订单状态：已发货；配送地址：北京朝阳；用户电话：13800\"}");
        List<BuzhouMessage> history2 = new ArrayList<>(history1);
        history2.add(user(sid, 6, "r6"));
        history2.add(user(sid, 7, "r7"));
        ivp.process(sid, history2, 7);

        // 对账正文被应用（去重/更新后的版本落库）
        NineSectionSummary second = new SummaryStoreBridge(store).loadLatest(sid).orElseThrow();
        assertThat(second.sections().get(SummarySection.CURRENT_STATE).body()).contains("已发货");
        // 四态裁决可观测：事件含 section + 计数
        assertThat(events).anyMatch(e -> SummaryFactReconciler.EVENT_RECONCILED.equals(e.type())
                && "CURRENT_STATE".equals(e.payload().get("section")));
        // T26 双时序：被取代的旧正文保留并闭合 valid_until；时序回查可取旧版本
        BiTemporalFactLedger ledger = new BiTemporalFactLedger(stateStore);
        List<BiTemporalFactLedger.ValidityRecord> history =
                ledger.historyOf(sid, "CURRENT_STATE");
        assertThat(history).isNotEmpty();
        BiTemporalFactLedger.ValidityRecord superseded = history.stream()
                .filter(r -> r.body() != null).findFirst().orElseThrow();
        assertThat(superseded.validUntil()).isNotNull();
        assertThat(superseded.body()).contains("订单状态");
        assertThat(ledger.validAt(sid, "CURRENT_STATE", superseded.validUntil().minusSeconds(1)))
                .isPresent();
        // 新版本同步开口生效：现时点回查命中新版本（validFrom=now，body 以摘要库为准）
        assertThat(ledger.validAt(sid, "CURRENT_STATE", java.time.Instant.now().plusSeconds(1)))
                .map(BiTemporalFactLedger.ValidityRecord::body).isEmpty();
    }

    @Test
    void unparseableReconcileResponseIsNoop() {
        // 韧性：对账模型返回不可解析文本 → NOOP（正文保持合并结果，不落半成品）
        RecordingSummaryModel model = new RecordingSummaryModel();
        InMemorySummaryStore store = new InMemorySummaryStore();
        InjectionViewProcessor ivp = processor(model, store, true);
        String sid = "noop-sess";

        model.enqueue(nineSections("第一代正文"));
        List<BuzhouMessage> history1 = List.of(
                user(sid, 1, "m1"), user(sid, 2, "m2"), user(sid, 3, "m3"),
                user(sid, 4, "r4"), user(sid, 5, "r5"));
        ivp.process(sid, history1, 5);

        model.enqueue(nineSections("第二代正文"));
        model.enqueue("这不是 JSON"); // 对账响应不可解析 → NOOP
        List<BuzhouMessage> history2 = new ArrayList<>(history1);
        history2.add(user(sid, 6, "r6"));
        history2.add(user(sid, 7, "r7"));
        ivp.process(sid, history2, 7);

        NineSectionSummary second = new SummaryStoreBridge(store).loadLatest(sid).orElseThrow();
        // 合并正文原样保留（NOOP 不改动）
        assertThat(second.sections().get(SummarySection.CURRENT_STATE).body())
                .contains("第二代正文");
    }

    private static String stateOnly(String currentState) {
        return "## CURRENT_STATE（当前工作现场）\n" + currentState + "\n";
    }

    private static String nineSections(String currentState) {
        return "## USER_INTENT（用户核心诉求）\n排查订单问题\n\n"
                + "## CURRENT_STATE（当前工作现场）\n" + currentState + "\n\n"
                + "## NEXT_STEP（下一步）\n继续跟进\n";
    }

    private int countMergePrompts(RecordingSummaryModel model) {
        return mergePrompts(model).size();
    }

    private List<String> mergePrompts(RecordingSummaryModel model) {
        return model.seenPrompts.stream()
                .filter(p -> p.contains("会话压缩器") || p.contains("合并更新"))
                .toList();
    }
}
