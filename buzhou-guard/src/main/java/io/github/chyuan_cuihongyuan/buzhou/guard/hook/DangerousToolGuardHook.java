package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.AuthTtl;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.ConfirmOption;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolConfig;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolEntry;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolMatcher;
import io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint.ArgumentFingerprint;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HITL 危险工具守卫（spec 07 HITL 危险工具守卫）。
 *
 * <p>beforeTool（order 300）以通配匹配危险工具清单；命中后查 {@link SessionStateStore} 授权标记
 * {@code auth.{toolName}.{fingerprint}}：
 * <ul>
 *   <li>命中授权：一次性即消费删除（authTtl=once）或保留（authTtl=session）→ CONTINUE 放行。</li>
 *   <li>无授权：BLOCK，工具结果="等待人工确认：{hint}"；经 emitEvent 透出确认请求事件
 *       （{@code buzhou.guard.confirmation.requested}，schema 含 confirmation.options 多选项 +
 *       单输入控件 + hint 嵌 diff）；记 {@code guard.tool.blocked} 审计 Event。</li>
 * </ul>
 *
 * <p>授权写回经 {@link GuardAuthApi#approve}（业务侧 REST），写回后业务重发同一输入 →
 * 守卫查 state 命中 → 放行。state 持久化，跨实例续跑可放行。
 */
public class DangerousToolGuardHook implements BuzhouHook {

    /** 确认请求事件类型（透出与回写共用）。 */
    public static final String EVENT_CONFIRMATION_REQUESTED = "buzhou.guard.confirmation.requested";
    public static final String EVENT_CONFIRMATION_RESPONSE = "buzhou.guard.confirmation.response";
    public static final String EVENT_GUARD_BLOCKED = "guard.tool.blocked";
    public static final String EVENT_AUTH_CONSUMED = "guard.auth.consumed";
    public static final String EVENT_AUTH_REUSED = "guard.auth.reused";

    private final DangerousToolConfig config;
    private final DangerousToolMatcher matcher;
    private final SessionStateStore stateStore;

    public DangerousToolGuardHook(DangerousToolConfig config, SessionStateStore stateStore) {
        this.config = config;
        this.matcher = new DangerousToolMatcher(config.dangerousTools());
        this.stateStore = stateStore;
    }

    @Override
    public String name() {
        return "DangerousToolGuardHook";
    }

    @Override
    public int order() {
        return 300; // spec 07：beforeTool 内置序「副本分离(100) → Onload(200) → HITL(300)」
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        if (!config.enabled()) {
            return HookResult.CONTINUE;
        }
        Optional<DangerousToolEntry> matched = matcher.match(ctx.toolName());
        if (matched.isEmpty()) {
            return HookResult.CONTINUE;
        }
        DangerousToolEntry entry = matched.get();
        String fingerprint = ArgumentFingerprint.fingerprint(ctx.arguments());
        String authKey = ArgumentFingerprint.authKey(ctx.toolName(), fingerprint);

        Optional<StateEntry> auth = stateStore.get(ctx.sessionId(), authKey);
        if (auth.isPresent()
                && consumeIfOnce(ctx, fingerprint, authKey, auth.get().value())) {
            return HookResult.CONTINUE;
        }
        return handleUnauthorized(ctx, entry, fingerprint);
    }

    /**
     * 授权命中处理：once → 经 {@link SessionStateStore#deleteIfValueMatches} 原子消费
     * （CAS 失败 = 授权已被并发实例消费，视同未授权返回 false，重新走确认流程）；
     * session → 保留放行。
     */
    private boolean consumeIfOnce(ToolCallContext ctx, String fingerprint, String authKey, String authValue) {
        if (config.authTtl() == AuthTtl.ONCE) {
            if (!stateStore.deleteIfValueMatches(ctx.sessionId(), authKey, authValue)) {
                return false;
            }
            ctx.emitEvent(new SessionEvent(EVENT_AUTH_CONSUMED, Map.of(
                    "toolName", ctx.toolName(),
                    "toolCallId", ctx.toolCallId(),
                    "fingerprint", fingerprint,
                    "ttl", "once"), Instant.now()));
        } else {
            ctx.emitEvent(new SessionEvent(EVENT_AUTH_REUSED, Map.of(
                    "toolName", ctx.toolName(),
                    "toolCallId", ctx.toolCallId(),
                    "fingerprint", fingerprint,
                    "ttl", "session"), Instant.now()));
        }
        return true;
    }

    private HookResult handleUnauthorized(ToolCallContext ctx, DangerousToolEntry entry, String fingerprint) {
        // 透出确认请求事件（经 SessionEventListener 桥接给业务前端）
        Map<String, Object> payload = buildConfirmationRequestPayload(ctx, entry, fingerprint);
        ctx.emitEvent(new SessionEvent(EVENT_CONFIRMATION_REQUESTED, payload, Instant.now()));
        // 审计：阻断
        ctx.emitEvent(new SessionEvent(EVENT_GUARD_BLOCKED, Map.of(
                "toolName", ctx.toolName(),
                "toolCallId", ctx.toolCallId(),
                "fingerprint", fingerprint,
                "requiredState", entry.requiredState()), Instant.now()));
        String hint = renderHint(entry.hint(), ctx.arguments());
        return HookResult.block("等待人工确认：" + hint);
    }

    private Map<String, Object> buildConfirmationRequestPayload(ToolCallContext ctx,
                                                                DangerousToolEntry entry, String fingerprint) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", ctx.sessionId());
        payload.put("turn", ctx.turn());
        payload.put("toolCallId", ctx.toolCallId());
        payload.put("toolName", ctx.toolName());
        payload.put("argumentsPreview", ctx.arguments() == null ? Map.of() : ctx.arguments());
        payload.put("requiredState", entry.requiredState());
        payload.put("hint", renderHint(entry.hint(), ctx.arguments()));
        payload.put("fingerprint", fingerprint);
        if (entry.confirmation() != null) {
            Map<String, Object> confirmation = new LinkedHashMap<>();
            confirmation.put("title", entry.confirmation().title());
            confirmation.put("options", entry.confirmation().options().stream()
                    .map(this::optionToMap).toList());
            payload.put("confirmation", confirmation);
        }
        return payload;
    }

    private Map<String, Object> optionToMap(ConfirmOption opt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", opt.id());
        m.put("label", opt.label());
        m.put("value", opt.value());
        m.put("hasInput", opt.hasInput());
        if (opt.hasInput()) {
            m.put("inputPlaceholder", opt.inputPlaceholder());
            m.put("inputType", opt.inputType());
        }
        return m;
    }

    /** 渲染 hint 模板：${paramName} 占位符替换为入参值。 */
    private String renderHint(String hint, Map<String, Object> arguments) {
        if (hint == null || hint.isBlank()) {
            return "";
        }
        String rendered = hint;
        if (arguments != null) {
            for (Map.Entry<String, Object> e : arguments.entrySet()) {
                rendered = rendered.replace("${" + e.getKey() + "}", String.valueOf(e.getValue()));
            }
        }
        return rendered;
    }
}
