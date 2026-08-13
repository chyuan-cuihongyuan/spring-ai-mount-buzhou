package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

import java.util.Map;
import java.util.Set;

/**
 * 审计链事件收集器（wayfinder2 impl-22 / T50）：把护栏/记忆裁决事件追加进
 * {@link AuditChain}——覆盖 HITL 裁决（guard.*）、FIDES 写门（guard.taint.*）、
 * 自愈记忆修订（memory.revise 语义事件经 state 台账、此处采 guard 域事件）、悬空修复等。
 * 经 {@code SpawnOptions.withListeners(collector)} 挂入会话。
 */
public final class AuditTrailCollector implements SessionEventListener {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> AUDITED_TYPES = Set.of(
            "guard.tool.blocked", "guard.auth.granted", "guard.auth.consumed",
            "guard.confirmation.requested", "guard.confirmation.response",
            "guard.taint.marked", "guard.taint.blocked",
            "guard.canary.leaked", "guard.canary.variant.blocked",
            "session.cancelled", "turn.loop.bounded", "turn.loop.reask_failed",
            "turn.loop.cancelled", "dangling.repaired", "memory.fact.reconciled");

    private final AuditChain chain;

    public AuditTrailCollector(AuditChain chain) {
        this.chain = chain;
    }

    public AuditChain chain() {
        return chain;
    }

    @Override
    public void onEvent(SessionEvent event) {
        if (event == null || !AUDITED_TYPES.contains(event.type())) {
            return;
        }
        chain.append(event.payload().getOrDefault("sessionId", "").toString(),
                event.type(), payloadJson(event.payload()), outcomeOf(event.type()));
    }

    private static String payloadJson(Map<String, Object> payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return String.valueOf(payload);
        }
    }

    private static String outcomeOf(String eventType) {
        if (eventType.endsWith(".blocked") || eventType.contains(".leaked")) {
            return "BLOCKED";
        }
        if (eventType.contains(".granted") || eventType.contains(".response")) {
            return "ALLOWED";
        }
        return "RECORDED";
    }
}
