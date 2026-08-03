package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint.ArgumentFingerprint;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 业务侧（REST）授权写回 API（spec 07 HITL 全流程步骤 4）。
 *
 * <p>用户选择 → 业务侧调 {@link #approve} 把授权写回 {@link SessionStateStore} →
 * 业务重发同一输入，守卫查 state 命中指纹 → 放行。
 *
 * <p>授权 state key = {@code auth.{toolName}.{fingerprint}}；value = "approved"（+ optionId 审计）。
 * 跨实例：state 走持久化 store，任意实例续跑可放行。
 *
 * <p>本类是无状态服务：构造注入 SessionStateStore，可被多会话共用；事件经可选 listener 透出（审计）。
 */
public class GuardAuthApi {

    private final SessionStateStore stateStore;
    private final List<Consumer<SessionEvent>> listeners = new CopyOnWriteArrayList<>();

    public GuardAuthApi(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    /** 注册事件监听器（审计用：授权/拒绝事件透出）。 */
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
        String fingerprint = ArgumentFingerprint.fingerprint(toolName, arguments);
        String authKey = ArgumentFingerprint.authKey(toolName, fingerprint);
        String value = "approved:" + (optionId == null ? "approve" : optionId);
        stateStore.put(sessionId, new StateEntry(authKey, value, "guard-auth", 0, null, Instant.now()));
        emitResponse(sessionId, toolName, fingerprint, optionId, input, "approved");
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
        String fingerprint = ArgumentFingerprint.fingerprint(toolName, arguments);
        // 不写 auth key（保持未授权）；仅记审计事件
        emitResponse(sessionId, toolName, fingerprint, optionId, input, "rejected");
    }

    /** 撤销已存在的授权（删除 auth key）。 */
    public void revoke(String sessionId, String toolName, Map<String, Object> arguments) {
        String fingerprint = ArgumentFingerprint.fingerprint(toolName, arguments);
        String authKey = ArgumentFingerprint.authKey(toolName, fingerprint);
        stateStore.delete(sessionId, authKey);
    }

    /** 查询某工具+参数是否已授权（调试/前端展示用）。 */
    public boolean isAuthorized(String sessionId, String toolName, Map<String, Object> arguments) {
        String fingerprint = ArgumentFingerprint.fingerprint(toolName, arguments);
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
        payload.put("decision", decision);
        if (input != null) {
            payload.put("input", input);
        }
        SessionEvent event = new SessionEvent(
                DangerousToolGuardHook.EVENT_CONFIRMATION_RESPONSE, payload, Instant.now());
        listeners.forEach(l -> l.accept(event));
    }
}
