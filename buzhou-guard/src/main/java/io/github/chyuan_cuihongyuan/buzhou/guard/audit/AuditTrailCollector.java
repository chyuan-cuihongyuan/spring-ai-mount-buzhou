package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 审计链事件收集器（wayfinder2 impl-22 / T50）：把护栏/记忆裁决事件追加进
 * {@link AuditChain}——覆盖 HITL 裁决（guard.*）、FIDES 写门（guard.taint.*）、
 * 自愈记忆修订（memory.revise 语义事件经 state 台账、此处采 guard 域事件）、悬空修复等。
 * 经 {@code SpawnOptions.withListeners(collector)} 挂入会话。
 *
 * <p>impl-39 / spec 13 §T64 增强：每条记录即时落 {@link AuditRecordStore}（append-only
 * 持久化，失败明示 ERROR 不静默）；{@code session.closed} 收尾发布该会话
 * {@code sessionHash}（以审计记录形态入链留档，随事件总线可见）。
 */
public final class AuditTrailCollector implements SessionEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(AuditTrailCollector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Set<String> AUDITED_TYPES = Set.of(
            "guard.tool.blocked", "guard.auth.granted", "guard.auth.consumed",
            "guard.confirmation.requested", "guard.confirmation.response",
            "guard.taint.marked", "guard.taint.blocked",
            "guard.canary.leaked", "guard.canary.variant.blocked",
            "session.cancelled", "turn.loop.bounded", "turn.loop.reask_failed",
            "turn.loop.cancelled", "dangling.repaired", "memory.fact.reconciled");

    private final AuditChain chain;
    private final AuditRecordStore store;
    private final Set<String> openSessions = new LinkedHashSet<>();

    public AuditTrailCollector(AuditChain chain) {
        this(chain, null);
    }

    public AuditTrailCollector(AuditChain chain, AuditRecordStore store) {
        this.chain = chain;
        this.store = store;
    }

    public AuditChain chain() {
        return chain;
    }

    @Override
    public void onEvent(SessionEvent event) {
        if (event == null) {
            return;
        }
        if ("session.closed".equals(event.type())) {
            closeOpenSessions();
            return;
        }
        if (!AUDITED_TYPES.contains(event.type())) {
            return;
        }
        String sessionId = String.valueOf(event.payload().getOrDefault("sessionId", ""));
        appendRecord(sessionId, event.type(), payloadJson(event.payload()),
                outcomeOf(event.type()));
        if (!sessionId.isBlank()) {
            openSessions.add(sessionId);
        }
    }

    private synchronized void closeOpenSessions() {
        // session.closed 不携带 sessionId（DefaultAgentSession 收尾广播）：
        // 收尾当前仍开着的全部会话（单 runtime 一会话为主流形态）
        for (String sessionId : openSessions) {
            String sessionHash = chain.sessionHash(sessionId);
            appendRecord(sessionId, "audit.session.closed",
                    "{\"sessionHash\":\"" + sessionHash + "\"}", "RECORDED");
        }
        openSessions.clear();
    }

    private void appendRecord(String sessionId, String actionType, String actionDetail,
            String outcome) {
        AgentAuditRecord record = chain.append(sessionId, actionType, actionDetail, outcome);
        if (store != null) {
            try {
                store.append(record);
            } catch (RuntimeException e) {
                // 审计持久化失败明示（不静默吞审计）；链本身仍在内存可验
                LOG.error("buzhou-guard 审计记录持久化失败 actionType={} recordId={}",
                        actionType, record.recordId(), e);
            }
        }
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
