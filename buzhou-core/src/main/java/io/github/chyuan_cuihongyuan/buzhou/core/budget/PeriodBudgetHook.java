package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.metadata.Usage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日历周期预算 hook（spec 408 / T707，AWS Budgets calendar period 借鉴）：
 * 全进程月/周/日预算——periodTag 入键，<b>翻页 = 换 tag 隐式重置</b>
 * （SessionQuotaHook epochDay 技巧推广）。afterModel 计量（tokens + cost
 * 双轨，usage 提取与 TokenBudgetHook 同口径）；beforeModel 闸（本周期
 * 累计达任一限额即 block——不可逆预算纪律，计量不 block 闸在下一调用）；
 * warning-percent 软预警一次一发（338 同语义按周期域）。状态挂合成会话
 * {@code __buzhou.period-budget__}（archive/eval 先例）。全局预算单进程
 * 语义（多实例 = N 份——runbook 口径）。
 */
public final class PeriodBudgetHook implements BuzhouHook {

    /** 软预警（一次一发——warned 即终局，按周期域）。 */
    public static final String EVENT_WARNING = "budget.period.warning";
    /** 周期耗尽拦截。 */
    public static final String EVENT_EXCEEDED = "budget.period.exceeded";

    /** 合成会话 Id（全局周期预算状态挂点）。 */
    public static final String SYNTHETIC_SESSION = "__buzhou.period-budget__";

    /** 周期粒度。 */
    public enum Unit { MONTHLY, WEEKLY, DAILY }

    /** 每百万 token 单价（USD）。 */
    public record Pricing(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {
    }

    private static final String KEY_TOKENS = "period.tokens";
    private static final String KEY_COST = "period.cost-micro-usd";
    private static final int WARNED_BOUND = 1024;

    private final SessionStateStore store;
    private final Unit unit;
    private final Long tokensLimit;      // null = 不限
    private final Long costMicroLimit;   // null = 不限
    private final int warningPercent;
    private final Map<String, Pricing> pricing;
    private final String defaultModelName;
    private final Clock clock;
    private final Object lock = new Object();
    private final Map<String, Boolean> warned = new ConcurrentHashMap<>();

    public PeriodBudgetHook(SessionStateStore store, Unit unit, Long tokensLimit,
            Long costMicroLimit, int warningPercent, Map<String, Pricing> pricing,
            String defaultModelName, Clock clock) {
        this.store = store;
        this.defaultModelName = defaultModelName == null || defaultModelName.isBlank()
                ? "unknown" : defaultModelName;
        this.unit = unit == null ? Unit.MONTHLY : unit;
        this.tokensLimit = tokensLimit != null && tokensLimit > 0 ? tokensLimit : null;
        this.costMicroLimit = costMicroLimit != null && costMicroLimit > 0 ? costMicroLimit : null;
        this.warningPercent = Math.min(100, Math.max(1, warningPercent));
        this.pricing = pricing == null ? Map.of() : pricing;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public String name() {
        return "PeriodBudgetHook";
    }

    @Override
    public int order() {
        return 1150; // TokenBudgetHook(1100) 之后：预算闸链尾
    }

    /** 闸：本周期累计达任一限额 → block（下一调用拦截）。 */
    @Override
    public HookResult beforeModel(ModelCallContext ctx) {
        if (tokensLimit == null && costMicroLimit == null) {
            return HookResult.CONTINUE;
        }
        String tag = periodTag();
        long tokens = cumulative(KEY_TOKENS, tag);
        long cost = cumulative(KEY_COST, tag);
        if (tokensLimit != null && tokens >= tokensLimit) {
            ctx.emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                    EVENT_EXCEEDED, Map.of("unit", unit.name(), "period", tag,
                            "dimension", "tokens", "used", tokens,
                            "limit", tokensLimit.longValue())));
            return HookResult.block("周期[" + tag + "] token 预算已耗尽（限额 "
                    + tokensLimit + "，已用 " + tokens + "）——下个周期自动重置");
        }
        if (costMicroLimit != null && cost >= costMicroLimit) {
            ctx.emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                    EVENT_EXCEEDED, Map.of("unit", unit.name(), "period", tag,
                            "dimension", "cost-micro-usd", "used", cost,
                            "limit", costMicroLimit.longValue())));
            return HookResult.block("周期[" + tag + "] 成本预算已耗尽（限额 "
                    + costMicroLimit + " microUsd，已用 " + cost + "）——下个周期自动重置");
        }
        return HookResult.CONTINUE;
    }

    /** 计量：usage 入账（tokens + cost 双轨）+ 软预警（不 block——闸在下一 beforeModel）。 */
    @Override
    public HookResult afterModel(ModelCallContext ctx) {
        ChatClientResponse response = ctx.response();
        if (response == null || response.chatResponse() == null
                || response.chatResponse().getMetadata() == null) {
            return HookResult.CONTINUE;
        }
        Usage usage = response.chatResponse().getMetadata().getUsage();
        if (usage == null) {
            return HookResult.CONTINUE;
        }
        long prompt = nz(usage.getPromptTokens());
        long completion = nz(usage.getCompletionTokens());
        if (prompt == 0 && completion == 0) {
            return HookResult.CONTINUE; // 替身模型/无计量响应：不误记
        }
        String tag = periodTag();
        long tokens;
        long cost;
        synchronized (lock) {
            tokens = add(KEY_TOKENS, tag, prompt + completion);
            cost = add(KEY_COST, tag, microUsd(resolveModelName(ctx), prompt, completion));
        }
        maybeWarn(ctx, tag, tokens, cost);
        return HookResult.CONTINUE;
    }

    /** 当前周期累计（测试/面板读）。 */
    public long periodTokens() {
        return cumulative(KEY_TOKENS, periodTag());
    }

    public long periodCostMicroUsd() {
        return cumulative(KEY_COST, periodTag());
    }

    void maybeWarn(ModelCallContext ctx, String tag, long tokens, long cost) {
        String warnKey = unit + ":" + tag;
        if (warned.size() < WARNED_BOUND && warned.putIfAbsent(warnKey, Boolean.TRUE) != null) {
            return; // 一次一发（warned 即终局）
        }
        String dimension = overPercent(tokensLimit, tokens, costMicroLimit, cost, warningPercent);
        if (dimension != null) {
            ctx.emitEvent(io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent.of(
                    EVENT_WARNING, Map.of("unit", unit.name(), "period", tag,
                            "dimension", dimension, "warningPercent", warningPercent,
                            "tokens", tokens, "costMicroUsd", cost)));
        }
    }

    private static String overPercent(Long tokensLimit, long tokens, Long costLimit, long cost,
            int warningPercent) {
        String dimension = null;
        int maxPct = 0;
        if (tokensLimit != null && tokensLimit > 0) {
            int pct = (int) Math.min(100, tokens * 100 / tokensLimit);
            if (pct >= maxPct) {
                maxPct = pct;
                dimension = "tokens";
            }
        }
        if (costLimit != null && costLimit > 0) {
            int pct = (int) Math.min(100, cost * 100 / costLimit);
            if (pct >= maxPct) {
                maxPct = pct;
                dimension = "cost-micro-usd";
            }
        }
        return maxPct >= warningPercent ? dimension : null;
    }

    /** 周期 tag：MONTHLY=yyyy-MM / WEEKLY=epochWeek / DAILY=epochDay。 */
    String periodTag() {
        LocalDate today = LocalDate.now(clock);
        return switch (unit) {
            case MONTHLY -> YearMonth.from(today).toString();
            case WEEKLY -> String.valueOf(today.toEpochDay() / 7);
            case DAILY -> String.valueOf(today.toEpochDay());
        };
    }

    private long cumulative(String key, String tag) {
        return store.get(SYNTHETIC_SESSION, key)
                .map(e -> countForTag(e.value(), tag))
                .orElse(0L);
    }

    private long add(String key, String tag, long delta) {
        long current = cumulative(key, tag);
        long next = current + delta;
        store.put(SYNTHETIC_SESSION, new StateEntry(key, tag + ":" + next, "budget",
                0, null, Instant.now(clock)));
        return next;
    }

    /** 值形态 {@code <tag>:<cumulative>}——tag 不匹配即 0（翻页隐式重置）。 */
    static long countForTag(String raw, String tag) {
        if (raw == null) {
            return 0;
        }
        int sep = raw.indexOf(':');
        if (sep < 0 || !tag.equals(raw.substring(0, sep))) {
            return 0;
        }
        try {
            return Long.parseLong(raw.substring(sep + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String resolveModelName(ModelCallContext ctx) {
        if (ctx.request() != null && ctx.request().prompt() != null) {
            var options = ctx.request().prompt().getOptions();
            if (options != null && options.getModel() != null && !options.getModel().isBlank()) {
                return options.getModel();
            }
        }
        return defaultModelName;
    }

    /** microUsd 口径：token × 每百万价（USD）恰为 microUsd/token；无价目 = 0（诚实）。 */
    private long microUsd(String model, long prompt, long completion) {
        Pricing p = pricing.get(model);
        if (p == null) {
            return 0;
        }
        long micro = 0;
        if (p.inputPerMillion() != null) {
            micro += p.inputPerMillion().multiply(BigDecimal.valueOf(prompt))
                    .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP)
                    .movePointRight(6).longValue();
        }
        if (p.outputPerMillion() != null) {
            micro += p.outputPerMillion().multiply(BigDecimal.valueOf(completion))
                    .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP)
                    .movePointRight(6).longValue();
        }
        return micro;
    }

    private static long nz(Number v) {
        return v == null ? 0L : v.longValue();
    }
}
