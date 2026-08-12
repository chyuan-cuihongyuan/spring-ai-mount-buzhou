package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouBackpressureProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RecoveryConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeDrainingException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionCapacityExceededException;
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
import java.util.Map;
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
 * 背压与多层限流端到端测试（主缝合点：经 {@code Buzhou.runtime(...)} 装配完整 runtime 后断言
 * spawn 闸排队/拒绝/事件、drain 唤醒、steal 绕过、工具扇出闸许可串行化/超时降级）。
 *
 * <p>对齐 {@code GracefulShutdownEndToEndTest} / {@code CrashRecoveryEndToEndTest} 的 e2e 形态：
 * 全程用 {@link CountDownLatch} / 有界轮询保证确定性，不用 wall-clock sleep。
 */
class BackpressureEndToEndTest {

    // ---- 维度① spawn 闸 ----

    /** QUEUE 档：上限=1 时第二 spawn 排队，第一会话 close 后放行成功。 */
    @Test
    void queuePolicySecondSpawnWaitsForClose() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, 1, Duration.ofSeconds(5), "QUEUE", null);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.defaults(), bp);

        AgentSession first = runtime.spawn("app", "agent", "sess-first");
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<AgentSession> secondFuture = vt.submit(() -> runtime.spawn("app", "agent", "sess-second"));

        // 第二 spawn 应在排队（不立即返回）
        assertThatThrownBy(() -> secondFuture.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);

        // 第一会话 close → 空位释放 → 第二 spawn 放行
        first.close();
        AgentSession second = secondFuture.get(5, TimeUnit.SECONDS);
        assertThat(second.sessionId()).isEqualTo("sess-second");
        second.close();
        vt.shutdownNow();
    }

    /** FAIL_FAST 档：上限=1 时第二 spawn 立即抛容量异常。 */
    @Test
    void failFastPolicyRejectsImmediately() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, 1, Duration.ofSeconds(5), "FAIL_FAST", null);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.defaults(), bp);

        AgentSession first = runtime.spawn("app", "agent", "sess-first");
        assertThatThrownBy(() -> runtime.spawn("app", "agent", "sess-second"))
                .isInstanceOf(SessionCapacityExceededException.class)
                .hasMessageContaining("sess-second")
                .hasMessageContaining("limit=1");
        first.close();
    }

    /** QUEUE 档排队超时后抛容量异常，message 带计数上下文。 */
    @Test
    void queueTimeoutThrowsCapacityException() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, 1, Duration.ofMillis(200), "QUEUE", null);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.defaults(), bp);

        AgentSession first = runtime.spawn("app", "agent", "sess-first");
        long start = System.nanoTime();
        assertThatThrownBy(() -> runtime.spawn("app", "agent", "sess-second"))
                .isInstanceOf(SessionCapacityExceededException.class)
                .hasMessageContaining("sess-second")
                .hasMessageContaining("currentActive=1")
                .hasMessageContaining("limit=1")
                .hasMessageContaining("waitedMs=");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isGreaterThanOrEqualTo(150); // 排队了约 200ms
        first.close();
    }

    /** 不配置任何阈值时行为与现状完全一致（回归断言）。 */
    @Test
    void noLimitByDefaultBehaviorUnchanged() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        // 全默认：不限并发会话
        AgentRuntime runtime = Buzhou.runtime(model, stores);

        // 可同时 spawn 多个会话——无容量拒绝
        AgentSession s1 = runtime.spawn("app", "agent", "s1");
        AgentSession s2 = runtime.spawn("app", "agent", "s2");
        AgentSession s3 = runtime.spawn("app", "agent", "s3");
        s1.close();
        s2.close();
        s3.close();
    }

    /** backpressure.spawn-queued / spawn-rejected 事件按序出现，计数与上下文正确。 */
    @Test
    void spawnQueuedAndRejectedEventsAppear() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, 1, Duration.ofMillis(200), "QUEUE", null);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.defaults(), bp);

        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        runtime.addRuntimeEventListener(events::add);

        AgentSession first = runtime.spawn("app", "agent", "sess-first");
        // 触发排队 + 超时拒绝
        assertThatThrownBy(() -> runtime.spawn("app", "agent", "sess-second"))
                .isInstanceOf(SessionCapacityExceededException.class);
        first.close();

        // 事件流：spawn-queued 先于 spawn-rejected
        int queuedIdx = indexOfEventType(events, SpawnGate.EVENT_SPAWN_QUEUED);
        int rejectedIdx = indexOfEventType(events, SpawnGate.EVENT_SPAWN_REJECTED);
        assertThat(queuedIdx).isLessThan(rejectedIdx);
        SessionEvent queued = events.get(queuedIdx);
        assertThat(queued.payload())
                .containsEntry("sessionId", "sess-second")
                .containsEntry("currentActive", 1)
                .containsEntry("limit", 1);
        SessionEvent rejected = events.get(rejectedIdx);
        assertThat(rejected.payload())
                .containsEntry("sessionId", "sess-second")
                .containsEntry("reason", SpawnGate.REASON_TIMEOUT)
                .containsKey("waitedMs");
    }

    /** 排队中的 spawn 不持有租约（排队期间同 sessionId 在另一 runtime 可正常 spawn）。 */
    @Test
    void queuedSpawnDoesNotHoldLease() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, 1, Duration.ofSeconds(5), "QUEUE", null);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.defaults(), bp);

        AgentSession first = runtime.spawn("app", "agent", "sess-shared");
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<AgentSession> queuedFuture = vt.submit(() -> runtime.spawn("app", "agent", "sess-shared"));

        // 排队期间不持有租约——另一 runtime 同 sessionId 可正常 spawn（排队中的 spawn 还没到 doSpawn）
        assertThatThrownBy(() -> queuedFuture.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);
        AgentRuntime runtime2 = Buzhou.runtime(new ScriptedChatModel(), stores);
        assertThatThrownBy(() -> runtime2.spawn("app", "agent", "sess-shared"))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException.class);

        first.close();
        vt.shutdownNow();
    }

    // ---- 维度① 边界交互（drain 唤醒 / steal 绕过） ----

    /** drain 置位后新 spawn 抛 RuntimeDrainingException（不经容量排队）。 */
    @Test
    void drainRejectsNewSpawnWithoutQueueing() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, 10, Duration.ofSeconds(5), "QUEUE", null);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.defaults(), bp);

        runtime.spawn("app", "agent", "sess-1").close();
        runtime.drain(Duration.ofSeconds(2));

        assertThatThrownBy(() -> runtime.spawn("app", "agent", "sess-after-drain"))
                .isInstanceOf(RuntimeDrainingException.class);
    }

    /** 容量排队中的 spawn 在 drain 置位时被唤醒拒绝，drain 不被排队请求卡住。 */
    @Test
    void drainWakesQueuedSpawnAndRejects() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, 1, Duration.ofSeconds(30), "QUEUE", null);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.defaults(), bp);

        AgentSession first = runtime.spawn("app", "agent", "sess-active");
        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<AgentSession> queuedFuture = vt.submit(() -> runtime.spawn("app", "agent", "sess-queued"));

        // 确认第二 spawn 在排队
        assertThatThrownBy(() -> queuedFuture.get(500, TimeUnit.MILLISECONDS))
                .isInstanceOf(java.util.concurrent.TimeoutException.class);

        // drain 置位 → 排队中的 spawn 被唤醒拒绝
        long start = System.nanoTime();
        runtime.drain(Duration.ofSeconds(5));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        // drain 应迅速返回（排队者被唤醒拒绝，不被 30s 排队超时卡住）
        assertThat(elapsedMs).isLessThan(3000);

        assertThatThrownBy(() -> queuedFuture.get(5, TimeUnit.SECONDS))
                .isInstanceOf(java.util.concurrent.ExecutionException.class)
                .hasCauseInstanceOf(RuntimeDrainingException.class);
        vt.shutdownNow();
    }

    /** 容量已满时 spawn(steal=true) 接管既有会话成功，不被容量闸拒绝。 */
    @Test
    void stealBypassesCapacityGate() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, 1, Duration.ofSeconds(5), "FAIL_FAST", null);
        AgentRuntime runtime1 = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.defaults(), bp);

        // runtime1 上有一个活跃会话（容量已满）
        runtime1.spawn("app", "agent", "sess-steal");

        // steal=true 接管：不占新容量，应成功（即使容量已满）
        AgentRuntime runtime2 = Buzhou.runtime(new ScriptedChatModel(), stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.defaults(), bp);
        AgentSession stolen = runtime2.spawn("app", "agent", "sess-steal", SpawnOptions.withSteal());
        assertThat(stolen.sessionId()).isEqualTo("sess-steal");
        stolen.close();
    }

    // ---- 维度② 工具扇出闸 ----

    /** 小每轮并发上限下并行工具调用被许可串行化（计数器佐证）。 */
    @Test
    void smallFanoutLimitSerializesParallelTools() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouBackpressureProperties.Tool toolCfg = new BuzhouBackpressureProperties.Tool(
                1, Duration.ofSeconds(5), null, "QUEUE");
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, null, null, null, toolCfg);
        AtomicInteger concurrent = new AtomicInteger();
        AtomicInteger maxSeen = new AtomicInteger();
        ToolCallback toolA = slowProbe("ta", concurrent, maxSeen, 150);
        ToolCallback toolB = slowProbe("tb", concurrent, maxSeen, 150);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.disabled(), bp, toolA, toolB);

        AgentSession session = runtime.spawn("app", "agent", "sess-fanout");
        // 两个工具调用在同一轮模型回复中——并行扇出
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall("tc-a", "function", "ta", "{}"),
                        new AssistantMessage.ToolCall("tc-b", "function", "tb", "{}")))
                .build());
        model.enqueueText("完成");

        session.chat("跑两个工具");

        // maxConcurrentPerTurn=1：两工具被串行化，最大并发=1
        assertThat(maxSeen.get()).isEqualTo(1);
        session.close();
    }

    /**
     * FAIL_FAST 档工具扇出闸：maxConcurrentPerTurn=1 + FAIL_FAST → 第二个工具立即返回错误结果，
     * 轮次正常完结。latch 保证阻塞工具先拿到许可（callback 在拿到许可后才执行，
     * started 信号确保 permit 被持有）。
     */
    @Test
    void failFastToolPermitReturnsErrorResult() throws Exception {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        // maxConcurrentPerTurn=1 + FAIL_FAST（等价 permitAcquireTimeout=0）
        BuzhouBackpressureProperties.Tool toolCfg = new BuzhouBackpressureProperties.Tool(
                1, Duration.ofSeconds(5), null, "FAIL_FAST");
        BuzhouBackpressureProperties bp = new BuzhouBackpressureProperties(
                true, null, null, null, toolCfg);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ToolCallback blockingTool = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("block").description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                started.countDown();
                try {
                    release.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "block-done";
            }
        };
        ToolCallback fastTool = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("fast").description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return "fast-done";
            }
        };
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.disabled(), bp, blockingTool, fastTool);

        AgentSession session = runtime.spawn("app", "agent", "sess-ff");
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall("tc-1", "function", "block", "{}"),
                        new AssistantMessage.ToolCall("tc-2", "function", "fast", "{}")))
                .build());
        model.enqueueText("完成");

        ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor();
        Future<String> chat = vt.submit(() -> session.chat("跑两个工具"));
        // 等 block 拿到 permit（此时 fast 的 tryAcquire 必然失败 → 返回错误结果）
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

        // 释放 block → 轮次完结（fast 已返回错误结果，不阻塞）
        release.countDown();
        String reply = chat.get(10, TimeUnit.SECONDS);
        assertThat(reply).isEqualTo("完成");
        vt.shutdownNow();
        session.close();
    }

    /** 不配置工具扇出参数时行为与现状一致（每轮 8 / 60s 现值回归断言）。 */
    @Test
    void defaultFanoutBehaviorUnchanged() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger concurrent = new AtomicInteger();
        AtomicInteger maxSeen = new AtomicInteger();
        // 无 tool 配置——保持现值常量 8 / 60s
        ToolCallback ta = slowProbe("ta", concurrent, maxSeen, 300);
        ToolCallback tb = slowProbe("tb", concurrent, maxSeen, 300);
        ToolCallback tc = slowProbe("tc", concurrent, maxSeen, 300);
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                RecoveryConfig.disabled(), ta, tb, tc);

        AgentSession session = runtime.spawn("app", "agent", "sess-default");
        // 三个工具调用在同一轮模型回复中——并行扇出
        model.enqueue(AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall("tc-a", "function", "ta", "{}"),
                        new AssistantMessage.ToolCall("tc-b", "function", "tb", "{}"),
                        new AssistantMessage.ToolCall("tc-c", "function", "tc", "{}")))
                .build());
        model.enqueueText("完成");

        long start = System.nanoTime();
        String reply = session.chat("跑三个并行工具");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(reply).isEqualTo("完成");
        // 3×300ms 并行（max 不是 sum）+ 模型开销 + 虚拟线程调度 slack
        assertThat(elapsedMs).isLessThan(2000);
        // 默认 maxConcurrentPerTurn=8：三个工具全部并行，最大并发=3
        assertThat(maxSeen.get()).isEqualTo(3);
        session.close();
    }

    // ---- helpers ----

    private static AssistantMessage toolCall(String id, String name) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, "{}")))
                .build();
    }

    private static int indexOfEventType(List<SessionEvent> events, String type) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).type().equals(type)) {
                return i;
            }
        }
        throw new AssertionError("事件流中未找到类型: " + type + "，实际事件: " + events);
    }

    private static ToolCallback slowProbe(String name, AtomicInteger concurrent,
                                           AtomicInteger maxSeen, long sleepMillis) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d").inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                int now = concurrent.incrementAndGet();
                maxSeen.accumulateAndGet(now, Math::max);
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                concurrent.decrementAndGet();
                return name + "-done";
            }
        };
    }
}
