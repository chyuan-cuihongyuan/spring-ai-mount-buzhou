package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * 轮次限速 hook（spec 425 / T741，nginx token bucket 借鉴——突发桶 +
 * 匀速回填）：beforeTurn 准入（ORDER=210 早于守卫族 220+——速率准入最
 * 外层先判）。每次 turn 扣 1 令牌；超限 {@code HookResult.block}（守卫族
 * 同词汇——拒绝即响应不炸轮）+ 计数（无 tag——session 键无界纪律）。
 *
 * <p>key 默认 {@code sessionId}（per-session 频次帽）；构造可插拔
 * {@code keyFunction}（常量键 = per-runtime/租户整体帽——TokenBudgetHook
 * 构造身份同法）。桶 map 按 key 惰性建，键空间=会话数上界（诚实边界）。
 * 惰性回填无定时器——取用时按 elapsed nanos 比例补足（封顶 burst）。
 */
public final class TurnRateLimitHook implements BuzhouHook {

    /** 速率准入位（早于守卫族 220+）。 */
    public static final int ORDER = 210;

    /** 限速策略（burst>=1；permitsPerMinute>0——回填速率）。 */
    public record Policy(int burst, double permitsPerMinute) {

        public Policy {
            if (burst < 1) {
                throw new IllegalArgumentException("burst>=1（当前 " + burst + "）");
            }
            if (!(permitsPerMinute > 0) || Double.isInfinite(permitsPerMinute)) {
                throw new IllegalArgumentException("permitsPerMinute>0（当前 " + permitsPerMinute + "）");
            }
        }
    }

    /** 惰性令牌桶（synchronized 单桶争用=回填算术两行，可接受）。 */
    private static final class Bucket {
        double tokens;
        long lastRefillNanos;

        Bucket(double tokens, long lastRefillNanos) {
            this.tokens = tokens;
            this.lastRefillNanos = lastRefillNanos;
        }

        synchronized boolean tryAcquire(double amount, Policy policy, long nowNanos) {
            refill(policy, nowNanos);
            if (tokens >= amount) {
                tokens -= amount;
                return true;
            }
            return false;
        }

        synchronized double available(Policy policy, long nowNanos) {
            refill(policy, nowNanos);
            return tokens;
        }

        private void refill(Policy policy, long nowNanos) {
            double elapsedMinutes = (nowNanos - lastRefillNanos) / 60e9;
            if (elapsedMinutes > 0) {
                tokens = Math.min(policy.burst(), tokens + elapsedMinutes * policy.permitsPerMinute());
                lastRefillNanos = nowNanos;
            }
        }
    }

    private final Policy policy;
    private final Function<TurnContext, String> keyFunction;
    private final LongSupplier nanoSupplier;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** 默认键=sessionId（per-session 频次帽；System.nanoTime 时钟）。 */
    public TurnRateLimitHook(Policy policy) {
        this(policy, TurnContext::sessionId, System::nanoTime);
    }

    /** 全参构造（keyFunction 可插拔——常量键=租户整体帽；nanoSupplier 测试注入）。 */
    public TurnRateLimitHook(Policy policy, Function<TurnContext, String> keyFunction,
            LongSupplier nanoSupplier) {
        if (keyFunction == null || nanoSupplier == null) {
            throw new IllegalArgumentException("keyFunction/nanoSupplier 必须非空");
        }
        this.policy = policy;
        this.keyFunction = keyFunction;
        this.nanoSupplier = nanoSupplier;
    }

    @Override
    public String name() {
        return "TurnRateLimitHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        String key = keyFunction.apply(ctx);
        if (key == null || key.isBlank()) {
            return HookResult.CONTINUE; // 无键不限（防御——键函数输出空）
        }
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(policy.burst(), nanoSupplier.getAsLong()));
        if (bucket.tryAcquire(1, policy, nanoSupplier.getAsLong())) {
            return HookResult.CONTINUE;
        }
        BuzhouMetricsHolder.metrics().counter("buzhou.ratelimit.turn-blocked");
        return HookResult.block("轮次限速触发（key=" + key + "，桶容量 " + policy.burst()
                + "，回填 " + policy.permitsPerMinute() + "/分钟）——请稍后重试");
    }

    /** 各 key 当前可用令牌快照（观测面；key 字典序）。 */
    public Map<String, Double> availableSnapshot() {
        long now = nanoSupplier.getAsLong();
        Map<String, Double> snapshot = new TreeMap<>();
        buckets.forEach((k, b) -> snapshot.put(k, b.available(policy, now)));
        return snapshot;
    }

    Policy policy() {
        return policy;
    }
}
