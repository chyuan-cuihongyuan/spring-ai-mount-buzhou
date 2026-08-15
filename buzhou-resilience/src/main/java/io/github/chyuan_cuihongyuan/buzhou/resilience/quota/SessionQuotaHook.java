package io.github.chyuan_cuihongyuan.buzhou.resilience.quota;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.Usage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * per-session 日配额 Hook（spec 16「per-session 配额」，T84 / impl-59）：turns / tool-calls /
 * tokens 三维度，UTC 自然日固定窗口。
 *
 * <p><b>计数存放</b>：SessionStateStore 单键单维度 {@code "<epochDay>:<count>"}（键
 * {@code buzhou.quota.turns / tool-calls / tokens}），读时日不符即重置——免过期键清理、跨崩溃持久化。
 * tokens 在 {@code afterModel} 自行按日键累计（与 core TokenBudgetHook 同源提取 usage，键不交叉：
 * 生命周期累计 ≠ 日窗配额）。
 *
 * <p><b>拦截点</b>：beforeTurn（turns）/ beforeTool（tool-calls，reason 回注为工具结果文本）/
 * beforeModel（tokens，读当日已累计）。超配额 {@code block}（受控终态文本，对齐 runaway/budget 硬顶）
 * + 事件 {@code quota.exceeded}。
 *
 * <p><b>诚实边界</b>：单进程语义——多实例部署 = 每实例独立配额（分布式配额 out-of-scope）。
 */
public class SessionQuotaHook implements BuzhouHook {

    /** 超配额事件（dimension/limit/value/day/sessionId/turn）。 */
    public static final String EVENT_QUOTA_EXCEEDED = "quota.exceeded";

    private static final String KEY_TURNS = "buzhou.quota.turns";
    private static final String KEY_TOOL_CALLS = "buzhou.quota.tool-calls";
    private static final String KEY_TOKENS = "buzhou.quota.tokens";

    private final ResilienceProperties.SessionQuota quota;
    private final ResilienceStats stats; // null 安全
    private final java.time.Clock clock; // spec 41 §B / T154：UTC 日窗时钟可注入（测试零等待翻日）
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();

    public SessionQuotaHook(ResilienceProperties.SessionQuota quota, ResilienceStats stats) {
        this(quota, stats, java.time.Clock.systemUTC());
    }

    /** spec 41 §B / T154 / impl-125：时钟注入（契约：须为 UTC 基时钟）；缺省 systemUTC。 */
    public SessionQuotaHook(ResilienceProperties.SessionQuota quota, ResilienceStats stats,
            java.time.Clock clock) {
        this.quota = quota;
        this.stats = stats;
        this.clock = clock == null ? java.time.Clock.systemUTC() : clock;
    }

    @Override
    public int order() {
        return 1150; // budget(1100) 之后：日窗配额在生命周期预算之后裁决
    }

    /** 是否任一维度配置（装配期判定是否挂本 Hook）。 */
    public static boolean anyDimension(ResilienceProperties.SessionQuota quota) {
        return quota != null && (quota.turnsPerDay() != null
                || quota.toolCallsPerDay() != null || quota.tokensPerDay() != null);
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        Integer cap = quota.turnsPerDay();
        if (cap == null) {
            return HookResult.CONTINUE;
        }
        int used = incrementDayCounter(ctx, KEY_TURNS);
        if (used > cap) {
            return exceeded(ctx, "turns", cap, used - 1); // 本轮未开始即拦截，计数回退语义：用 cap 表述
        }
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        Integer cap = quota.toolCallsPerDay();
        if (cap == null) {
            return HookResult.CONTINUE;
        }
        int used;
        synchronized (lockFor(ctx.sessionId())) {
            used = incrementDayCounter(ctx, KEY_TOOL_CALLS);
        }
        if (used > cap) {
            return exceeded(ctx, "tool-calls", cap, used);
        }
        return HookResult.CONTINUE;
    }

    /** tokens 配额：beforeModel 读当日已累计（afterModel 只入账不拦截——响应已生成）。 */
    @Override
    public HookResult beforeModel(ModelCallContext ctx) {
        Long cap = quota.tokensPerDay();
        if (cap == null) {
            return HookResult.CONTINUE;
        }
        long usedToday;
        synchronized (lockFor(ctx.sessionId())) {
            usedToday = readToday(ctx, KEY_TOKENS);
        }
        if (usedToday >= cap) {
            return exceeded(ctx, "tokens", cap, usedToday);
        }
        return HookResult.CONTINUE;
    }

    /** tokens 入账：与 TokenBudgetHook 同源提取 usage（null/零跳过）。 */
    @Override
    public HookResult afterModel(ModelCallContext ctx) {
        Long cap = quota.tokensPerDay();
        if (cap == null) {
            return HookResult.CONTINUE;
        }
        ChatClientResponse response = ctx.response();
        if (response == null || response.chatResponse() == null
                || response.chatResponse().getMetadata() == null) {
            return HookResult.CONTINUE;
        }
        Usage usage = response.chatResponse().getMetadata().getUsage();
        if (usage == null) {
            return HookResult.CONTINUE;
        }
        long tokens = nz(usage.getPromptTokens()) + nz(usage.getCompletionTokens());
        if (tokens == 0) {
            return HookResult.CONTINUE;
        }
        synchronized (lockFor(ctx.sessionId())) {
            long today = readToday(ctx, KEY_TOKENS);
            ctx.state().put(KEY_TOKENS, todayKey() + ":" + (today + tokens));
        }
        return HookResult.CONTINUE;
    }

    // ---- helpers ----

    /** 递增并返回当日计数（日不符先重置；调用方按需持会话锁——beforeTurn 单线程可不持）。 */
    private int incrementDayCounter(HookContext ctx, String key) {
        long today = readToday(ctx, key);
        int next = (int) today + 1;
        ctx.state().put(key, todayKey() + ":" + next);
        return next;
    }

    private long readToday(HookContext ctx, String key) {
        String raw = ctx.state().get(key, String.class).orElse(null);
        if (raw == null) {
            return 0L;
        }
        int sep = raw.indexOf(':');
        if (sep < 0) {
            return 0L;
        }
        try {
            long day = Long.parseLong(raw.substring(0, sep));
            if (day != todayKey()) {
                return 0L; // 新的一天：读时重置（写回由随后 increment/put 完成）
            }
            return Long.parseLong(raw.substring(sep + 1));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private long todayKey() {
        return LocalDate.now(clock).toEpochDay(); // clock 契约：UTC 基（缺省 systemUTC）
    }

    private Object lockFor(String sessionId) {
        return sessionLocks.computeIfAbsent(sessionId, k -> new Object());
    }

    private HookResult exceeded(HookContext ctx, String dimension, Number limit, Number value) {
        if (stats != null) {
            stats.recordQuotaRejection();
        }
        ctx.emitEvent(new SessionEvent(EVENT_QUOTA_EXCEEDED, Map.of(
                "sessionId", ctx.sessionId(),
                "turn", ctx.turn(),
                "dimension", dimension,
                "limit", limit,
                "value", value,
                "day", todayKey()
        ), Instant.now()));
        return HookResult.block("已达到本会话当日配额上限（" + dimension + "，限额 " + limit
                + "，已用 " + value + "，UTC 日 " + todayKey() + "）。配额每 UTC 自然日重置；"
                + "如需继续请明日再试或新开会话。");
    }

    private static long nz(Number v) {
        return v == null ? 0L : v.longValue();
    }
}
