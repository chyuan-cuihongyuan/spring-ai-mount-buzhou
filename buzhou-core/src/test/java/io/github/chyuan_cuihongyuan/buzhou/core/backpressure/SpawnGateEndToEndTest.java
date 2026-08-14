package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.HarnessAssembler;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionCapacityExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-45 / spec 14 §A：spawn 容量闸端到端（main 版，适配 impl-30 停机语义）。
 *
 * <p>主接缝 = {@code Buzhou.runtime(...)} + 新构造入口（SpawnGate 注入 {@code DefaultAgentRuntime}）。
 * 好测试只测外部行为：满载拒绝（FAIL_FAST/QUEUE 超时）、close 归还空位、停机唤醒排队者、
 * steal 接管不占容量。
 */
class SpawnGateEndToEndTest {

    @Test
    void failFastRejectsWhenCapacityFull() {
        DefaultAgentRuntime runtime = runtimeWithGate(1, OverloadPolicy.FAIL_FAST, Duration.ofSeconds(1));
        AgentSession first = runtime.spawn("app", "a", "s1");
        first.chat("hi"); // 会话真实可用（ScriptedChatModel 默认回复）

        assertThatThrownBy(() -> runtime.spawn("app", "a", "s2"))
                .isInstanceOf(SessionCapacityExceededException.class)
                .hasMessageContaining("s2");
        first.close();
        runtime.shutdownGracefully(Duration.ofSeconds(5));
    }

    @Test
    void closeReleasesSlotForNextSpawn() {
        DefaultAgentRuntime runtime = runtimeWithGate(1, OverloadPolicy.FAIL_FAST, Duration.ofSeconds(1));
        AgentSession first = runtime.spawn("app", "a", "s1");
        first.close();

        // close 后空位归还：下一个 spawn 成功
        AgentSession second = runtime.spawn("app", "a", "s2");
        assertThat(second.chat("hi")).isEqualTo("ok"); // helper 预入队的回复（会话真实可用）
        second.close();
        runtime.shutdownGracefully(Duration.ofSeconds(5));
    }

    @Test
    void queueTimeoutRejectsWithWaitedDuration() {
        DefaultAgentRuntime runtime = runtimeWithGate(1, OverloadPolicy.QUEUE, Duration.ofMillis(150));
        AgentSession first = runtime.spawn("app", "a", "s1");

        long start = System.nanoTime();
        assertThatThrownBy(() -> runtime.spawn("app", "a", "s2"))
                .isInstanceOf(SessionCapacityExceededException.class);
        long waitedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(waitedMs).isGreaterThanOrEqualTo(100); // 确实排队等待过
        first.close();
        runtime.shutdownGracefully(Duration.ofSeconds(5));
    }

    @Test
    void shutdownWakesQueuedWaiterWithStructuredRefusal() throws Exception {
        DefaultAgentRuntime runtime = runtimeWithGate(1, OverloadPolicy.QUEUE, Duration.ofSeconds(30));
        AgentSession first = runtime.spawn("app", "a", "s1");

        ExecutorService testExec = Executors.newVirtualThreadPerTaskExecutor();
        try {
            CountDownLatch queued = new CountDownLatch(1);
            Future<Object> waiter = testExec.submit(() -> {
                queued.countDown();
                try {
                    return runtime.spawn("app", "a", "s2");
                } catch (RuntimeException e) {
                    return e;
                }
            });
            assertThat(queued.await(2, TimeUnit.SECONDS)).isTrue();
            // 排队中的 spawn：给一点时间确保已进入等待，随后停机唤醒
            runtime.shutdownGracefully(Duration.ofSeconds(5));
            Object outcome = waiter.get(10, TimeUnit.SECONDS);
            assertThat(outcome)
                    .as("排队等待者被停机唤醒并收到 SHUTDOWN_INTERRUPTED，而非排队超时")
                    .isInstanceOf(BuzhouException.class);
            assertThat(((BuzhouException) outcome).errorCode()).isEqualTo(ErrorCode.SHUTDOWN_INTERRUPTED);
        } finally {
            testExec.shutdownNow();
            first.close();
        }
    }

    @Test
    void stealTakeoverDoesNotConsumeCapacity() {
        DefaultAgentRuntime runtime = runtimeWithGate(1, OverloadPolicy.FAIL_FAST, Duration.ofSeconds(1));
        AgentSession first = runtime.spawn("app", "a", "s1");
        first.close();
        // 先占满
        AgentSession holder = runtime.spawn("app", "a", "s1");
        // steal 接管路径：不占新容量（同一 sessionId 的接管）
        AgentSession taken = runtime.spawn("app", "a", "s1",
                io.github.chyuan_cuihongyuan.buzhou.core.session.SpawnOptions.withSteal());
        assertThat(taken).isNotNull();
        taken.close();
        runtime.shutdownGracefully(Duration.ofSeconds(5));
    }

    @Test
    void gateEventsEmittedForRejection() {
        List<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> events =
                new CopyOnWriteArrayList<>();
        DefaultAgentRuntime runtime = runtimeWithGate(1, OverloadPolicy.FAIL_FAST, Duration.ofSeconds(1), events);
        AgentSession first = runtime.spawn("app", "a", "s1");
        assertThatThrownBy(() -> runtime.spawn("app", "a", "s2"))
                .isInstanceOf(SessionCapacityExceededException.class);

        assertThat(events).anyMatch(e -> SpawnGate.EVENT_SPAWN_REJECTED.equals(e.type())
                && SpawnGate.REASON_FAIL_FAST.equals(e.payload().get("reason"))
                && Integer.valueOf(1).equals(e.payload().get("limit")));
        first.close();
        runtime.shutdownGracefully(Duration.ofSeconds(5));
    }

    // ---- helpers ----

    private static DefaultAgentRuntime runtimeWithGate(int limit, OverloadPolicy policy,
            Duration queueTimeout) {
        return runtimeWithGate(limit, policy, queueTimeout, new CopyOnWriteArrayList<>());
    }

    private static DefaultAgentRuntime runtimeWithGate(int limit, OverloadPolicy policy, Duration queueTimeout,
            List<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> events) {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(new AssistantMessage("ok"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        SpawnGate gate = new SpawnGate(limit, queueTimeout, policy, events::add);
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(model, stores, new HarnessAssembler(),
                RuntimeConfig.defaults(), null, null, null, null, gate);
        return runtime;
    }
}
