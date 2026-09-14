package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * per-tool 会话配额 hook（spec 185 / T557，云厂商 per-API quota 借鉴）：
 * beforeTool（order 250）按会话态 {@code buzhou.tool-quota.<tool>} 计数——
 * 超限 block（可读理由）；<b>放行即计、被拒不计</b>；列名上限优先于 "*" 通配
 * 默认；会话态生命周期 = 天然 per-session 配额窗。不配置零限制零变化。
 */
public final class ToolQuotaHook implements BuzhouHook {

    public static final int ORDER = 250;
    static final String STATE_PREFIX = "buzhou.tool-quota.";
    static final String WILDCARD = "*";
    static final String BLOCKED_COUNTER = "buzhou.tool-quota.blocked";

    private final Map<String, Integer> quotas;

    // —— spec 1068 / impl 820：配额消耗读面（per-API quota 对账思想；静态面理由同
    // R46–R67 先例）。守恒：calls = allowed + quotaBlocks + unmanagedSkips；
    // excludedTokens 为旁路修正量不占入口桶。
    private static final java.util.concurrent.atomic.AtomicLong CALLS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong ALLOWED =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong QUOTA_BLOCKS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong UNMANAGED_SKIPS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong EXCLUDED_TOKENS =
            new java.util.concurrent.atomic.AtomicLong();

    /** 配额消耗分布快照（spec 1068）。 */
    public record ToolQuotaStats(long calls, long allowed, long quotaBlocks,
                                 long unmanagedSkips, long excludedTokens) {
    }

    /** 只读快照（守恒 calls = allowed + quotaBlocks + unmanagedSkips）。 */
    public static ToolQuotaStats stats() {
        return new ToolQuotaStats(CALLS.get(), ALLOWED.get(), QUOTA_BLOCKS.get(),
                UNMANAGED_SKIPS.get(), EXCLUDED_TOKENS.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        CALLS.set(0);
        ALLOWED.set(0);
        QUOTA_BLOCKS.set(0);
        UNMANAGED_SKIPS.set(0);
        EXCLUDED_TOKENS.set(0);
    }

    public ToolQuotaHook(Map<String, Integer> toolQuotas) {
        Map<String, Integer> normalized = new LinkedHashMap<>();
        if (toolQuotas != null) {
            toolQuotas.forEach((tool, limit) -> {
                if (tool != null && !tool.isBlank() && limit != null && limit >= 1) {
                    normalized.put(tool, limit);
                }
            });
        }
        this.quotas = Map.copyOf(normalized);
    }

    @Override
    public String name() {
        return "ToolQuotaHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        CALLS.incrementAndGet();
        if (ctx == null || ctx.toolName() == null) {
            UNMANAGED_SKIPS.incrementAndGet();
            return HookResult.CONTINUE;
        }
        Integer limit = quotas.get(ctx.toolName());
        if (limit == null) {
            limit = quotas.get(WILDCARD); // 未列名走通配默认
        }
        if (limit == null) {
            UNMANAGED_SKIPS.incrementAndGet();
            return HookResult.CONTINUE; // 零配置零限制
        }
        String key = STATE_PREFIX + ctx.toolName();
        // 会话态 value 口径为 String（StateEntry 存 String.valueOf）——字符串往返
        int used = ctx.state().get(key, String.class)
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        EXCLUDED_TOKENS.incrementAndGet(); // 坏值静默修正显形
                        return 0;
                    }
                })
                .orElse(0);
        if (used >= limit) {
            QUOTA_BLOCKS.incrementAndGet();
            BuzhouMetricsHolder.metrics().counter(BLOCKED_COUNTER, 1, "tool", ctx.toolName());
            return HookResult.block("本会话工具「" + ctx.toolName() + "」调用已达上限（"
                    + limit + " 次）——请改用其他工具或结束当前任务");
        }
        ctx.state().put(key, String.valueOf(used + 1)); // 放行即计（被拒不重复计）
        ALLOWED.incrementAndGet();
        return HookResult.CONTINUE;
    }
}
