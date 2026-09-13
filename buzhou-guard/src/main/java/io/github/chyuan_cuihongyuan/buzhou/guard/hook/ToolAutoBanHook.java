package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具自动封禁 hook（spec 800 / T1101，fail2ban 借鉴）：
 * beforeTool（order 255）发现受监视工具在封禁期内即 block（带剩余秒）；
 * afterTool 观测执行失败（{@code error != null}）在滑动窗口内累计——达
 * {@code maxViolations} 即自动封禁 {@code banSeconds}（窗口过期自然滑出，
 * <b>成功调用不重置</b>——比 fail2ban maxretry 滑窗语义忠实）。
 * <p>粒度为 (sessionId, tool)：单会话失控不锁全租户（fail2ban 按 IP，
 * 此处按会话键——多租户正确性优先）；跟踪键封顶 {@value #MAX_TRACKED_KEYS}
 * （超限 {@code truncated=true} 不再记——基数有界纪律）。
 * <p><b>零配置零行为</b>：watch 集为空 = 恒放行。进程内内存有界（重启清零；
 * 跨实例共享封禁归 Redis 后端族，留位）。
 */
public final class ToolAutoBanHook implements BuzhouHook {

    public static final int ORDER = 255;

    /** 跟踪键（session×tool）封顶。 */
    public static final int MAX_TRACKED_KEYS = 256;
    /** 单键失败时间戳环容量（窗口内不可能超此值有意义——maxViolations 上限建议 ≤ 此值）。 */
    public static final int WINDOW_RING = 64;

    static final String BANNED_COUNTER = "buzhou.guard.tool-autoban.banned";
    static final String BLOCKED_COUNTER = "buzhou.guard.tool-autoban.blocked";

    /** 单条生效封禁（不可变）。 */
    public record ActiveBan(String sessionId, String toolName, long untilMillis, long remainingSeconds) {
    }

    /** 不可变快照：当前生效封禁 + 累计口径。 */
    public record BanSnapshot(List<ActiveBan> active, long totalViolations, long totalBans, boolean truncated) {
    }

    private static final class KeyState {
        final Deque<Long> failures = new ArrayDeque<>();
        long banUntilMillis;
    }

    private final Set<String> watchTools;
    private final int maxViolations;
    private final long windowMillis;
    private final long banMillis;
    private final Clock clock;
    private final Map<String, KeyState> states = new ConcurrentHashMap<>();
    private volatile boolean truncated;
    private long totalViolations;
    private long totalBans;

    public ToolAutoBanHook(Collection<String> watchTools, int maxViolations, long windowSeconds, long banSeconds) {
        this(watchTools, maxViolations, windowSeconds, banSeconds, Clock.systemUTC());
    }

    public ToolAutoBanHook(Collection<String> watchTools, int maxViolations, long windowSeconds,
                           long banSeconds, Clock clock) {
        if (maxViolations < 1) {
            throw new IllegalArgumentException("maxViolations 必须 >= 1，实际 " + maxViolations);
        }
        if (windowSeconds < 1 || banSeconds < 1) {
            throw new IllegalArgumentException("windowSeconds/banSeconds 必须 >= 1，实际 "
                    + windowSeconds + "/" + banSeconds);
        }
        this.watchTools = watchTools == null ? Set.of() : Set.copyOf(new HashSet<>(watchTools));
        this.maxViolations = maxViolations;
        this.windowMillis = windowSeconds * 1000L;
        this.banMillis = banSeconds * 1000L;
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String name() {
        return "ToolAutoBanHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        if (ctx == null || ctx.toolName() == null || watchTools.isEmpty()) {
            return HookResult.CONTINUE;
        }
        if (!watchTools.contains(ctx.toolName())) {
            return HookResult.CONTINUE;
        }
        long now = clock.millis();
        KeyState state = states.get(key(ctx.sessionId(), ctx.toolName()));
        if (state == null) {
            return HookResult.CONTINUE;
        }
        synchronized (state) {
            if (now >= state.banUntilMillis) {
                return HookResult.CONTINUE; // 未封禁或已到期（到期惰性清除语义在读数侧）
            }
            long remaining = (state.banUntilMillis - now) / 1000L;
            BuzhouMetricsHolder.metrics().counter(BLOCKED_COUNTER, 1, "tool", ctx.toolName());
            return HookResult.block("工具「" + ctx.toolName() + "」因滑动窗口内连续失败 "
                    + maxViolations + " 次已被自动封禁，剩余约 " + remaining + "s——请先排查失败原因");
        }
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx == null || ctx.toolName() == null || watchTools.isEmpty()) {
            return HookResult.CONTINUE;
        }
        if (!watchTools.contains(ctx.toolName()) || ctx.error() == null) {
            return HookResult.CONTINUE;
        }
        long now = clock.millis();
        String key = key(ctx.sessionId(), ctx.toolName());
        KeyState state = states.computeIfAbsent(key, k -> {
            if (states.size() >= MAX_TRACKED_KEYS) {
                truncated = true;
                return null; // 键封顶：不再跟踪新键（有界纪律）
            }
            return new KeyState();
        });
        if (state == null) {
            return HookResult.CONTINUE;
        }
        synchronized (state) {
            if (now < state.banUntilMillis) {
                return HookResult.CONTINUE; // 封禁期内失败不再累计（已被挡在外面，多数到不了这）
            }
            state.failures.addLast(now);
            totalViolations++;
            while (!state.failures.isEmpty() && now - state.failures.peekFirst() > windowMillis) {
                state.failures.pollFirst(); // 窗口外滑出
            }
            if (state.failures.size() >= maxViolations) {
                state.banUntilMillis = now + banMillis;
                state.failures.clear();
                totalBans++;
                BuzhouMetricsHolder.metrics().counter(BANNED_COUNTER, 1, "tool", ctx.toolName());
            }
        }
        return HookResult.CONTINUE;
    }

    /** 只读快照：生效中封禁（含键封顶/truncated 标记与累计口径）。 */
    public BanSnapshot snapshot() {
        long now = clock.millis();
        List<ActiveBan> active = new ArrayList<>();
        for (Map.Entry<String, KeyState> e : states.entrySet()) {
            KeyState state = e.getValue();
            synchronized (state) {
                if (now < state.banUntilMillis) {
                    String[] parts = e.getKey().split("\u0000", 2);
                    active.add(new ActiveBan(parts[0], parts.length > 1 ? parts[1] : "",
                            state.banUntilMillis, (state.banUntilMillis - now) / 1000L));                }
            }
        }
        active.sort(java.util.Comparator.comparingLong(ActiveBan::untilMillis).reversed());
        return new BanSnapshot(List.copyOf(active), totalViolations, totalBans, truncated);
    }

    private static String key(String sessionId, String toolName) {
        return (sessionId == null ? "" : sessionId) + "\u0000" + toolName;
    }

    /** 当前时钟读数（测试/运维诊断辅助；非变更面）。 */
    public Instant now() {
        return clock.instant();
    }
}
