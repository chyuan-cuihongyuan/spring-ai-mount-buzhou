package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A/B 成对评估红队（spec 71 §B / T294）：A 路全胜（内容优势 + 内容型 judge）胜率 1.0；
 * 单路执行异常 → 该项 error 不裁胜负（分母不计）；项序确定；并行与串行等值同序。
 */
class PairwiseEvalRunnerTest {

    /** 回显模型：回复固定前缀 + 输入尾（内容可辨识——内容型 judge 按标记裁）。 */
    static final class EchoModel extends ScriptedChatModel {
        private final String prefix;

        EchoModel(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            String text = prompt.getInstructions().getLast().getText();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    prefix + text.substring(text.length() - 2)))));
        }
    }

    /** 内容型 judge：含 gold 标记的输出位赢（与位置无关——双向一致可裁）。 */
    static final class GoldContentJudge implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            String user = prompt.getInstructions().get(1).getText();
            int a = user.indexOf("【输出A】") + "【输出A】".length();
            int b = user.indexOf("【输出B】", a);
            String slotA = user.substring(a, b);
            String winner = slotA.contains("gold") ? "WINNER_A" : "WINNER_B";
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    winner + " 内容更完整"))));
        }
    }

    private static PairwiseEvalRunner runner(BuzhouStores stores, ChatModel judge) {
        return new PairwiseEvalRunner(new EvalDatasetStore(stores.sessionStateStore()),
                new PairwiseJudge(judge));
    }

    private static void seedDataset(BuzhouStores stores, int items) {
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("ab", null);
        for (int i = 1; i <= items; i++) {
            ds.addItem("ab", "q" + String.format("%02d", i), "ignored", null, null);
        }
    }

    @Test
    void contentSuperiorSideWinsAcrossDataset() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 4);
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"),
                stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"),
                stores, RuntimeConfig.defaults());

        PairwiseEvalRunner.PairwiseEvalResult result = runner(stores, new GoldContentJudge())
                .compare("ab", runtimeA, runtimeB, 2);

        assertThat(result.summary().winsA()).isEqualTo(4);
        assertThat(result.summary().winRateA()).isEqualTo(1.0);
        assertThat(result.items()).allSatisfy(r -> {
            assertThat(r.error()).isNull();
            assertThat(r.outputA()).startsWith("gold-");
            assertThat(r.outputB()).startsWith("plain-");
        });
    }

    @Test
    void oneSideFailureIsErrorNotVerdict() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 2);
        ScriptedChatModel brokenB = new ScriptedChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new IllegalStateException("B 路挂了");
            }
        };
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"),
                stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(brokenB, stores, RuntimeConfig.defaults());

        PairwiseEvalRunner.PairwiseEvalResult result = runner(stores, new GoldContentJudge())
                .compare("ab", runtimeA, runtimeB, 1);

        assertThat(result.summary().errors()).isEqualTo(2); // 两项皆 B 路异常
        assertThat(result.summary().winsA()).isZero();
        assertThat(result.items()).allSatisfy(r -> assertThat(r.error()).contains("B 路执行异常"));
    }

    @Test
    void parallelMatchesSerialOrderAndValues() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 5);
        PairwiseJudge judge = new PairwiseJudge(new GoldContentJudge());
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        PairwiseEvalRunner r = new PairwiseEvalRunner(
                new EvalDatasetStore(stores.sessionStateStore()), judge);

        List<String> serial = r.compare("ab", runtimeA, runtimeB, 1).items().stream()
                .map(i -> i.itemId() + ":" + verdictOf(i)).toList();
        List<String> parallel = r.compare("ab", runtimeA, runtimeB, 4).items().stream()
                .map(i -> i.itemId() + ":" + verdictOf(i)).toList();
        assertThat(parallel).isEqualTo(serial); // 项序确定 + 裁决等值
    }

    private static String verdictOf(PairwiseEvalRunner.PairwiseItemResult r) {
        return r.error() != null ? "error" : r.verdict().winner().name();
    }

    /** spec 74 §B / T300：run 落盘 + 查询（dataset 过滤 / startedAt 倒序）；不落盘构造零变化。 */
    /** spec 93 §B / T352：A/B run 指纹——入档/回读等值，旧记录不误报。 */
    @Test
    void abRunCarriesDatasetFingerprint() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 2);
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        PairwiseEvalRunner persisting = new PairwiseEvalRunner(ds,
                new PairwiseJudge(new GoldContentJudge()), stores.sessionStateStore());

        PairwiseEvalRunner.PairwiseEvalResult result = persisting.compare("ab", runtimeA, runtimeB, 1);

        // 内存面等值 + 落盘回读等值 + 摘要行携带
        assertThat(result.datasetFingerprint()).isEqualTo(ds.fingerprint("ab").orElseThrow());
        var detail = PairwiseEvalRunner.abRun(stores.sessionStateStore(), result.runId()).orElseThrow();
        assertThat(detail.datasetFingerprint()).isEqualTo(result.datasetFingerprint());
        assertThat(PairwiseEvalRunner.abRuns(stores.sessionStateStore(), "ab")
                .getFirst().datasetFingerprint()).isEqualTo(result.datasetFingerprint());
    }

    @Test
    void abRunsPersistAndQuery() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 3);
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("other", null);
        ds.addItem("other", "x", "y", null, null);
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        PairwiseEvalRunner persisting = new PairwiseEvalRunner(ds, new PairwiseJudge(new GoldContentJudge()),
                stores.sessionStateStore());

        PairwiseEvalRunner.PairwiseEvalResult first = persisting.compare("ab", runtimeA, runtimeB, 1);
        PairwiseEvalRunner.PairwiseEvalResult second = persisting.compare("other", runtimeA, runtimeB, 1);

        java.util.List<PairwiseEvalRunner.AbRunSummary> all =
                PairwiseEvalRunner.abRuns(stores.sessionStateStore(), null);
        assertThat(all).hasSize(2);
        java.util.List<PairwiseEvalRunner.AbRunSummary> filtered =
                PairwiseEvalRunner.abRuns(stores.sessionStateStore(), "ab");
        assertThat(filtered).hasSize(1);
        assertThat(filtered.getFirst().runId()).isEqualTo(first.runId());
        assertThat(filtered.getFirst().summary().winsA()).isEqualTo(3); // 汇总回读等值
        assertThat(filtered.getFirst().summary().winRateA()).isEqualTo(1.0);
        // 倒序：other（后跑）在前
        assertThat(all.getFirst().runId()).isEqualTo(second.runId());
    }

    // ---- spec 75 §B / T304：A/B run 完成事件（eval.run.completed 家族扩展） ----

    /** 事件捕获面：全局监听器队列（收尾会话生命周期内异步派发，队列线程安全）。 */
    private static java.util.Queue<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent>
            captureEvents(io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime runtime) {
        java.util.Queue<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> received =
                new java.util.concurrent.ConcurrentLinkedQueue<>();
        runtime.addGlobalEventListener(received::add);
        return received;
    }

    @Test
    void abRunCompletionEmitsEventWithSummaryPayload() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 3);
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        Queue<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> events =
                captureEvents((io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime) runtimeA);

        PairwiseEvalRunner.PairwiseEvalResult result = runner(stores, new GoldContentJudge())
                .compare("ab", runtimeA, runtimeB, 1);

        io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent event = events.stream()
                .filter(e -> "ab.run.completed".equals(e.type()))
                .findFirst().orElseThrow();
        assertThat(event.payload().get("runId")).isEqualTo(result.runId());
        assertThat(event.payload().get("datasetName")).isEqualTo("ab");
        assertThat(event.payload().get("total")).isEqualTo(3);
        assertThat(event.payload().get("winsA")).isEqualTo(3);
        assertThat(event.payload().get("winsB")).isEqualTo(0);
        assertThat(event.payload().get("winRateA")).isEqualTo(1.0);
        assertThat(event.payload()).containsKey("durationMs");
    }

    @Test
    void emptyAbRunEmitsNoEvalFamilyEvent() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        ds.createDataset("ab-empty", null);
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        Queue<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> events =
                captureEvents((io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime) runtimeA);

        runner(stores, new GoldContentJudge()).compare("ab-empty", runtimeA, runtimeB, 1);

        // 空集 run 不发 eval/ab 事件（语义 = 对比完成，空集无对比发生）
        assertThat(events.stream()
                .noneMatch(e -> e.type().startsWith("eval.") || e.type().startsWith("ab.")))
                .isTrue();
    }

    @Test
    void eventEmitsIndependentlyOfPersistence() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 2);
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        Queue<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> events =
                captureEvents((io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime) runtimeA);

        // 2 参构造（不落盘）同样发事件——事件面与落盘正交
        runner(stores, new GoldContentJudge()).compare("ab", runtimeA, runtimeB, 1);

        assertThat(events.stream().anyMatch(e -> "ab.run.completed".equals(e.type()))).isTrue();
        assertThat(PairwiseEvalRunner.abRuns(stores.sessionStateStore(), null)).isEmpty();
    }

    // ---- spec 76 §B / T306：A/B run 明细回读（verdict 面 + 前缀隔离） ----

    @Test
    void abRunDetailRoundTripsVerdicts() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 2);
        // 第 2 项（输入 q02）B 路必挂——按输入内容判定，制造 1 win + 1 error 的确定混合
        ScriptedChatModel brokenOnQ02 = new ScriptedChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                String text = prompt.getInstructions().getLast().getText();
                if (text.contains("q02")) {
                    throw new IllegalStateException("B 路尾部异常");
                }
                return new EchoModel("plain-").call(prompt);
            }
        };
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(brokenOnQ02, stores, RuntimeConfig.defaults());
        PairwiseEvalRunner persisting = new PairwiseEvalRunner(
                new EvalDatasetStore(stores.sessionStateStore()),
                new PairwiseJudge(new GoldContentJudge()), stores.sessionStateStore());
        PairwiseEvalRunner.PairwiseEvalResult original = persisting.compare("ab", runtimeA, runtimeB, 1);

        var detail = PairwiseEvalRunner.abRun(stores.sessionStateStore(), original.runId());
        assertThat(detail).isPresent();
        PairwiseEvalRunner.PairwiseEvalResult decoded = detail.orElseThrow();
        assertThat(decoded.runId()).isEqualTo(original.runId());
        assertThat(decoded.datasetName()).isEqualTo("ab");
        assertThat(decoded.summary().winsA()).isEqualTo(original.summary().winsA());
        assertThat(decoded.summary().errors()).isEqualTo(original.summary().errors());
        assertThat(decoded.items()).hasSize(2);
        // verdict 面等值：win 项 winner/reason 保留；error 项 error 文本保留
        assertThat(decoded.items().get(0).verdict()).isNotNull();
        assertThat(decoded.items().get(0).verdict().winner())
                .isEqualTo(original.items().get(0).verdict().winner());
        assertThat(decoded.items().get(0).verdict().reason())
                .isEqualTo(original.items().get(0).verdict().reason());
        assertThat(decoded.items().get(1).error()).contains("B 路执行异常");
        // 输出原文不落盘（spec 74 决策）——回读为 null
        assertThat(decoded.items()).allSatisfy(i -> {
            assertThat(i.outputA()).isNull();
            assertThat(i.outputB()).isNull();
        });
    }

    @Test
    void unknownRunIdYieldsEmptyAndPrefixesStayIsolated() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedDataset(stores, 1);
        AgentRuntime runtimeA = Buzhou.runtime(new EchoModel("gold-"), stores, RuntimeConfig.defaults());
        AgentRuntime runtimeB = Buzhou.runtime(new EchoModel("plain-"), stores, RuntimeConfig.defaults());
        // 落一条 eval run（eval. 前缀）+ 一条 ab run（ab. 前缀）
        EvalDatasetStore ds = new EvalDatasetStore(stores.sessionStateStore());
        EvalRunner evalRunner = new EvalRunner(runtimeA, ds, stores.sessionStateStore());
        EvalRunResult evalResult = evalRunner.run("ab", BuiltInEvaluators.EXACT);
        PairwiseEvalRunner persisting = new PairwiseEvalRunner(ds,
                new PairwiseJudge(new GoldContentJudge()), stores.sessionStateStore());
        PairwiseEvalRunner.PairwiseEvalResult abResult = persisting.compare("ab", runtimeA, runtimeB, 1);

        assertThat(PairwiseEvalRunner.abRun(stores.sessionStateStore(), "no-such-run")).isEmpty();
        // eval runId 不是 ab run：前缀隔离不误命中
        assertThat(PairwiseEvalRunner.abRun(stores.sessionStateStore(), evalResult.runId())).isEmpty();
        // ab runId 也不是 eval run
        assertThat(new EvalQueryService(stores.sessionStateStore()).run(abResult.runId())).isEmpty();
    }
}
