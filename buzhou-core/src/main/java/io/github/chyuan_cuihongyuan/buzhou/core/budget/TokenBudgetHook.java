package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token/成本预算 Hook（spec 16，T83 / impl-58）：会话级 usage 累计 + 三硬顶预算闸。
 *
 * <p><b>与 RunawayHook 的分工</b>：runaway 管「行为失控」（步数/调用数/时长），本 Hook 管
 * 「资源消耗预算」（token/成本）——同范式分立演进（order 1100，runaway(1000) 之后）。
 *
 * <p><b>计量</b>：{@code afterModel} 从响应 usage 提取 prompt/completion tokens（与
 * ObservabilityAdvisor 同源同口径；null/零跳过——替身模型不误记），累计进 SessionStateStore
 * （跨崩溃持久化，与 runaway 会话累计同先例）。成本按 {@code buzhou.token-budget.pricing.<model>}
 * 价目换算为 micro-USD long 累计（{@code token × 每百万价 = microUsd/token}，整数无浮点漂移；
 * 无价目 = 零成本）。每次有效累计发 {@code budget.tokens-accumulated} 事件（双写 EventRecord）。
 *
 * <p><b>闸门</b>（safe-by-default，null = 不限）：{@code beforeModel} 检查会话累计是否已超
 * prompt/total/cost 三硬顶——已消耗预算不可逆，拦截发生在<b>下一次</b>模型调用之前；
 * 超限发 hard-stop 事件（携带 partialResultRef，与 runaway 硬顶同口径）+ {@code block(reason)} 终止。
 *
 * <p><b>线程安全</b>：per-session 监视器锁（工具扇出下多步并行 afterModel 的 RMW 竞争防护）。
 */
public class TokenBudgetHook implements BuzhouHook {

    /** 每次有效 usage 累计（payload：本次 + 会话累计快照 + 可选成本与模型名）。 */
    public static final String EVENT_TOKENS_ACCUMULATED = "budget.tokens-accumulated";
    /** token 硬顶终止（reason=prompt-tokens/total-tokens，payload 含 partialResultRef）。 */
    public static final String EVENT_TOKEN_HARD_STOP = "budget.token-hard-stop";
    /** 成本硬顶终止（payload 含 partialResultRef）。 */
    public static final String EVENT_COST_HARD_STOP = "budget.cost-hard-stop";

    private static final String KEY_PROMPT = "buzhou.budget.prompt-tokens";
    private static final String KEY_COMPLETION = "buzhou.budget.completion-tokens";
    private static final String KEY_COST_MICRO_USD = "buzhou.budget.cost-micro-usd";

    private final BuzhouTokenBudgetProperties props;
    private final String defaultModelName;
    private final io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore observabilityStore;
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();

    public TokenBudgetHook(BuzhouTokenBudgetProperties props, String defaultModelName,
                           io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore observabilityStore) {
        this.props = props;
        this.defaultModelName = defaultModelName == null || defaultModelName.isBlank()
                ? "unknown" : defaultModelName;
        this.observabilityStore = observabilityStore;
    }

    @Override
    public int order() {
        return 1100; // runaway(1000) 之后：预算闸在行为闸之后裁决
    }

    /** 累计：afterModel 提取 usage 入账 + 事件（不 Block——响应已生成，闸在下一 beforeModel）。 */
    @Override
    public HookResult afterModel(ModelCallContext ctx) {
        if (!props.enabled()) {
            return HookResult.CONTINUE;
        }
        ChatClientResponse response = ctx.response();
        if (response == null || response.chatResponse() == null
                || response.chatResponse().getMetadata() == null) {
            return HookResult.CONTINUE;
        }
        Usage usage = response.chatResponse().getMetadata().getUsage();
        if (usage == null || (nz(usage.getPromptTokens()) == 0 && nz(usage.getCompletionTokens()) == 0)) {
            return HookResult.CONTINUE; // 替身模型/无计量响应：不误记
        }
        long prompt = nz(usage.getPromptTokens());
        long completion = nz(usage.getCompletionTokens());
        String model = resolveModelName(ctx);
        long costMicroUsd = microUsd(model, prompt, completion);

        long sessionPrompt;
        long sessionCompletion;
        long sessionCost;
        synchronized (lockFor(ctx.sessionId())) {
            sessionPrompt = stateAdd(ctx, KEY_PROMPT, prompt);
            sessionCompletion = stateAdd(ctx, KEY_COMPLETION, completion);
            sessionCost = stateAdd(ctx, KEY_COST_MICRO_USD, costMicroUsd);
        }

        BuzhouMetricsHolder.metrics().counter("buzhou.budget.prompt-tokens", prompt);
        BuzhouMetricsHolder.metrics().counter("buzhou.budget.completion-tokens", completion);

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("sessionId", ctx.sessionId());
        payload.put("turn", ctx.turn());
        payload.put("model", model);
        payload.put("promptTokens", prompt);
        payload.put("completionTokens", completion);
        payload.put("sessionPromptTokens", sessionPrompt);
        payload.put("sessionCompletionTokens", sessionCompletion);
        payload.put("sessionTotalTokens", sessionPrompt + sessionCompletion);
        if (costMicroUsd > 0 || props.pricing() != null) {
            payload.put("sessionCostUsd", microUsdToUsdString(sessionCost));
        }
        emit(ctx, EVENT_TOKENS_ACCUMULATED, payload);
        return HookResult.CONTINUE;
    }

    /** 闸门：beforeModel 检查会话累计硬顶，超限拦截本次模型调用。 */
    @Override
    public HookResult beforeModel(ModelCallContext ctx) {
        if (!props.enabled() || !props.anyCapConfigured()) {
            return HookResult.CONTINUE;
        }
        long sessionPrompt = stateGet(ctx, KEY_PROMPT);
        long sessionCompletion = stateGet(ctx, KEY_COMPLETION);
        long sessionTotal = sessionPrompt + sessionCompletion;
        if (props.maxSessionTotalTokens() != null && sessionTotal >= props.maxSessionTotalTokens()) {
            return hardStop(ctx, EVENT_TOKEN_HARD_STOP, "total-tokens",
                    props.maxSessionTotalTokens(), sessionTotal);
        }
        if (props.maxSessionPromptTokens() != null && sessionPrompt >= props.maxSessionPromptTokens()) {
            return hardStop(ctx, EVENT_TOKEN_HARD_STOP, "prompt-tokens",
                    props.maxSessionPromptTokens(), sessionPrompt);
        }
        if (props.maxSessionCostUsd() != null) {
            long sessionCost = stateGet(ctx, KEY_COST_MICRO_USD);
            long capMicro = usdToMicroUsd(props.maxSessionCostUsd());
            if (sessionCost >= capMicro) {
                return hardStop(ctx, EVENT_COST_HARD_STOP, "cost-usd",
                        props.maxSessionCostUsd(), BigDecimal.valueOf(sessionCost, 6));
            }
        }
        return HookResult.CONTINUE;
    }

    // ---- helpers ----

    private String resolveModelName(ModelCallContext ctx) {
        if (ctx.request() != null && ctx.request().prompt() != null) {
            ChatOptions options = ctx.request().prompt().getOptions();
            if (options != null && options.getModel() != null && !options.getModel().isBlank()) {
                return options.getModel();
            }
        }
        return defaultModelName;
    }

    /** microUsd 口径：token × 每百万价（USD）恰为 microUsd/token；无价目 = 0。 */
    private long microUsd(String model, long promptTokens, long completionTokens) {
        BuzhouTokenBudgetProperties.Pricing p =
                props.pricing() == null ? null : props.pricing().get(model);
        if (p == null) {
            return 0L;
        }
        // (tokens/1e6) × priceUsd × 1e6 = tokens × price（整数化取半 rounding）
        long in = BigDecimal.valueOf(promptTokens).multiply(p.inputPerMillion())
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
        long out = BigDecimal.valueOf(completionTokens).multiply(p.outputPerMillion())
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
        return in + out;
    }

    private long stateAdd(HookContext ctx, String key, long delta) {
        long current = stateGet(ctx, key);
        long next = current + delta;
        ctx.state().put(key, Long.toString(next));
        return next;
    }

    private long stateGet(HookContext ctx, String key) {
        return ctx.state().get(key, String.class).map(Long::valueOf).orElse(0L);
    }

    private Object lockFor(String sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, k -> new Object());
    }

    private HookResult hardStop(HookContext ctx, String event, String reason, Number limit, Number value) {
        emit(ctx, event, Map.of(
                "sessionId", ctx.sessionId(),
                "turn", ctx.turn(),
                "reason", reason,
                "limit", limit,
                "value", value,
                "partialResultRef", "messageStore:" + ctx.sessionId()
        ));
        BuzhouMetricsHolder.metrics().counter("buzhou.budget.hard-stops", "reason", reason);
        return HookResult.block("已达到会话 token/成本预算上限（" + reason + "，限额 " + limit
                + "，已消耗 " + value + "），本轮终止。已完成的工具调用结果已随轮次保留，"
                + "可基于部分结果新开会话继续。");
    }

    private void emit(HookContext ctx, String type, Map<String, Object> payload) {
        SessionEvent event = new SessionEvent(type, payload, Instant.now());
        ctx.emitEvent(event);
        if (observabilityStore != null) {
            try {
                observabilityStore.saveEvents(List.of(new EventRecord(
                        java.util.UUID.randomUUID().toString(), null, ctx.sessionId(),
                        type, event.occurredAt(), payload)));
            } catch (RuntimeException swallowed) {
                // 双写失败不阻断主流程（观测降级），与 runaway 同口径
            }
        }
    }

    private static long nz(Number v) {
        return v == null ? 0L : v.longValue();
    }

    private static long usdToMicroUsd(BigDecimal usd) {
        return usd.movePointRight(6).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static String microUsdToUsdString(long microUsd) {
        return BigDecimal.valueOf(microUsd, 6).setScale(6, RoundingMode.HALF_UP)
                .toPlainString() + " USD";
    }
}
