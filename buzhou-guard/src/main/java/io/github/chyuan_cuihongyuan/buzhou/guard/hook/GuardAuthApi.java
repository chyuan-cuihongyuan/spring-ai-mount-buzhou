package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.AuthTtl;
import io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint.ArgumentFingerprint;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 业务侧（REST）授权写回 API（spec 07 HITL 全流程步骤 4）。
 *
 * <p>用户选择 → 业务侧调 {@link #approve} 把授权写回 {@link SessionStateStore} →
 * 业务重发同一输入，守卫查 state 命中指纹 → 放行。
 *
 * <p>授权 state key = {@code auth.{toolName}.{fingerprint}}；value 为 spec 07 存储节的
 * 六字段 JSON：{@code {optionId, value, input, grantedTurn, ttlMode, consumed}}。
 * 跨实例：state 走持久化 store，任意实例续跑可放行。
 *
 * <p>审计：授权记 {@code guard.auth.granted}、撤销记 {@code guard.auth.revoked}、
 * 回写记 {@code buzhou.guard.confirmation.response}——双投监听器与 {@link ObservabilityStore}（可空）。
 *
 * <p>本类是无状态服务：构造注入 SessionStateStore，可被多会话共用。
 */
public class GuardAuthApi {

    /** 授权审计事件（spec 07 事件类型清单）。 */
    public static final String EVENT_AUTH_GRANTED = "guard.auth.granted";
    public static final String EVENT_AUTH_REVOKED = "guard.auth.revoked";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SessionStateStore stateStore;
    private final AuthTtl authTtl;
    private final ObservabilityStore observabilityStore;
    private final List<Consumer<SessionEvent>> listeners = new CopyOnWriteArrayList<>();

    public GuardAuthApi(SessionStateStore stateStore, AuthTtl authTtl,
                        ObservabilityStore observabilityStore) {
        this.stateStore = stateStore;
        this.authTtl = authTtl == null ? AuthTtl.ONCE : authTtl;
        this.observabilityStore = observabilityStore;
    }

    /** 注册事件监听器（审计用：授权/拒绝/撤销事件透出）。 */
    public void addListener(Consumer<SessionEvent> listener) {
        listeners.add(listener);
    }

    /**
     * 写回授权（用户确认 approve）。
     *
     * @param sessionId 会话 id
     * @param toolName  工具名
     * @param arguments 危险参数（用于计算指纹）
     * @param optionId  用户选择的选项 id（如 approve/approval）
     * @param input     可选文本输入（approval 审批人等）
     */
    public void approve(String sessionId, String toolName, Map<String, Object> arguments,
                        String optionId, String input) {
        approve(sessionId, toolName, arguments, optionId, input, 0);
    }

    /** 写回授权（含授权发生轮次，取自确认请求事件的 {@code turn} 字段）。 */
    public void approve(String sessionId, String toolName, Map<String, Object> arguments,
                        String optionId, String input, int grantedTurn) {
        String fingerprint = ArgumentFingerprint.fingerprint(arguments);
        String authKey = ArgumentFingerprint.authKey(toolName, fingerprint);
        String normalizedOption = optionId == null ? "approve" : optionId;
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("optionId", normalizedOption);
        record.put("value", normalizedOption);
        record.put("input", input);
        record.put("grantedTurn", grantedTurn);
        record.put("ttlMode", authTtl == AuthTtl.ONCE ? "once" : "session");
        record.put("consumed", false);
        stateStore.put(sessionId, new StateEntry(authKey, toJson(record), "guard-auth",
                grantedTurn, null, Instant.now()));
        emitResponse(sessionId, toolName, fingerprint, optionId, input, "approved");
        emitAudit(EVENT_AUTH_GRANTED, sessionId, toolName, fingerprint, Map.of(
                "optionId", normalizedOption,
                "ttlMode", record.get("ttlMode"),
                "grantedTurn", grantedTurn));
    }

    /** 写回授权的便捷重载（默认 approve 选项、无输入）。 */
    public void approve(String sessionId, String toolName, Map<String, Object> arguments) {
        approve(sessionId, toolName, arguments, "approve", null);
    }

    /**
     * 拒绝（不写授权，保持未授权状态）。
     */
    public void reject(String sessionId, String toolName, Map<String, Object> arguments,
                       String optionId, String input) {
        String fingerprint = ArgumentFingerprint.fingerprint(arguments);
        // 不写 auth key（保持未授权）；仅记审计事件
        emitResponse(sessionId, toolName, fingerprint, optionId, input, "rejected");
    }

    /** 撤销已存在的授权（删除 auth key），记 {@code guard.auth.revoked} 审计事件。 */
    public void revoke(String sessionId, String toolName, Map<String, Object> arguments) {
        String fingerprint = ArgumentFingerprint.fingerprint(arguments);
        String authKey = ArgumentFingerprint.authKey(toolName, fingerprint);
        stateStore.delete(sessionId, authKey);
        emitAudit(EVENT_AUTH_REVOKED, sessionId, toolName, fingerprint, Map.of());
    }

    /** 查询某工具+参数是否已授权（调试/前端展示用）。 */
    public boolean isAuthorized(String sessionId, String toolName, Map<String, Object> arguments) {
        String fingerprint = ArgumentFingerprint.fingerprint(arguments);
        String authKey = ArgumentFingerprint.authKey(toolName, fingerprint);
        return stateStore.get(sessionId, authKey).isPresent();
    }

    private void emitResponse(String sessionId, String toolName, String fingerprint,
                              String optionId, String input, String decision) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("toolName", toolName);
        payload.put("fingerprint", fingerprint);
        payload.put("optionId", optionId);
        payload.put("value", optionId);
        payload.put("decision", decision);
        if (input != null) {
            payload.put("input", input);
        }
        SessionEvent event = new SessionEvent(
                DangerousToolGuardHook.EVENT_CONFIRMATION_RESPONSE, payload, Instant.now());
        listeners.forEach(l -> l.accept(event));
    }

    /** 审计事件双投：监听器 + ObservabilityStore（spec 07：授权/撤销均记 Event）。 */
    private void emitAudit(String type, String sessionId, String toolName, String fingerprint,
                           Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("toolName", toolName);
        payload.put("param.fingerprint", fingerprint);
        payload.putAll(extra);
        SessionEvent event = new SessionEvent(type, payload, Instant.now());
        listeners.forEach(l -> l.accept(event));
        if (observabilityStore != null) {
            try {
                observabilityStore.saveEvents(List.of(new EventRecord(
                        UUID.randomUUID().toString(), null, sessionId, type, Instant.now(), payload)));
            } catch (RuntimeException ignored) {
                // 观测落库失败不阻断授权主链路
            }
        }
    }

    private static String toJson(Map<String, Object> record) {
        try {
            return MAPPER.writeValueAsString(record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("auth record serialization failed", e);
        }
    }
}
