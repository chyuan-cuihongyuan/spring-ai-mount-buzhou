package io.github.chyuan_cuihongyuan.buzhou.core.shutdown;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.DurabilityTier;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RecoveryConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ResumeStrategy;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.DrainResult;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeDrainingException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionResourceCustomizer;
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
 * 优雅停机（drain）e2e 骨架（ticket 01）：复用 CrashRecoveryEndToEndTest 装配形态
 * （{@code Buzhou.runtime(...)} + {@link ScriptedChatModel} + latch 阻塞工具 + 事件捕获）。
 * 全程断言「drain 结果 + 事件流 + 租约/可接管性 + 幂等」；计时用 CountDownLatch，无 wall-clock sleep。
 * 后续票据（02/03）在此骨架上增量补入「等完在途轮次 / 超时强杀」用例。
 */
class GracefulShutdownEndToEndTest {

    private final List<SessionEvent> events = new CopyOnWriteArrayList<>();

    /** 阻塞型副作用工具：started 后开始等待 release，count 记录真实执行次数，interrupted 捕获取消传播。 */
    static final class BlockingTool implements ToolCallback {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
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
                interrupted.countDown();
            }
            return result;
        }
    }

    /** 脚本化「助手发起一次工具调用」。 */
    private static AssistantMessage toolCall(String id, String name) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, "{}")))
                .build();
    }

    @Test
    void drainRefusesNewSpawnAfterDrainStarts() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);

        // drain 前可正常 spawn
        AgentSession session = runtime.spawn("app", "agent", "sess-refuse",
                new SpawnOptions(false, List.of(events::add)));
        session.close();

        runtime.drain(Duration.ofSeconds(2));

        // drain 后 spawn 抛拒新异常，message 带 sessionId 与 drain 上下文
        assertThatThrownBy(() -> runtime.spawn("app", "agent", "sess-after-drain"))
                .isInstanceOf(RuntimeDrainingException.class)
                .hasMessageContaining("sess-after-drain")
                .hasMessageContaining("draining");
    }

    @Test
    void idleSessionsDrainAndReleaseLeaseForImmediateRespawnOnOtherRuntime() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime1 = Buzhou.runtime(model, stores);

        AgentSession session = runtime1.spawn("app", "agent", "sess-idle",
                new SpawnOptions(false, List.of(events::add)));
        // 空闲会话：未发起任何轮次

        DrainResult result = runtime1.drain(Duration.ofSeconds(2));

        // drain 结果：1 等完、0 强杀
        assertThat(result.drainedCount()).isEqualTo(1);
        assertThat(result.forceKilledCount()).isEqualTo(0);
        // 会话已 close：后续 chat 抛 IllegalStateException
        assertThatThrownBy(() -> session.chat("x")).isInstanceOf(IllegalStateException.class);
        // 租约已释放：另一 runtime 同 sessionId 可立即 spawn 续接（双 runtime 共享 stores）
        AgentRuntime runtime2 = Buzhou.runtime(new ScriptedChatModel(), stores);
        AgentSession resumed = runtime2.spawn("app", "agent", "sess-idle");
        resumed.close();
    }

    @Test
    void drainStartedAndFinishedEventsAppearInOrderWithCorrectCounts() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);

        runtime.spawn("app", "agent", "sess-evt-1",
                new SpawnOptions(false, List.of(events::add)));

        runtime.drain(Duration.ofSeconds(2));

        // drain.started 先于 drain.finished，计数正确（活跃会话数=1，等完=1，强杀=0）
        int startedIdx = indexOfEventType("drain.started");
        int finishedIdx = indexOfEventType("drain.finished");
        assertThat(startedIdx).isLessThan(finishedIdx);
        SessionEvent started = events.get(startedIdx);
        assertThat(started.payload()).containsEntry("activeCount", 1);
        SessionEvent finished = events.get(finishedIdx);
        assertThat(finished.payload())
                .containsEntry("drainedCount", 1)
                .containsEntry("forceKilledCount", 0);
    }

    @Test
    void drainIsIdempotentConcurrentAndRepeatedCallsShareFirstResult() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);

        runtime.spawn("app", "agent", "sess-idem",
                new SpawnOptions(false, List.of(events::add)));

        // 并发调用 drain：两线程同时触发，只生效一次，得到同一结果
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<DrainResult> a = vt.submit(() -> runtime.drain(Duration.ofSeconds(2)));
        Future<DrainResult> b = vt.submit(() -> runtime.drain(Duration.ofSeconds(2)));
        DrainResult first = a.get(5, TimeUnit.SECONDS);
        DrainResult second = b.get(5, TimeUnit.SECONDS);
        vt.shutdownNow();

        assertThat(second).isEqualTo(first);
        assertThat(first.drainedCount()).isEqualTo(1);

        // 重复调用：得到同一结果
        DrainResult third = runtime.drain(Duration.ofSeconds(2));
        assertThat(third).isEqualTo(first);
    }

    private int indexOfEventType(String type) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).type().equals(type)) {
                return i;
            }
        }
        throw new AssertionError("事件流中未找到类型: " + type + "，实际事件: " + events);
    }

    // ---- ticket 02: 等完在途轮次 + EXIT 档 flush 联动 ----

    /** 恢复配置 helper：指定持久化档位（EXIT 档 flush 联动测试用）。 */
    private static RecoveryConfig recoveryWithTier(DurabilityTier tier) {
        return new RecoveryConfig(true, Duration.ofSeconds(5), Duration.ofMillis(500),
                tier, ResumeStrategy.VOID, RecoveryConfig.DEFAULT_CRASHLOOP_HARD_CAP, true);
    }

    @Test
    void drainWaitsForInFlightTurnThenCloses() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        BlockingTool tool = new BlockingTool("slow_op", "done");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                recoveryWithTier(DurabilityTier.ASYNC), tool);

        AgentSession session = runtime.spawn("app", "agent", "sess-inflight",
                new SpawnOptions(false, List.of(events::add)));
        model.enqueue(toolCall("tc-1", "slow_op"));
        model.enqueueText("轮次完成");
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<String> chat = vt.submit(() -> session.chat("跑长任务"));
        // 工具已开始执行（在途轮次）：drain 不应返回
        assertThat(tool.started.await(2, TimeUnit.SECONDS)).isTrue();

        // drain 在另一虚拟线程上发起；在途轮次未完结前应阻塞
        Future<DrainResult> drainFuture = vt.submit(() -> runtime.drain(Duration.ofSeconds(5)));
        assertThatThrownBy(() -> drainFuture.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);

        // 释放 latch → 轮次完结 → drain 返回；工具恰好执行一次
        tool.release.countDown();
        assertThat(chat.get(5, TimeUnit.SECONDS)).isEqualTo("轮次完成");
        DrainResult result = drainFuture.get(5, TimeUnit.SECONDS);
        assertThat(result.drainedCount()).isEqualTo(1);
        assertThat(tool.calls).hasValue(1);

        // drain.session.completed 事件带 sessionId + 处置方式=等完 + 耗时
        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("drain.session.completed");
            assertThat(e.payload())
                    .containsEntry("sessionId", "sess-inflight")
                    .containsEntry("disposition", "waited");
            assertThat(e.payload()).containsKey("durationMs");
        });
        vt.shutdownNow();
    }

    @Test
    void idleSessionIsClosedDirectlyWithoutWaiting() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);

        runtime.spawn("app", "agent", "sess-idle-direct",
                new SpawnOptions(false, List.of(events::add)));

        // 空闲会话：drain 应迅速返回（无在途轮次可等）
        java.time.Instant before = java.time.Instant.now();
        DrainResult result = runtime.drain(Duration.ofSeconds(5));
        Duration elapsed = Duration.between(before, java.time.Instant.now());
        assertThat(result.drainedCount()).isEqualTo(1);
        // 无在途轮次：drain 不应消耗显著时间（远低于预算）
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void drainFlushesExitTierBufferedWritesOnClose() {
        // durability-tier=EXIT：轮次完结但缓冲未落底层；drain 关闭会话触发既有 flush 钩子同步落盘
        String sid = "sess-exit-drain";
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                recoveryWithTier(DurabilityTier.EXIT));

        AgentSession session = runtime.spawn("app", "agent", sid,
                new SpawnOptions(false, List.of(events::add)));
        model.enqueueText("完成");
        session.chat("你好");
        // EXIT 档：轮次完结但底层尚不可见（缓冲）
        assertThat(stores.messageStore().load(sid)).isEmpty();

        // drain 关闭会话 → 触发既有谢幕链 → EXIT flush 同步落盘
        runtime.drain(Duration.ofSeconds(5));
        assertThat(stores.messageStore().load(sid)).isNotEmpty();
    }

    @Test
    void singleSessionCloseExceptionDoesNotBlockOthers() {
        // 同一 runtime 上两个会话：sess-boom 注册 close 时抛异常的资源，sess-ok 正常。
        // drain 仍关闭两者（boom 异常被首异常收集），最终汇总抛出 boom 异常；sess-ok 已 close（租约释放可接管）。
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionResourceCustomizer boomCustomizer = (registry, appId, agentName, sessionId) -> {
            if ("sess-boom".equals(sessionId)) {
                registry.register("boom", () -> {
                    throw new RuntimeException("boom on close: " + sessionId);
                });
            }
        };
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.merge(
                        io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                        io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.sessionCustomizers(List.of(boomCustomizer))));

        runtime.spawn("app", "agent", "sess-boom");
        runtime.spawn("app", "agent", "sess-ok",
                new SpawnOptions(false, List.of(events::add)));

        // drain 收集首异常后汇总抛出（boom 会话）；sess-ok 仍被正常 close
        assertThatThrownBy(() -> runtime.drain(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");

        // sess-ok 的租约已释放：另一 runtime 可立即 spawn 续接
        AgentRuntime runtime2 = Buzhou.runtime(new ScriptedChatModel(), stores);
        AgentSession resumed = runtime2.spawn("app", "agent", "sess-ok");
        resumed.close();
        runtime2.drain(Duration.ofSeconds(2));
    }

    // ---- ticket 03: 超时强杀（取消传播） ----

    @Test
    void drainForceKillsOnTimeoutViaCancelPropagation() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        BlockingTool tool = new BlockingTool("slow_op", "done");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                recoveryWithTier(DurabilityTier.ASYNC), tool);

        AgentSession session = runtime.spawn("app", "agent", "sess-kill",
                new SpawnOptions(false, List.of(events::add)));
        model.enqueue(toolCall("tc-1", "slow_op"));
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<String> chat = vt.submit(() -> session.chat("跑长任务"));
        assertThat(tool.started.await(2, TimeUnit.SECONDS)).isTrue();

        // 不释放 latch：drain 预算耗尽后强杀（取消传播到达工具层——工具收到中断语义）
        Duration budget = Duration.ofMillis(500);
        java.time.Instant before = java.time.Instant.now();
        DrainResult result = runtime.drain(budget);
        Duration elapsed = Duration.between(before, java.time.Instant.now());
        // drain 在预算附近返回（预算 + 处理 slack，远低于工具的 10s await）
        assertThat(elapsed).isLessThan(Duration.ofSeconds(3));

        // 取消传播到达工具层：工具收到中断语义
        assertThat(tool.interrupted.await(2, TimeUnit.SECONDS)).isTrue();
        // 强杀事件带被强杀 sessionId
        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("drain.timeout-force-kill");
            assertThat(e.payload()).containsEntry("count", 1);
            java.util.List<?> ids = (java.util.List<?>) e.payload().get("sessionIds");
            assertThat(ids).hasSize(1);
            assertThat(ids.get(0)).isEqualTo("sess-kill");
        });
        // drain.finished 计数区分等完/强杀
        assertThat(result.forceKilledCount()).isEqualTo(1);
        assertThat(result.drainedCount()).isEqualTo(0);
        // drain.session.completed 处置方式=强杀
        assertThat(events).anySatisfy(e -> {
            assertThat(e.type()).isEqualTo("drain.session.completed");
            assertThat(e.payload())
                    .containsEntry("sessionId", "sess-kill")
                    .containsEntry("disposition", "force-killed");
        });

        // 强杀后会话仍被正常 close：租约已释放，同 sessionId 可立即在新 runtime spawn 续接
        AgentRuntime runtime2 = Buzhou.runtime(new ScriptedChatModel(), stores);
        AgentSession resumed = runtime2.spawn("app", "agent", "sess-kill");
        resumed.close();
        runtime2.drain(Duration.ofSeconds(2));
        vt.shutdownNow();
    }

    @Test
    void drainMixesWaitedAndForceKilledSessions() throws Exception {
        // 竞争边界（latch 控制时序，无 sleep）：两会话同一 runtime——一会话释放 latch 等完，
        // 另一会话不释放被强杀。drain.finished 计数 drainedCount=1 / forceKilledCount=1。
        ScriptedChatModel model = new ScriptedChatModel();
        BlockingTool toolA = new BlockingTool("slow_a", "a-done");
        BlockingTool toolB = new BlockingTool("slow_b", "b-done");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                recoveryWithTier(DurabilityTier.ASYNC), toolA, toolB);

        AgentSession sessionA = runtime.spawn("app", "agent", "sess-mix-a",
                new SpawnOptions(false, List.of(events::add)));
        AgentSession sessionB = runtime.spawn("app", "agent", "sess-mix-b",
                new SpawnOptions(false, List.of(events::add)));
        // 两个工具调用先入队（FIFO 共享队列，两会话各取一个），再入一个文本（供等完会话的二次模型调用）
        model.enqueue(toolCall("tc-a", "slow_a"));
        model.enqueue(toolCall("tc-b", "slow_b"));
        model.enqueueText("完成");
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<String> chatA = vt.submit(() -> sessionA.chat("跑 a"));
        Future<String> chatB = vt.submit(() -> sessionB.chat("跑 b"));
        assertThat(toolA.started.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(toolB.started.await(2, TimeUnit.SECONDS)).isTrue();

        // 释放 A 的 latch（等完），不释放 B（强杀）；drain 用足够 A 完结但 B 超时的预算
        toolA.release.countDown();
        DrainResult result = runtime.drain(Duration.ofSeconds(3));
        assertThat(result.drainedCount()).isEqualTo(1);
        assertThat(result.forceKilledCount()).isEqualTo(1);
        // B 收到取消传播
        assertThat(toolB.interrupted.await(2, TimeUnit.SECONDS)).isTrue();

        // 两会话均被 close：租约均释放，另一 runtime 可分别 spawn 续接
        AgentRuntime runtime2 = Buzhou.runtime(new ScriptedChatModel(), stores);
        runtime2.spawn("app", "agent", "sess-mix-a").close();
        runtime2.spawn("app", "agent", "sess-mix-b").close();
        runtime2.drain(Duration.ofSeconds(2));
        vt.shutdownNow();
    }

    @Test
    void drainWaitsForInFlightStreamTurn() throws Exception {
        // 轮次在途信号覆盖 stream 形态：stream() 内 onTurnStart 触发（返回前）→ drain 等完；
        // 订阅完结 onTurnEnd → drain 返回
        ScriptedChatModel model = new ScriptedChatModel();
        BlockingTool tool = new BlockingTool("slow_stream_op", "done");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                recoveryWithTier(DurabilityTier.ASYNC), tool);

        AgentSession session = runtime.spawn("app", "agent", "sess-stream",
                new SpawnOptions(false, List.of(events::add)));
        model.enqueue(toolCall("tc-s", "slow_stream_op"));
        model.enqueueText("stream 完成");
        // stream() 立即返回（Flux 惰性）；onTurnStart 已在 stream() 内触发 → 轮次在途
        reactor.core.publisher.Flux<org.springframework.ai.chat.model.ChatResponse> flux = session.stream("跑流式任务");
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        // 订阅触发模型调用 → 工具阻塞
        java.util.concurrent.Future<?> subscription = vt.submit(() -> flux.blockLast());
        assertThat(tool.started.await(2, TimeUnit.SECONDS)).isTrue();

        // drain 在途轮次未完结前应阻塞（stream turn 在途，onTurnEnd 未触发）
        java.util.concurrent.Future<DrainResult> drainFuture = vt.submit(() -> runtime.drain(Duration.ofSeconds(5)));
        assertThatThrownBy(() -> drainFuture.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);

        // 释放 latch → 工具完结 → 流完结 → onTurnEnd → drain 返回
        tool.release.countDown();
        DrainResult result = drainFuture.get(5, TimeUnit.SECONDS);
        assertThat(result.drainedCount()).isEqualTo(1);
        vt.shutdownNow();
    }

    @Test
    void drainForceKillsAndFlushesExitTierBufferedWrites() throws Exception {
        // EXIT 档下被强杀会话的缓冲写仍被 flush（close 路径一致）
        String sid = "sess-exit-kill";
        ScriptedChatModel model = new ScriptedChatModel();
        BlockingTool tool = new BlockingTool("slow_op", "done");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                recoveryWithTier(DurabilityTier.EXIT), tool);

        AgentSession session = runtime.spawn("app", "agent", sid,
                new SpawnOptions(false, List.of(events::add)));
        model.enqueue(toolCall("tc-1", "slow_op"));
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        vt.submit(() -> session.chat("跑长任务"));
        assertThat(tool.started.await(2, TimeUnit.SECONDS)).isTrue();

        // 不释放 latch：强杀 + close → EXIT flush 同步落盘缓冲写（USER + ASSISTANT 工具调用消息）
        DrainResult result = runtime.drain(Duration.ofMillis(500));
        assertThat(result.forceKilledCount()).isEqualTo(1);
        assertThat(stores.messageStore().load(sid)).isNotEmpty();
        vt.shutdownNow();
    }
}
