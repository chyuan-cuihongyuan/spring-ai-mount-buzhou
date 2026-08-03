package io.github.chyuan_cuihongyuan.buzhou.observability.advisor;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanRecorder;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContextCarrier;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 会话作用域的可观测状态：实现 {@link ObservabilityAdvisor.ObservabilitySessionHooks}（供 advisor 与
 * 工具回调共享 session span / turn seq / iteration）与 {@link SessionObserver}（开/关 SESSION span、
 * 记轮次、强制 flush）。
 *
 * <p>{@link SpanContextCarrier} 是 core 暴露的会话级显式载体（抗串味），本类负责把 turn/iteration
 * 进展同步给 carrier，并把 SESSION span 写回 carrier 供子 span 作 parent。
 */
public class ObservabilitySessionState
        implements ObservabilityAdvisor.ObservabilitySessionHooks, SessionObserver {

    private final SpanRecorder recorder;
    private final SpanContextCarrier carrier;
    private final String sessionId;
    private final String agentName;
    private final String appId;
    private final String modelName;
    private final AtomicReference<SpanContext> sessionSpanRef = new AtomicReference<>();
    private final AtomicReference<SpanContext> turnSpanRef = new AtomicReference<>();
    private final AtomicInteger iterationCounter = new AtomicInteger();
    private volatile SpanHandle sessionSpanHandle;
    private volatile SpanHandle turnSpanHandle;
    // 本轮聚合的 usage（由 ObservabilityAdvisor 在 ModelCall 关闭时写入，onTurnEnd 时读）
    private final java.util.concurrent.atomic.AtomicInteger turnPromptTokens = new AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger turnCompletionTokens = new AtomicInteger();

    public ObservabilitySessionState(SpanRecorder recorder, SpanContextCarrier carrier, String sessionId,
                                     String agentName, String appId, String modelName) {
        this.recorder = recorder;
        this.carrier = carrier;
        this.sessionId = sessionId;
        this.agentName = agentName;
        this.appId = appId;
        this.modelName = modelName;
    }

    @Override
    public void onOpen() {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("agent.name", agentName);
        attrs.put("app.id", appId);
        attrs.put("model.name", modelName);
        // SESSION span 是根（parent=null），sessionId 不能从 parent 取，必须显式传
        SpanContext sessionCtx = new SpanContext(java.util.UUID.randomUUID().toString(), sessionId, 0);
        sessionSpanHandle = recorder.openSpan(SpanKind.SESSION, "session", null, attrs, sessionCtx);
        sessionSpanRef.set(sessionSpanHandle.context());
        if (carrier != null) {
            carrier.bindSessionSpan(sessionSpanHandle.context());
        }
    }

    @Override
    public void onTurnStart(int turnSeq, String userInput) {
        SpanContext session = sessionSpanRef.get();
        if (session == null) {
            return;
        }
        iterationCounter.set(0);
        turnPromptTokens.set(0);
        turnCompletionTokens.set(0);
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("turn.seq", turnSeq);
        if (userInput != null) {
            attrs.put("user.input.preview", userInput.length() > 200
                    ? userInput.substring(0, 200) : userInput);
        }
        turnSpanHandle = recorder.openSpan(SpanKind.TURN, "turn", session, attrs,
                new SpanContext(java.util.UUID.randomUUID().toString(), sessionId, turnSeq));
        // 使用 span handle 的真实 context
        turnSpanRef.set(turnSpanHandle.context());
        if (carrier != null) {
            carrier.bindTurn(turnSpanHandle.context());
        }
    }

    @Override
    public void onTurnEnd(int turnSeq, String finalReply) {
        if (turnSpanHandle == null) {
            return;
        }
        turnSpanHandle.attribute("turn.completed", true);
        turnSpanHandle.attribute("usage.prompt_tokens", turnPromptTokens.get());
        turnSpanHandle.attribute("usage.completion_tokens", turnCompletionTokens.get());
        turnSpanHandle.attribute("iteration.count", iterationCounter.get());
        turnSpanHandle.close();
        turnSpanHandle = null;
        turnSpanRef.set(null);
    }

    /** 由 advisor 在每次 ModelCall 关闭时累加本轮 usage。 */
    void accumulateTurnUsage(Integer promptTokens, Integer completionTokens) {
        if (promptTokens != null) {
            turnPromptTokens.addAndGet(promptTokens);
        }
        if (completionTokens != null) {
            turnCompletionTokens.addAndGet(completionTokens);
        }
    }

    @Override
    public void onClose() {
        if (sessionSpanHandle != null) {
            sessionSpanHandle.close();
        }
        recorder.flush();
    }

    @Override
    public void onCancel() {
        if (sessionSpanHandle != null) {
            sessionSpanHandle.close(io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus.CANCELLED);
        }
        recorder.flush();
    }

    @Override
    public SpanContext sessionSpan() {
        return sessionSpanRef.get();
    }

    @Override
    public Integer currentTurnSeq(String sid) {
        SpanContext turn = turnSpanRef.get();
        return turn == null ? null : turn.turnSeq();
    }

    @Override
    public int nextIteration(String sid) {
        return iterationCounter.incrementAndGet();
    }

    @Override
    public void bindModelCall(SpanContext modelCall) {
        if (carrier != null && modelCall != null) {
            carrier.bindModelCall(modelCall);
        }
    }
}
