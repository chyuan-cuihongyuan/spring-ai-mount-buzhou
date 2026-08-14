package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-30 / spec 13 §core-1（优雅停机与生命周期）core 级测试：
 *
 * <ul>
 *   <li>停机拒绝——spawn 与既有会话的 chat/stream 以结构化
 *       {@link ErrorCode#SHUTDOWN_INTERRUPTED}（RETRYABLE）拒绝；</li>
 *   <li>排空语义——在途 Turn（长工具）收到 AFTER_CURRENT_TURN：当前工具批完成后本轮自然收尾，
 *       预算内排空完成（callback 前提）、会话关闭、租约释放、注册表注销；</li>
 *   <li>超时硬截断——预算耗尽对在途会话 IMMEDIATE 取消 + executor shutdownNow，
 *       停机调用本身有限时间返回（会话同样收尾关闭）；</li>
 *   <li>phase 契约——{@link AgentRuntimeLifecycle#getPhase()} = CORE（最大，最先 stop）。</li>
 * </ul>
 *
 * <p>计时断言沿用契约测试的真实时序风格（预算与工具时长两侧 ≥200ms 余量防 flake）。</p>
 */
class DefaultAgentRuntimeShutdownTest {

    private static final long TOOL_MILLIS = 400L;

    @Test
    void shouldRejectSpawnAndNewTurns_whenRuntimeShuttingDown() {
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("回复"));
        DefaultAgentRuntime runtime = newRuntime(model, Duration.ofSeconds(5));
        DefaultAgentSession session =
                (DefaultAgentSession) runtime.spawn("app", "agent", "shutdown-reject");

        // 停机前正常可用
        assertThat(session.chat("你好")).isEqualTo("回复");

        // 停机拒新通道（会话层）：SHUTDOWN_INTERRUPTED、RETRYABLE
        session.beginShutdown();
        assertThatThrownBy(() -> session.chat("再来一轮"))
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.SHUTDOWN_INTERRUPTED))
                .hasMessageContaining("拒绝新 Turn");
        // stream 同通道拒绝
        assertThatThrownBy(() -> session.stream("再来一轮").blockFirst())
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.SHUTDOWN_INTERRUPTED));

        // 停机后 spawn 拒绝（运行时层）：SHUTDOWN_INTERRUPTED
        runtime.shutdownGracefully(Duration.ofSeconds(5));
        assertThat(runtime.isShuttingDown()).isTrue();
        assertThatThrownBy(() -> runtime.spawn("app", "agent", "shutdown-reject-2"))
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.SHUTDOWN_INTERRUPTED))
                .hasMessageContaining("拒绝创建新会话");
        // 二次停机幂等（destroy 兜底双触发无害）
        runtime.close();
    }

    @Test
    void shouldDrainInFlightTurnsAndReleaseResources_whenShutdownWithinBudget() throws Exception {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("slow_tool", "{}"),
                ScriptStep.text("本轮完成"));
        CountDownLatch toolStarted = new CountDownLatch(1);
        AtomicReference<String> toolResult = new AtomicReference<>("未执行");
        BuzhouStores stores = Buzhou.inMemoryStores();
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(model, stores,
                new HarnessAssembler(), RuntimeConfig.defaults(),
                null, null, Duration.ofSeconds(5), slowTool("slow_tool", toolStarted, toolResult));

        AgentSession session = runtime.spawn("app", "agent", "drain-graceful");
        CompletableFuture<String> reply = CompletableFuture.supplyAsync(() -> session.chat("开始"));
        assertThat(toolStarted.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(runtime.activeSessionCount()).isEqualTo(1);

        boolean drained = runtime.shutdownGracefully(Duration.ofSeconds(5));

        // 预算内排空：AFTER_CURRENT_TURN 不打断——当前工具完成、本轮自然收尾
        assertThat(drained).isTrue();
        assertThat(reply.get(5, TimeUnit.SECONDS)).isEqualTo("本轮完成");
        assertThat(toolResult.get()).isEqualTo("slow_tool-done");
        // 会话已被停机序列关闭：注册表注销、租约释放、后续调用按「会话已关闭」拒绝
        assertThat(runtime.activeSessionCount()).isZero();
        assertThat(stores.sessionLeaseStore().inspect("drain-graceful")).isEmpty();
        assertThatThrownBy(() -> session.chat("停机后")).hasMessageContaining("already closed");
    }

    @Test
    void shouldHardCutInFlightTurns_whenDrainExceedsBudget() throws Exception {
        // 工具挂起 10s（可中断 sleep）——预算 300ms 必然超时 → IMMEDIATE 硬截断
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("hang_tool", "{}"),
                ScriptStep.text("硬截断后收尾"));
        CountDownLatch toolStarted = new CountDownLatch(1);
        BuzhouStores stores = Buzhou.inMemoryStores();
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(model, stores,
                new HarnessAssembler(), RuntimeConfig.defaults(),
                null, null, Duration.ofSeconds(5), hangTool("hang_tool", toolStarted, 10_000L));

        AgentSession session = runtime.spawn("app", "agent", "drain-hardcut");
        CompletableFuture<String> reply = CompletableFuture.supplyAsync(() -> session.chat("开始"));
        assertThat(toolStarted.await(5, TimeUnit.SECONDS)).isTrue();

        long startNanos = System.nanoTime();
        boolean drained = runtime.shutdownGracefully(Duration.ofMillis(300));
        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;

        // 硬截断：停机调用本身在「预算 + 少量收尾」内返回（不等工具的 10s）
        assertThat(drained).isFalse();
        assertThat(elapsedMillis).isLessThan(5_000L);
        // 被中断的工具以取消反馈收口 → 模型给出最终回复，Turn 有限时间内终结
        assertThat(reply.get(5, TimeUnit.SECONDS)).isEqualTo("硬截断后收尾");
        // 收尾语义与优雅路径一致：会话关闭、租约释放
        assertThat(runtime.activeSessionCount()).isZero();
        assertThat(stores.sessionLeaseStore().inspect("drain-hardcut")).isEmpty();
    }

    @Test
    void shouldCloseSessionsAndStopRenewLoop_whenDestroyWithoutStop() {
        // 容器不调 stop 直接 destroy：close() 兜底收尾（不等待排空）
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("回复"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        DefaultAgentRuntime runtime = newRuntime(model, stores, Duration.ofSeconds(5));
        AgentSession session = runtime.spawn("app", "agent", "destroy-only");

        runtime.close();

        assertThat(runtime.isShuttingDown()).isTrue();
        assertThat(runtime.activeSessionCount()).isZero();
        assertThat(stores.sessionLeaseStore().inspect("destroy-only")).isEmpty();
        assertThatThrownBy(() -> session.chat("销毁后"))
                .hasMessageContaining("already closed");
        // 已完成的停机：再次 close / shutdownGracefully 幂等 no-op
        runtime.close();
        assertThat(runtime.shutdownGracefully(Duration.ofSeconds(1))).isTrue();
    }

    @Test
    void shouldReportCorePhaseAndCallbackAfterDrain_whenLifecycleStops() throws Exception {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("slow_tool", "{}"),
                ScriptStep.text("完成"));
        CountDownLatch toolStarted = new CountDownLatch(1);
        AtomicReference<String> toolResult = new AtomicReference<>("未执行");
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(model, Buzhou.inMemoryStores(),
                new HarnessAssembler(), RuntimeConfig.defaults(),
                null, null, Duration.ofSeconds(5), slowTool("slow_tool", toolStarted, toolResult));
        AgentRuntimeLifecycle lifecycle = new AgentRuntimeLifecycle(runtime, null);

        // phase 契约：core 最大（最先 stop）
        assertThat(lifecycle.getPhase()).isEqualTo(BuzhouLifecyclePhases.CORE);
        assertThat(lifecycle.isRunning()).isFalse();

        lifecycle.start();
        assertThat(lifecycle.isRunning()).isTrue();

        AgentSession session = runtime.spawn("app", "agent", "lifecycle-drain");
        CompletableFuture<String> reply = CompletableFuture.supplyAsync(() -> session.chat("开始"));
        assertThat(toolStarted.await(5, TimeUnit.SECONDS)).isTrue();

        // 回调时点必须已排空（活跃会话注册表已清空——stop 完成后回调的契约）
        AtomicInteger activeSessionsWhenCallback = new AtomicInteger(-1);
        CountDownLatch callbackRan = new CountDownLatch(1);
        lifecycle.stop(() -> {
            activeSessionsWhenCallback.set(runtime.activeSessionCount());
            callbackRan.countDown();
        });

        assertThat(callbackRan.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(lifecycle.isRunning()).isFalse();
        assertThat(activeSessionsWhenCallback.get()).isZero();
        assertThat(reply.get(5, TimeUnit.SECONDS)).isEqualTo("完成");
    }

    private static DefaultAgentRuntime newRuntime(FakeChatModel model, Duration shutdownTimeout) {
        return newRuntime(model, Buzhou.inMemoryStores(), shutdownTimeout);
    }

    private static DefaultAgentRuntime newRuntime(FakeChatModel model, BuzhouStores stores,
                                                  Duration shutdownTimeout) {
        return new DefaultAgentRuntime(model, stores, new HarnessAssembler(),
                RuntimeConfig.defaults(), null, null, shutdownTimeout);
    }

    /** 慢工具：开跑即 latch 通知 → 睡 {@link #TOOL_MILLIS} → 正常返回（模拟长工具批）。 */
    private static ToolCallback slowTool(String name, CountDownLatch started,
                                         AtomicReference<String> resultSink) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                return call(toolInput);
            }

            @Override
            public String call(String toolInput) {
                started.countDown();
                try {
                    Thread.sleep(TOOL_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return name + "-interrupted";
                }
                resultSink.set(name + "-done");
                return name + "-done";
            }
        };
    }

    /** 挂起工具：可中断 sleep 指定时长（预算内必不完成 → 触发硬截断路径）。 */
    private static ToolCallback hangTool(String name, CountDownLatch started, long millis) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                return call(toolInput);
            }

            @Override
            public String call(String toolInput) {
                started.countDown();
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return name + "-interrupted";
                }
                return name + "-done";
            }
        };
    }
}
