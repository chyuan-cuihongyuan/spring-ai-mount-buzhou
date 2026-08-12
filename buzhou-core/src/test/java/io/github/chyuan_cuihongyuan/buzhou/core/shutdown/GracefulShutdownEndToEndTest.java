package io.github.chyuan_cuihongyuan.buzhou.core.shutdown;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.DrainResult;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeDrainingException;
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
 * 优雅停机（drain）e2e 骨架（ticket 01）：复用 CrashRecoveryEndToEndTest 装配形态
 * （{@code Buzhou.runtime(...)} + {@link ScriptedChatModel} + latch 阻塞工具 + 事件捕获）。
 * 全程断言「drain 结果 + 事件流 + 租约/可接管性 + 幂等」；计时用 CountDownLatch，无 wall-clock sleep。
 * 后续票据（02/03）在此骨架上增量补入「等完在途轮次 / 超时强杀」用例。
 */
class GracefulShutdownEndToEndTest {

    private final List<SessionEvent> events = new CopyOnWriteArrayList<>();

    /** 阻塞型副作用工具：started 后开始等待 release，count 记录真实执行次数。 */
    static final class BlockingTool implements ToolCallback {
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
}
