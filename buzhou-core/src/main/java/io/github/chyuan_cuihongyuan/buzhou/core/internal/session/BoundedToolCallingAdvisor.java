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
    /** impl-33：租约哨兵（null = 无租约语义路径，如 Buzhou.enhance）。 */
    private final SessionLeaseGuard leaseGuard;
    private final AtomicInteger executedToolRounds = new AtomicInteger();
    private final AtomicReference<Instant> loopStartedAt = new AtomicReference<>();

    public BoundedToolCallingAdvisor(ToolCallingManager toolCallingManager,
                                     TurnLoopPolicy policy,
                                     String sessionId,
                                     String agentName,
                                     Consumer<SessionEvent> eventSink) {
        this(toolCallingManager, policy, sessionId, agentName, eventSink, null);
    }

    public BoundedToolCallingAdvisor(ToolCallingManager toolCallingManager,
                                     TurnLoopPolicy policy,
                                     String sessionId,
                                     String agentName,
                                     Consumer<SessionEvent> eventSink,
                                     SessionLeaseGuard leaseGuard) {
        // 与 ToolCallingAdvisor.builder().toolCallingManager(tm).build() 的默认保持一致：
        // 默认 eligibility checker、默认 order（最外层）、内部会话历史启用。
        super(toolCallingManager, DEFAULT_TOOL_EXECUTION_ELIGIBILITY_CHECKER, DEFAULT_ORDER, true);
        this.policy = policy == null ? TurnLoopPolicy.defaults() : policy;
        this.sessionId = sessionId;
        this.agentName = agentName;
        this.eventSink = eventSink;
        this.toolCallingManager = toolCallingManager;
        this.leaseGuard = leaseGuard;
    }

    @Override
    protected ChatClientRequest doInitializeLoop(ChatClientRequest request, CallAdvisorChain chain) {
        executedToolRounds.set(0);
        loopStartedAt.set(Instant.now());
        resetValidationBudget();
        clearPendingCancel();
        beginTurnDeadline();
        return super.doInitializeLoop(request, chain);
    }

    @Override
    protected ChatClientRequest doInitializeLoopStream(ChatClientRequest request, StreamAdvisorChain chain) {
        executedToolRounds.set(0);
        loopStartedAt.set(Instant.now());
        resetValidationBudget();
        clearPendingCancel();
        beginTurnDeadline();
        return super.doInitializeLoopStream(request, chain);
    }

    /**
     * impl-33 / spec 13 §core-3：Turn 循环<b>每轮开始</b>（工具结果落库前、本轮模型调用前）的
     * 租约裁决——先 fence（{@code inspect} 校验 fencingToken 仍持有：双主窗口零写入，上一轮
     * 在飞工具结果在此被丢弃、不入 history），剩余租期 &lt; TTL/3 时续租；租约已被 steal /
     * 过期不可再取即抛 {@code LeaseLostException}（经模型调用链上抛，由会话层按 LeaseLost
     * 语义中止 Turn）。此缝位于 Spring AI 内部工具循环（adviseCall/adviseStream 每轮
     * {@code doBeforeCall/doBeforeStream}），是唯一先于 {@code BuzhouMemoryAdvisor} 落库的
     * 轮间挂点——会话层只见整次模型调用，无轮可见性。
     */
    @Override
    protected ChatClientRequest doBeforeCall(ChatClientRequest request, CallAdvisorChain chain) {
        renewLeaseBeforeRound();
        return super.doBeforeCall(request, chain);
    }

    @Override
    protected ChatClientRequest doBeforeStream(ChatClientRequest request, StreamAdvisorChain chain) {
        renewLeaseBeforeRound();
        return super.doBeforeStream(request, chain);
    }

    private void renewLeaseBeforeRound() {
        if (leaseGuard != null) {
            leaseGuard.beforeRound();
        }
    }

    /**
     * impl-28 / spec 13 §core-2：Turn 开始把有效 Deadline（min(turnDeadline, loopTimeout)，
     * 以本时刻为起点）交给工具管理器——派发/组锁/许可/外层 join 各等待点按剩余时间限时化，
     * 不响应中断的挂死工具无法拖死会话。未配置预算时置 none 哨兵（既有无限等待行为）。
     */
    private void beginTurnDeadline() {
        if (toolCallingManager instanceof io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager harness) {
            harness.beginTurn(policy.effectiveTurnDeadline());
        }
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
        // impl-05 / T31：取消护栏——IMMEDIATE / AFTER_CURRENT_TOOLS 命中时本轮工具不执行，
        // 替换为优雅取消收尾（在飞工具已被中断或已自然完成）；AFTER_CURRENT_TURN 不打断本轮
        io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode cancelMode = pendingCancel();
        if (cancelMode == io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode.IMMEDIATE
                || cancelMode == io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode.AFTER_CURRENT_TOOLS) {
            emitCancelled(ctx, cancelMode);
            ChatResponse graceful = new ChatResponse(List.of(new Generation(
                    new AssistantMessage(cancelledFinal(cancelMode)))));
            return new ChatClientResponse(graceful, response.context());
        }
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

    /** impl-05：Turn 开始清零取消标记（空闲期取消不影响下一 Turn）。 */
    private void clearPendingCancel() {
        if (toolCallingManager instanceof io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager harness) {
            harness.clearPendingCancel();
        }
    }

    private io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode pendingCancel() {
        if (toolCallingManager instanceof io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager harness) {
            return harness.pendingCancel();
        }
        return null;
    }

    /** impl-05：取消收尾文案（按档位如实描述语义）。 */
    private static String cancelledFinal(
            io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode mode) {
        return switch (mode) {
            case IMMEDIATE -> "本次请求已按「立即」模式取消：在飞工具调用被中断、在途结果已丢弃，"
                    + "未产生半成品更新。如需继续请重新发起请求。";
            case AFTER_CURRENT_TOOLS -> "本次请求已按「当前工具批后」模式取消：已启动的工具已执行完成，"
                    + "此后不再进入新的工具调用轮次。已获得的结果可用于回答；如需继续请重新发起。";
            default -> "本次请求已取消。";
        };
    }

    private void emitCancelled(TurnLoopContext ctx,
                               io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode mode) {
        if (eventSink == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("sessionId", ctx.sessionId());
        payload.put("agentName", ctx.agentName());
        payload.put("cancelMode", mode.name());
        payload.put("executedToolRounds", ctx.executedToolRounds());
        eventSink.accept(new SessionEvent("turn.loop.cancelled", payload, Instant.now()));
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
