package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnLoopContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnLoopPolicy;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolCallingManager;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 有界 Turn 的 {@link ToolCallingAdvisor} 扩展（wayfinder T17 / docs/spec/11 core）：
 * 在 Spring AI 工具调用循环的「模型响应后、工具执行前」缝隙裁决停止条件；
 * 命中时把该工具调用响应替换为优雅最终回复（非工具调用响应 → 循环自然退出），
 * 模型陷入工具死循环也能在预算内终止并优雅收尾。
 *
 * <p>每个会话一个实例（{@code HarnessAssembler.assemble} 每次装配新建），轮次计数与
 * 起始时间为实例状态，在 {@code doInitializeLoop*}（每次 advise 调用开头）复位；
 * 同一会话顺序的 chat/stream 调用安全，并发的同会话多重 advise 属未定义使用。
 */
public class BoundedToolCallingAdvisor extends ToolCallingAdvisor {

    private final TurnLoopPolicy policy;
    private final String sessionId;
    private final String agentName;
    private final Consumer<SessionEvent> eventSink;
    private final ToolCallingManager toolCallingManager;
    private final AtomicInteger executedToolRounds = new AtomicInteger();
    private final AtomicReference<Instant> loopStartedAt = new AtomicReference<>();

    public BoundedToolCallingAdvisor(ToolCallingManager toolCallingManager,
                                     TurnLoopPolicy policy,
                                     String sessionId,
                                     String agentName,
                                     Consumer<SessionEvent> eventSink) {
        // 与 ToolCallingAdvisor.builder().toolCallingManager(tm).build() 的默认保持一致：
        // 默认 eligibility checker、默认 order（最外层）、内部会话历史启用。
        super(toolCallingManager, DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER, DEFAULT_ORDER, true);
        this.policy = policy == null ? TurnLoopPolicy.defaults() : policy;
        this.sessionId = sessionId;
        this.agentName = agentName;
        this.eventSink = eventSink;
        this.toolCallingManager = toolCallingManager;
    }

    @Override
    protected ChatClientRequest doInitializeLoop(ChatClientRequest request, CallAdvisorChain chain) {
        executedToolRounds.set(0);
        loopStartedAt.set(Instant.now());
        resetValidationBudget();
        return super.doInitializeLoop(request, chain);
    }

    @Override
    protected ChatClientRequest doInitializeLoopStream(ChatClientRequest request, StreamAdvisorChain chain) {
        executedToolRounds.set(0);
        loopStartedAt.set(Instant.now());
        resetValidationBudget();
        return super.doInitializeLoopStream(request, chain);
    }

    @Override
    protected ChatClientResponse doAfterCall(ChatClientResponse response, CallAdvisorChain chain) {
        ChatClientResponse result = super.doAfterCall(response, chain);
        return guardBeforeToolExecution(result);
    }

    @Override
    protected ChatClientResponse doAfterStream(ChatClientResponse response, StreamAdvisorChain chain) {
        ChatClientResponse result = super.doAfterStream(response, chain);
        return guardBeforeToolExecution(result);
    }

    /** 模型刚给出工具调用响应、工具尚未执行：在此裁决是否放行本轮。 */
    private ChatClientResponse guardBeforeToolExecution(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) {
            return response;
        }
        if (!DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER.isToolCallResponse(response.chatResponse())) {
            return response; // 普通最终回复，无需护栏
        }
        int nextRound = executedToolRounds.get() + 1;
        TurnLoopContext ctx = snapshot(nextRound);
        // impl-04 / T30：参数校验重试预算——校验失败累计超过预算（且模型仍在要工具）→
        // REASK_FAILED 优雅收尾（与轮数上界独立扣减的第三条护栏）
        int validationFailures = validationFailures();
        if (validationFailures > policy.effectiveRetryBudget()) {
            emitReaskFailed(ctx, validationFailures);
            String finalText = policy.reaskFailedFinal(ctx, validationFailures);
            ChatResponse graceful = new ChatResponse(
                    List.of(new Generation(new AssistantMessage(finalText))));
            return new ChatClientResponse(graceful, response.context());
        }
        if (!policy.shouldStop(ctx)) {
            executedToolRounds.incrementAndGet();
            return response;
        }
        // 命中停止条件：本轮工具不执行，替换为优雅最终回复（非工具调用响应 → 循环退出）
        emitBounded(ctx);
        String finalText = policy.gracefulFinal(ctx);
        ChatResponse graceful = new ChatResponse(List.of(new Generation(new AssistantMessage(finalText))));
        return new ChatClientResponse(graceful, response.context());
    }

    /** impl-04：Turn 开始复位校验失败计数（仅 Harness 管理器持有计数器）。 */
    private void resetValidationBudget() {
        if (toolCallingManager instanceof io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager harness) {
            harness.resetValidationFailures();
        }
    }

    private int validationFailures() {
        if (toolCallingManager instanceof io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager harness) {
            return harness.validationFailures();
        }
        return 0;
    }

    private void emitReaskFailed(TurnLoopContext ctx, int validationFailures) {
        if (eventSink == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", ctx.sessionId());
        payload.put("agentName", ctx.agentName());
        payload.put("validationFailures", validationFailures);
        payload.put("retryBudget", policy.effectiveRetryBudget());
        payload.put("onFail", "REASK_FAILED");
        eventSink.accept(new SessionEvent("turn.loop.reask_failed", payload, Instant.now()));
    }

    private TurnLoopContext snapshot(int nextRound) {
        Instant startedAt = loopStartedAt.get();
        Instant now = Instant.now();
        Duration elapsed = startedAt == null ? Duration.ZERO : Duration.between(startedAt, now);
        int executed = nextRound - 1;
        return new TurnLoopContext() {
            @Override
            public int executedToolRounds() {
                return executed;
            }

            @Override
            public int nextToolRound() {
                return nextRound;
            }

            @Override
            public Duration elapsed() {
                return elapsed;
            }

            @Override
            public String sessionId() {
                return sessionId;
            }

            @Override
            public String agentName() {
                return agentName;
            }
        };
    }

    private void emitBounded(TurnLoopContext ctx) {
        if (eventSink == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", ctx.sessionId());
        payload.put("agentName", ctx.agentName());
        payload.put("executedToolRounds", ctx.executedToolRounds());
        payload.put("nextToolRound", ctx.nextToolRound());
        if (policy.maxToolRounds() != null) {
            payload.put("maxToolRounds", policy.maxToolRounds());
        }
        if (policy.loopTimeout() != null) {
            payload.put("loopTimeoutMillis", policy.loopTimeout().toMillis());
        }
        eventSink.accept(new SessionEvent("turn.loop.bounded", payload, Instant.now()));
    }
}
