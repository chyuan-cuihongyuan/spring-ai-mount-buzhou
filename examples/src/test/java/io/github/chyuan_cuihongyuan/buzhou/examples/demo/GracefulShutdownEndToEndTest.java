package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.AgentRuntimeLifecycle;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAssemblyCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionResourceCustomizer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.TestDoubleChatModel;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-30 / spec 13 §core-1（优雅停机与生命周期）端到端（FakeChatModel 驱动）：
 *
 * <ul>
 *   <li><b>优雅停机</b>：会话在途（长工具）→ lifecycle.stop() → 在途 Turn 完成当前工具批后
 *       收尾（AFTER_CURRENT_TURN 不打断）、资源注册表清空（探针资源被关）、会话关闭、
 *       排空完成后 callback 被调、无异常外溢（chat 正常返回最终回复）；</li>
 *   <li><b>流式取消</b>：订阅者 cancel 后 span/记账收尾仍发生——observer 收到 Turn 终结事件
 *       （onTurnError，CancellationException），不再泄漏无终结信号的 Turn。</li>
 * </ul>
 */
class GracefulShutdownEndToEndTest {

    private static final long LONG_TOOL_MILLIS = 400L;

    @Test
    void gracefulShutdownDrainsInFlightTurnAndClearsResources() throws Exception {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("long_tool", "{}"),
                ScriptStep.text("本轮已收尾"));
        FakeModelGuard.requireTestDouble(model);

        CountDownLatch toolStarted = new CountDownLatch(1);
        AtomicBoolean probeResourceClosed = new AtomicBoolean();
        SessionResourceCustomizer probe = (registry, appId, agentName, sessionId) ->
                registry.register("e2e-probe", () -> probeResourceClosed.set(true));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.merge(RuntimeConfig.defaults(),
                        RuntimeConfig.sessionCustomizers(List.of(probe))),
                null, null, Duration.ofSeconds(5), longTool("long_tool", toolStarted));

        AgentSession session = runtime.spawn("e2e-app", "agent", "shutdown-e2e");
        // 在途 Turn：长工具执行中触发停机（另一线程跑 chat）
        CompletableFuture<String> reply =
                CompletableFuture.supplyAsync(() -> session.chat("开始长任务"));
        assertThat(toolStarted.await(5, TimeUnit.SECONDS)).isTrue();

        DefaultAgentRuntime defaultRuntime = (DefaultAgentRuntime) runtime;
        AgentRuntimeLifecycle lifecycle = new AgentRuntimeLifecycle(defaultRuntime, Duration.ofSeconds(5));
        lifecycle.start();
        CountDownLatch callbackRan = new CountDownLatch(1);
        lifecycle.stop(callbackRan::countDown);

        // 排空完成 → callback 被调；在途 Turn 完成当前工具批后自然收尾、无异常外溢
        assertThat(callbackRan.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(reply.get(5, TimeUnit.SECONDS)).isEqualTo("本轮已收尾");
        assertThat(lifecycle.isRunning()).isFalse();
        // 资源注册表清空（探针资源已关闭）、会话已被停机序列关闭
        assertThat(probeResourceClosed.get()).isTrue();
        assertThat(defaultRuntime.activeSessionCount()).isZero();
        assertThatThrownBy(() -> session.chat("停机后"))
                .hasMessageContaining("already closed");
        // 停机期新会话被拒（SHUTDOWN_INTERRUPTED）
        assertThatThrownBy(() -> runtime.spawn("e2e-app", "agent", "after-stop"))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("拒绝创建新会话");
    }

    @Test
    void streamSubscriberCancelStillFinalizesTurnAccounting() throws Exception {
        NeverCompletingStreamModel model = new NeverCompletingStreamModel();
        FakeModelGuard.requireTestDouble(model);

        List<String> turnEvents = new CopyOnWriteArrayList<>();
        SessionAssemblyCustomizer observerCustomizer = ctx ->
                ctx.addObserver(new SessionObserver() {
                    @Override
                    public void onTurnStart(int turnSeq, String userInput) {
                        turnEvents.add("start#" + turnSeq);
                    }

                    @Override
                    public void onTurnEnd(int turnSeq, String finalReply) {
                        turnEvents.add("end#" + turnSeq);
                    }

                    @Override
                    public void onTurnError(int turnSeq, Throwable error) {
                        turnEvents.add("error#" + turnSeq + ":" + error.getClass().getSimpleName());
                    }
                });
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.merge(RuntimeConfig.defaults(),
                        RuntimeConfig.assemblyCustomizers(List.of(observerCustomizer))));
        AgentSession session = runtime.spawn("e2e-app", "agent", "stream-cancel");

        // 收到首个信号后即取消订阅（上游永不完成 → 必然走 CANCEL 终结路径）
        ChatResponse first = session.stream("流式输出").next().block(Duration.ofSeconds(5));
        assertThat(first.getResult().getOutput().getText()).isEqualTo("部分输出");

        // cancel 后 doFinally 收尾仍发生：Turn 终结事件（onTurnError/CancellationException）
        // 在有限时间内到达（不再泄漏无终结信号的 Turn）；正常完成路径（onTurnEnd）不触发。
        assertThat(turnEvents).contains("start#1");
        // 轮询上限 5s（契约测试真实时序风格；实际应在 block 返回后的毫秒级到达）
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!turnEvents.stream().anyMatch(e -> e.startsWith("error#1"))
                && System.nanoTime() < deadline) {
            Thread.sleep(50L);
        }
        assertThat(turnEvents).anySatisfy(e -> assertThat(e).startsWith("error#1:"));
        assertThat(turnEvents).noneMatch(e -> e.startsWith("end#"));
        session.close();
    }

    /** 长工具：开跑即 latch 通知 → 睡 400ms → 正常返回（停机排空的对象）。 */
    private static org.springframework.ai.tool.ToolCallback longTool(String name, CountDownLatch started) {
        return new org.springframework.ai.tool.ToolCallback() {
            @Override
            public org.springframework.ai.tool.definition.ToolDefinition getToolDefinition() {
                return org.springframework.ai.tool.definition.ToolDefinition.builder()
                        .name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return call(toolInput, null);
            }

            @Override
            public String call(String toolInput, org.springframework.ai.chat.model.ToolContext toolContext) {
                started.countDown();
                try {
                    Thread.sleep(LONG_TOOL_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return name + "-interrupted";
                }
                return name + "-done";
            }
        };
    }

    /**
     * 流式测试替身：{@code call} 给最终文本；{@code stream} 发出一个信号后<b>永不完成</b>
     * （订阅者取消是唯一终结路径——驱动 CANCEL 收尾语义）。
     */
    static final class NeverCompletingStreamModel implements TestDoubleChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            return textResponse("最终回复");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.concat(Flux.just(textResponse("部分输出")), Flux.never());
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        private static ChatResponse textResponse(String text) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }
    }
}
