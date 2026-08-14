package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnLoopPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FaultInjectingToolCallback;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.TestDoubleChatModel;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * impl-28（spec 13 §core-2）端到端：Turn Deadline 挂起免疫——
 *
 * <ul>
 *   <li>不响应中断的挂死工具（{@code hangForever}）在配置 turnDeadline 后<b>有限时间内</b>
 *       得到 TIMEOUT 超时反馈，模型据此收尾，Turn 正常结束、会话不僵死；</li>
 *   <li>挂死的<b>模型调用</b>由会话层兜底（预算 + 收尾宽限）硬截断，上抛结构化 TIMEOUT 异常，
 *       会话随后仍可继续对话；</li>
 *   <li>无 Deadline 的既有行为不回归（默认配置下正常工具轮照旧）。</li>
 * </ul>
 *
 * <p>整体时长上限用 CompletableFuture + 超时断言：任何路径挂死都表现为测试失败而非测试挂起。
 */
class DeadlineHangImmunityEndToEndTest {

    /** 整体时长守卫（秒）：任何 Turn 超过此值即测试失败（防挂死守卫，非业务断言）。 */
    private static final long TURN_GUARD_SECONDS = 20L;
    /** 预算内收尾断言上限（毫秒）：预算 500ms + 5s 收尾宽限的宽松上界。 */
    private static final long GRACEFUL_BOUND_MILLIS = 5_000L;
    /** 模型挂死硬截断断言上限（毫秒）：预算 300ms + 5s 收尾宽限 + 宽裕。 */
    private static final long HARD_CUT_BOUND_MILLIS = 10_000L;

    private static final String GRACEFUL_FINAL = "工具挂死已被超时兜底，基于超时反馈收尾";
    private static final String RECOVERED_REPLY = "恢复后的正常回复";

    @Test
    void hangForeverToolGetsTimeoutFeedbackAndTurnCompletesWithinDeadline() throws Exception {
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall(FaultInjectingToolCallback.DEFAULT_TOOL_NAME, "{}"),
                ScriptStep.text(GRACEFUL_FINAL));
        FakeModelGuard.requireTestDouble(model);
        // 预算 500ms（< 默认单工具 60s 超时）：Deadline 是唯一的截断者
        RuntimeConfig config = RuntimeConfig.turnLoopPolicy(
                TurnLoopPolicy.of(10).withTurnDeadline(Duration.ofMillis(500L)));
        FaultInjectingToolCallback hangTool = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.hanging());
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), config, hangTool);

        AgentSession session = runtime.spawn("deadline-app", "support-agent", "hang-tool-session");
        long start = System.nanoTime();
        String reply = CompletableFuture.supplyAsync(() -> session.chat("调用会挂死的工具"))
                .get(TURN_GUARD_SECONDS, TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // 有限时间内收尾（预算 + 收尾宽限以内），且以脚本终局文本优雅结束（非崩溃、非僵死）
        assertThat(elapsedMs).isLessThan(GRACEFUL_BOUND_MILLIS);
        assertThat(reply).isEqualTo(GRACEFUL_FINAL);
        // 模型确实收到了结构化超时反馈（词汇 + 工具名 + 原入参回显）
        String feedback = toolFeedbackSeen(model);
        assertThat(feedback).contains("执行超时");
        assertThat(feedback).contains(FaultInjectingToolCallback.DEFAULT_TOOL_NAME);
        assertThat(feedback).contains("建议：");
        // 会话不僵死：同一会话可继续对话（脚本耗尽重复末步）
        assertThat(session.chat("继续")).isEqualTo(GRACEFUL_FINAL);
        session.close();
    }

    @Test
    void hungModelCallIsHardCutWithStructuredTimeoutAndSessionStaysUsable() throws Exception {
        HangFirstThenTextModel model = new HangFirstThenTextModel();
        FakeModelGuard.requireTestDouble(model);
        RuntimeConfig config = RuntimeConfig.turnLoopPolicy(
                TurnLoopPolicy.of(10).withTurnDeadline(Duration.ofMillis(300L)));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), config);

        AgentSession session = runtime.spawn("deadline-app", "support-agent", "hang-model-session");
        long start = System.nanoTime();
        Throwable thrown = catchThrowable(() -> {
            try {
                CompletableFuture.supplyAsync(() -> session.chat("模型永不返回"))
                        .get(TURN_GUARD_SECONDS, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException e) {
                // 还原被守卫 CompletableFuture 包装的业务异常（硬截断的 TIMEOUT）
                throw e.getCause() == null ? e : e.getCause();
            }
        });
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // 硬截断：预算 + 收尾宽限内上抛结构化 TIMEOUT（模型侧挂死不吞、不无限占用会话）
        assertThat(elapsedMs).isLessThan(HARD_CUT_BOUND_MILLIS);
        assertThat(thrown).isInstanceOf(BuzhouException.class);
        assertThat(((BuzhouException) thrown).errorCode()).isEqualTo(ErrorCode.TIMEOUT);
        // 会话不僵死：挂死轮次被丢弃后，同一会话可继续完成正常对话
        assertThat(session.chat("恢复正常")).isEqualTo(RECOVERED_REPLY);
        session.close();
    }

    @Test
    void noDeadlineKeepsLegacyBehaviorEndToEnd() throws Exception {
        // 回归：默认配置（无 turnDeadline/loopTimeout）下正常工具轮照旧——既有行为不受影响
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("healthy_tool", "{}"),
                ScriptStep.text("正常完成"));
        FakeModelGuard.requireTestDouble(model);
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.defaults(), fixedTool("healthy_tool"));

        AgentSession session = runtime.spawn("deadline-app", "support-agent", "legacy-session");
        String reply = CompletableFuture.supplyAsync(() -> session.chat("正常调用"))
                .get(TURN_GUARD_SECONDS, TimeUnit.SECONDS);
        session.close();

        assertThat(reply).isEqualTo("正常完成");
    }

    /** 第二次模型调用回注的末条 ToolResponseMessage = 超时反馈载体（与既有 e2e 同型断言）。 */
    private static String toolFeedbackSeen(FakeChatModel model) {
        Message last = model.seenPrompts.get(1).getInstructions().getLast();
        assertThat(last).isInstanceOf(ToolResponseMessage.class);
        return ((ToolResponseMessage) last).getResponses().getFirst().responseData();
    }

    private static ToolCallback fixedTool(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return name + "-result";
            }
        };
    }

    /** 首次调用永久挂起且不响应中断（模型侧挂死样本）；后续调用恢复为正常文本回复。 */
    static final class HangFirstThenTextModel implements TestDoubleChatModel {

        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            if (calls.incrementAndGet() == 1) {
                hangIgnoringInterruption();
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(RECOVERED_REPLY))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        private static void hangIgnoringInterruption() {
            while (true) {
                try {
                    Thread.sleep(1_000L);
                } catch (InterruptedException e) {
                    // 模拟不可中断的挂死模型调用（兜底取消尽力而为，会话侧必须靠预算硬截断）
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
