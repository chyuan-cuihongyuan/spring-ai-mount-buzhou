package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 40 §B / T152 / impl-123：会话单飞闸——同会话并发第二个轮次快速失败
 * （TURN_IN_FLIGHT / NON_RETRYABLE）；轮次终结（成功或异常收尾）后闸释放。
 */
class SingleFlightGateTest {

    @Test
    void secondTurnRejectedWhileFirstInFlight_thenGateReleasedOnCompletion() throws Exception {
        CountDownLatch toolStarted = new CountDownLatch(1);
        AtomicReference<String> toolResult = new AtomicReference<>("未执行");
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("slow_tool", "{}"),
                ScriptStep.text("第一轮完成"));
        BuzhouStores stores = Buzhou.inMemoryStores();
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(model, stores,
                new HarnessAssembler(), io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                null, null, java.time.Duration.ofSeconds(30),
                slowTool("slow_tool", toolStarted, toolResult));

        DefaultAgentSession session = (DefaultAgentSession) runtime.spawn("app", "agent", "singleflight");
        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> session.chat("开始"));
        assertThat(toolStarted.await(5, TimeUnit.SECONDS)).isTrue();

        // 在途期间：并发第二个轮次（chat 与 stream 入口）确定拒绝，而非未定义并发
        assertThatThrownBy(() -> session.chat("并发轮次"))
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.TURN_IN_FLIGHT))
                .hasMessageContaining("singleflight");
        assertThatThrownBy(() -> session.stream("并发流式").blockFirst())
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.TURN_IN_FLIGHT));
        assertThat(session.inFlightTurns()).isEqualTo(1);

        // 第一轮终结后闸释放，下一轮正常
        assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("第一轮完成");
        assertThat(session.inFlightTurns()).isZero();
        assertThat(session.chat("第二轮")).isEqualTo("第一轮完成");

        runtime.close();
    }

    @Test
    void gateReleasedWhenTurnFails() throws Exception {
        // 模型侧直接抛错：Turn 异常收尾也必须释放闸（finally 语义）
        io.github.chyuan_cuihongyuan.buzhou.core.testsupport.TestDoubleChatModel boom =
                prompt -> {
                    throw new IllegalStateException("模型瞬态故障");
                };
        DefaultAgentRuntime runtime = new DefaultAgentRuntime(boom, Buzhou.inMemoryStores(),
                new HarnessAssembler(), io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                null, null, java.time.Duration.ofSeconds(30));

        DefaultAgentSession session = (DefaultAgentSession) runtime.spawn("app", "agent", "singleflight-fail");
        assertThatThrownBy(() -> session.chat("第一轮")).hasMessageContaining("模型瞬态故障");
        assertThat(session.inFlightTurns()).isZero();

        // 闸已释放：后续轮次不被前次失败卡死（新会话正常往返）
        FakeChatModel second = FakeChatModel.script(ScriptStep.text("恢复"));
        DefaultAgentRuntime runtime2 = new DefaultAgentRuntime(second, Buzhou.inMemoryStores(),
                new HarnessAssembler(), io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults(),
                null, null, java.time.Duration.ofSeconds(30));
        DefaultAgentSession session2 = (DefaultAgentSession) runtime2.spawn("app", "agent", "singleflight-recover");
        assertThat(session2.chat("恢复轮")).isEqualTo("恢复");

        runtime.close();
        runtime2.close();
    }

    static ToolCallback slowTool(String name, CountDownLatch started, AtomicReference<String> result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("slow tool")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                started.countDown();
                try {
                    Thread.sleep(300L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                result.set(name + "-done");
                return result.get();
            }
        };
    }
}
