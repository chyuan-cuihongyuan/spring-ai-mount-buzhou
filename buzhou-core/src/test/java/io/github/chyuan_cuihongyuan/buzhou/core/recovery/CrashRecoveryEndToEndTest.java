package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SpawnOptions;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 崩溃中轮次恢复 e2e 骨架（ticket 01）：故障注入 ChatModel/工具 + 双实例租约交接。
 * 全程断言「最终回复 + 事件流 + 工具调用计数」；计时用 CountDownLatch / 可控故障注入。
 */
class CrashRecoveryEndToEndTest {

    /** 心跳续约证明的有界等待上限。 */
    private static final Duration LEASE_RENEW_WAIT = Duration.ofSeconds(5);
    /** 租约续约轮询间隔。 */
    private static final long LEASE_POLL_MILLIS = 20L;

    private final List<SessionEvent> events = new CopyOnWriteArrayList<>();

    private static RecoveryConfig recovery(Duration ttl, Duration heartbeat) {
        return new RecoveryConfig(true, ttl, heartbeat, DurabilityTier.ASYNC,
                ResumeStrategy.VOID, RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP, true);
    }

    /** 脚本化「助手发起一次工具调用」。 */
    private static AssistantMessage toolCall(String id, String name) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, "{}")))
                .build();
    }

    /** 阻塞型副作用工具：started 后开始等待 release，count 记录真实执行次数。 */
    private static final class BlockingTool implements ToolCallback {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger calls = new AtomicInteger();
        private final String name;
        private final String result;

        BlockingTool(String name, String result) {
            this.name = name;
            this.result = result;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name(name).description(name).inputSchema("{}").build();
        }

        @Override
        public String call(String toolInput) {
            calls.incrementAndGet();
            started.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return result;
        }
    }

    @Test
    void heartbeatKeepsLeaseAliveDuringLongTurn() throws Exception {
        Duration ttl = Duration.ofMillis(200);
        ScriptedChatModel model = new ScriptedChatModel();
        BlockingTool tool = new BlockingTool("slow_op", "done");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime1 = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                recovery(ttl, Duration.ofMillis(50)), tool);

        AgentSession session = runtime1.spawn("app", "agent", "sess-hb");
        java.time.Instant initialExpiry = stores.sessionLeaseStore().inspect("sess-hb")
                .orElseThrow().expiresAt();
        model.enqueue(toolCall("tc-1", "slow_op"));
        model.enqueueText("长轮次完成");
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<String> chat = vt.submit(() -> session.chat("跑长任务"));

        assertThat(tool.started.await(2, TimeUnit.SECONDS)).isTrue();
        // 工具仍在执行：有界轮询等待心跳把租约到期时间推过初始到期点（证明续约发生，不打 wall-clock 死等）
        assertThat(awaitLeaseExtended(stores, "sess-hb", initialExpiry)).isAfter(initialExpiry);
        // 租约已被心跳续约：第二实例不得接管
        AgentRuntime runtime2 = Buzhou.runtime(new ScriptedChatModel(), stores, RuntimeConfig.defaults(),
                recovery(ttl, Duration.ofMillis(50)), tool);
        assertThatThrownBy(() -> runtime2.spawn("app", "agent", "sess-hb"))
                .isInstanceOf(SessionAlreadyActiveException.class);

        tool.release.countDown();
        assertThat(chat.get(5, TimeUnit.SECONDS)).isEqualTo("长轮次完成");
        session.close();
        // 会话谢幕后租约释放，第二实例可接管
        AgentSession resumed = runtime2.spawn("app", "agent", "sess-hb");
        resumed.close();
        vt.shutdownNow();
    }

    @Test
    void voidRecoveryWaitsForUserInputAfterLeaseHandoff() throws Exception {
        Duration ttl = Duration.ofSeconds(5);
        ScriptedChatModel model1 = new ScriptedChatModel();
        BlockingTool tool = new BlockingTool("charge", "charged");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime1 = Buzhou.runtime(model1, stores, RuntimeConfig.defaults(),
                recovery(ttl, Duration.ofMillis(500)), tool);

        // 第一实例：模型发起工具调用后「崩溃」——工具在途、结果未落库，会话不谢幕
        AgentSession crashed = runtime1.spawn("app", "agent", "sess-void");
        model1.enqueue(toolCall("tc-1", "charge"));
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<String> abandoned = vt.submit(() -> crashed.chat("扣款"));
        assertThat(tool.started.await(2, TimeUnit.SECONDS)).isTrue();

        // 第二实例经 steal 交接同会话：加载历史发现被中断轮次 → VOID 默认不续跑、事件留痕
        ScriptedChatModel model2 = new ScriptedChatModel();
        AgentRuntime runtime2 = Buzhou.runtime(model2, stores, RuntimeConfig.defaults(),
                recovery(ttl, Duration.ofMillis(500)), tool);
        AgentSession resumed = runtime2.spawn("app", "agent", "sess-void",
                new SpawnOptions(true, List.of(events::add)));

        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("turn-recovered");
            assertThat(e.payload()).containsEntry("action", "voided");
        });
        // 未 opt-in 自动重驱动：spawn 不得自发调用模型
        assertThat(model2.seenPrompts).isEmpty();

        // 用户下一次输入驱动：悬空调用经修复（非幂等工具、助手消息无文本 → 整条丢弃）后续跑
        model2.enqueueText("已为你继续处理");
        String reply = resumed.chat("继续");
        assertThat(reply).isEqualTo("已为你继续处理");
        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("dangling.repaired");
            assertThat(e.payload()).containsEntry("action", "dropped");
        });

        // 崩溃实例的在途工具只真实执行过一次（本票不保证恰好一次——去重归 ticket 03/04）
        tool.release.countDown();
        abandoned.get(5, TimeUnit.SECONDS);
        resumed.close();
        vt.shutdownNow();
    }

    @Test
    void dedupOnRecoverySkipsReExecutionOfSideEffectTool() {
        // ticket 04 e2e：崩溃发生在「工具已执行、结果未落库」窗口——去重记录 reserve-then-fill
        // 已捕获结果，但工具响应消息未及落库（悬空）。新实例经租约交接恢复后，
        // 该副作用工具不重执行（at-least-once 调用 + 去重 = 效果恰好一次）。
        String sid = "sess-dedup-rec";
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 崩溃前现场（等价于实例一已执行工具一次）：历史只到「助手发起工具调用」，
        // 去重记录已回填首次结果（reserve-then-fill 在消息 append 之前完成）
        stores.messageStore().append(sid, List.of(
                new io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage(
                        java.util.UUID.randomUUID().toString(), sid, 1, 1,
                        io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER, "扣款",
                        List.of(), null, null, null, java.util.Map.of(), java.time.Instant.now()),
                new io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage(
                        java.util.UUID.randomUUID().toString(), sid, 1, 2,
                        io.github.chyuan_cuihongyuan.buzhou.core.message.Role.ASSISTANT, "处理中",
                        List.of(new io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord(
                                "tc-1", "charge", "{}")),
                        null, null, null, java.util.Map.of(), java.time.Instant.now())));
        new io.github.chyuan_cuihongyuan.buzhou.core.internal.recovery.DedupRecorder(
                stores.sessionStateStore())
                .fill(sid, IdempotencyKeys.defaultKey("charge", "tc-1"), "charged-100");

        // 新实例：同会话租约交接恢复；计数工具证明副作用工具不重执行
        AtomicInteger chargeCalls = new AtomicInteger();
        ScriptedChatModel model = new ScriptedChatModel();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                new RecoveryConfig(true, Duration.ofSeconds(5), Duration.ofMillis(500),
                        DurabilityTier.ASYNC, ResumeStrategy.VOID,
                        RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP, true),
                countingTool("charge", chargeCalls, "charged-again"));
        AgentSession resumed = runtime.spawn("app", "agent", sid,
                new SpawnOptions(true, List.of(events::add)));

        model.enqueueText("已继续");
        String reply = resumed.chat("继续");
        assertThat(reply).isEqualTo("已继续");
        // 恰好一次：恢复重放命中去重记录 → 合成首次结果、不重执行
        assertThat(chargeCalls).hasValue(0);
        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("dedup-hit");
            assertThat(e.payload()).containsEntry("toolName", "charge");
        });
        resumed.close();
    }

    @Test
    void autoResumeRedrivesInterruptedTurnWithoutUserInput() {
        // ticket 05：opt-in AUTO_RESUME——加载+修复后历史结尾为被中断轮次 → 无需用户输入自动续跑
        String sid = "sess-autoresume";
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedInterruptedHistory(sid, stores);

        ScriptedChatModel model = new ScriptedChatModel();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                new RecoveryConfig(true, Duration.ofSeconds(5), Duration.ofMillis(500),
                        DurabilityTier.ASYNC, ResumeStrategy.AUTO_RESUME,
                        RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP, true),
                countingTool("charge", new AtomicInteger(), "charged"));
        model.enqueueText("续跑完成");
        AgentSession resumed = runtime.spawn("app", "agent", sid,
                new SpawnOptions(true, List.of(events::add)));

        // 无需用户输入：spawn 即自发续跑一轮（模型被调用一次），记 auto-resumed 事件
        assertThat(model.seenPrompts).hasSize(1);
        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("turn-recovered");
            assertThat(e.payload()).containsEntry("action", "auto-resumed");
        });
        resumed.close();
    }

    @Test
    void completedTurnIsNotAutoResumed() {
        // 完结轮次（历史结尾已有终结性助手回复）不触发续跑
        String sid = "sess-complete";
        BuzhouStores stores = Buzhou.inMemoryStores();
        stores.messageStore().append(sid, List.of(
                buzhouMessage(sid, 1, 1, io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                        "你好", List.of(), null),
                buzhouMessage(sid, 1, 2, io.github.chyuan_cuihongyuan.buzhou.core.message.Role.ASSISTANT,
                        "你好，有什么可以帮你？", List.of(), null)));

        ScriptedChatModel model = new ScriptedChatModel();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                new RecoveryConfig(true, Duration.ofSeconds(5), Duration.ofMillis(500),
                        DurabilityTier.ASYNC, ResumeStrategy.AUTO_RESUME,
                        RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP, true));
        AgentSession resumed = runtime.spawn("app", "agent", sid,
                new SpawnOptions(true, List.of(events::add)));

        assertThat(model.seenPrompts).isEmpty();
        assertThat(events).noneSatisfy(e -> assertThat(e.type()).isEqualTo("turn-recovered"));
        resumed.close();
    }

    @Test
    void crashloopHardCapStopsRepeatedAutoResume() {
        // 崩溃循环兜底：续跑计数已达硬顶 → 掐断、记 resume-skipped-crashloop、不自发调用模型
        String sid = "sess-crashloop";
        BuzhouStores stores = Buzhou.inMemoryStores();
        seedInterruptedHistory(sid, stores);
        stores.sessionStateStore().put(sid, new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                "recovery.autoresume.attempts",
                String.valueOf(RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP), "recovery", 0, null,
                java.time.Instant.now()));

        ScriptedChatModel model = new ScriptedChatModel();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                new RecoveryConfig(true, Duration.ofSeconds(5), Duration.ofMillis(500),
                        DurabilityTier.ASYNC, ResumeStrategy.AUTO_RESUME,
                        RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP, true));
        AgentSession resumed = runtime.spawn("app", "agent", sid,
                new SpawnOptions(true, List.of(events::add)));

        assertThat(model.seenPrompts).isEmpty();
        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("resume-skipped-crashloop");
            assertThat(e.payload()).containsEntry("hardCap", RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP);
        });
        resumed.close();
    }

    @Test
    void exitTierFlushesBufferedWritesOnSessionClose() {
        // ticket 02：EXIT 档仅入缓冲、会话谢幕时 flush；durability-tier 事件记录生效档位
        String sid = "sess-exit-tier";
        BuzhouStores stores = Buzhou.inMemoryStores();
        ScriptedChatModel model = new ScriptedChatModel();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                new RecoveryConfig(true, Duration.ofSeconds(5), Duration.ofMillis(500),
                        DurabilityTier.EXIT, ResumeStrategy.VOID,
                        RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP, true));
        AgentSession session = runtime.spawn("app", "agent", sid,
                new SpawnOptions(false, List.of(events::add)));

        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("durability-tier");
            assertThat(e.payload()).containsEntry("tier", "EXIT");
        });

        model.enqueueText("完成");
        session.chat("你好");
        // EXIT 档：轮次完结但底层尚不可见（崩溃丢整轮，由恢复语义兜底）
        assertThat(stores.messageStore().load(sid)).isEmpty();
        // 会话谢幕触发 flush：缓冲写入批量落底层
        session.close();
        assertThat(stores.messageStore().load(sid)).isNotEmpty();
    }

    /** 有界轮询等待心跳续约（租约到期时间被推过初始到期点即证明续约发生）。 */
    private static java.time.Instant awaitLeaseExtended(BuzhouStores stores, String sid,
                                                        java.time.Instant initialExpiry)
            throws InterruptedException {
        long deadlineNanos = System.nanoTime() + LEASE_RENEW_WAIT.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            var info = stores.sessionLeaseStore().inspect(sid);
            if (info.isPresent() && info.get().expiresAt().isAfter(initialExpiry)) {
                return info.get().expiresAt();
            }
            Thread.sleep(LEASE_POLL_MILLIS);
        }
        throw new AssertionError("心跳未在 " + LEASE_RENEW_WAIT + " 内续约租约（sessionId=" + sid + "）");
    }

    private static void seedInterruptedHistory(String sid, BuzhouStores stores) {        stores.messageStore().append(sid, List.of(
                buzhouMessage(sid, 1, 1, io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                        "扣款", List.of(), null),
                buzhouMessage(sid, 1, 2, io.github.chyuan_cuihongyuan.buzhou.core.message.Role.ASSISTANT,
                        "处理中",
                        List.of(new io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord(
                                "tc-1", "charge", "{}")), null)));
    }

    private static io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage buzhouMessage(
            String sid, int turnSeq, int seqInTurn,
            io.github.chyuan_cuihongyuan.buzhou.core.message.Role role, String content,
            List<io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord> toolCalls,
            String toolCallId) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage(
                java.util.UUID.randomUUID().toString(), sid, turnSeq, seqInTurn, role, content,
                toolCalls, toolCallId, null, null, java.util.Map.of(), java.time.Instant.now());
    }

    private static ToolCallback countingTool(String name, AtomicInteger calls, String result) {
        return new ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return org.springframework.ai.tool.definition.ToolDefinition.builder()
                        .name(name).description(name).inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                return result;
            }
        };
    }
}
