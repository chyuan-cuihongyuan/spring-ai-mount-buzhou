package io.github.chyuan_cuihongyuan.buzhou.examples.golden;

import com.sun.net.httpserver.HttpServer;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.CharHeuristicTokenEstimator;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.token.TableContextWindowResolver;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.BuzhouWebhookProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder;
import io.github.chyuan_cuihongyuan.buzhou.memory.InjectionViewProcessor;
import io.github.chyuan_cuihongyuan.buzhou.memory.budget.DefaultBudgetCalculator;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.CompactionCheckpoints;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryCircuitBreaker;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import io.github.chyuan_cuihongyuan.buzhou.spill.DiskSpillStore;
import io.github.chyuan_cuihongyuan.buzhou.spill.RangeReadRequest;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillEntry;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillUri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 黄金轨迹扩充 A（spec 34 §B / T116 / impl-91）：effort #6 新能力的确定性轨迹——
 * evidence 引用生命周期（保留→释放物理删）、outbox 跨重启补投递、压缩事件观测。
 * 轨迹断言为行为步骤序列（非 SessionEvent 通道——这些能力面在 store/观测层）。
 */
class GoldenTrajectoryEffort6Test {

    // ---- G7 evidence 引用计数生命周期 ----

    /** fork 引用 → 源删除保留（fork 可读）→ fork 关闭（最后引用者）→ 物理删。 */
    @Test
    void g7EvidenceRefcountLifecycle(@TempDir Path spillRoot) {
        DiskSpillStore store = new DiskSpillStore(spillRoot);
        SpillUri uri = new SpillUri("agent-g7", "src-session", "t1");
        store.store(SpillEntry.of(uri, "golden-evidence"), 100);

        // fork 登记 → 源删除：证据保留，分支可回读
        assertThat(store.acquireSessionReferences("src-session", "fork-session")).isEqualTo(1);
        assertThat(store.deleteBySession("agent-g7", "src-session")).isZero();
        assertThat(store.readRange(uri, RangeReadRequest.bytes(0, 20)).content())
                .contains("golden-evidence");

        // fork 关闭（最后引用者关闭）→ 延迟物理删
        assertThat(store.deleteBySession("agent-g7", "fork-session")).isEqualTo(1);
        assertThat(store.exists(uri)).isFalse();
        assertThat(store.readRange(uri, RangeReadRequest.bytes(0, 20)).content())
                .contains("EVIDENCE_GONE");
    }

    // ---- G8 outbox 跨重启补投递 ----

    /** 第一代恒 500（事件滞留）→ 关闭 → 第二代（共享 store）补投递成功；零死信。 */
    @Test
    void g8OutboxSurvivesRestart() throws Exception {
        ConcurrentLinkedQueue<String> received = new ConcurrentLinkedQueue<>();
        AtomicInteger hits = new AtomicInteger();
        AtomicInteger[] status = {new AtomicInteger(500)};
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hook", exchange -> {
            hits.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            if (status[0].get() == 200) {
                received.add("ok");
            }
            exchange.sendResponseHeaders(status[0].get(), -1);
            exchange.close();
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";

        InMemorySessionStateStore shared = new InMemorySessionStateStore();
        WebhookEventForwarder first = new WebhookEventForwarder(
                new BuzhouWebhookProperties(url, null, Duration.ofSeconds(2), 8, 100, null), shared);
        first.onEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                "g8.survive", Map.of()));
        await(() -> hits.get() >= 1); // 首试失败（500）
        first.close(); // 未到期退避记录滞留 store

        status[0].set(200); // 下游恢复
        WebhookEventForwarder second = new WebhookEventForwarder(
                new BuzhouWebhookProperties(url, null, Duration.ofSeconds(2), 8, 100, null), shared);
        await(() -> second.delivered() == 1); // 第二代补投递
        await(() -> second.pendingCount() == 0);
        assertThat(second.deadLetters()).isEmpty(); // 零死信
        second.close();
        server.stop(0);
    }

    // ---- G9 压缩事件观测轨迹 ----

    /** 大历史触发折叠 → memory.compacted 事件入观测库（计数与回收为正）。 */
    @Test
    void g9CompactionEmitsObservabilityEvent() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "g9-" + UUID.randomUUID();
        InjectionViewProcessor ivp = ivp(stores);

        List<BuzhouMessage> folded = ivp.process(sessionId, bigHistory(sessionId, 30), 31);

        assertThat(folded).isNotNull();
        List<EventRecord> events = stores.observabilityStore().eventsOfSession(sessionId);
        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("memory.compacted");
            assertThat(((Number) e.payload().get("compactedCount")).intValue()).isPositive();
            assertThat(((Number) e.payload().get("reclaimedChars")).intValue()).isPositive();
        });
    }

    // ---- helpers ----

    private static InjectionViewProcessor ivp(BuzhouStores stores) {
        InjectionViewProcessor ivp = new InjectionViewProcessor(
                new DefaultMicroCompactor(new DefaultCompletedTurnDetector()),
                name -> MicroCompactionPolicy.defaults(), 1,
                new DefaultBudgetCalculator(
                        new TableContextWindowResolver(Map.of("tiny", 13000)),
                        new CharHeuristicTokenEstimator()),
                new SummaryStoreBridge(stores.summaryStore()),
                new DefaultSummaryGenerator(), new SummaryCircuitBreaker(3), summaryModel(),
                "tiny", 2, null, 4000);
        ivp.setSessionStateStore(stores.sessionStateStore());
        ivp.setCheckpoints(new CompactionCheckpoints(stores.sessionStateStore()));
        ivp.setCompactionListener((sid, result) -> stores.observabilityStore().saveEvents(
                List.of(new EventRecord(UUID.randomUUID().toString(), null, sid,
                        "memory.compacted", Instant.now(),
                        Map.of("compactedCount", result.compactedMessageIds().size(),
                                "reclaimedChars", result.reclaimedChars())))));
        return ivp;
    }

    private static ChatModel summaryModel() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(
                        new AssistantMessage("## CURRENT_STATE\ngolden"))));
            }
        };
    }

    private static List<BuzhouMessage> bigHistory(String sessionId, int turns) {
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

    private static void await(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("等待超时");
            }
            Thread.sleep(20);
        }
    }
}
