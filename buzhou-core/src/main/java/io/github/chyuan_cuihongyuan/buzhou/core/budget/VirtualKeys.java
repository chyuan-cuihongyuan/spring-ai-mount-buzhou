package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 虚拟 key 配额注册表（spec 124 §A / T449，LiteLLM virtual-key budgets 借鉴）：
 * 每个 key 一个 token 硬顶，花超即拒——多租户/多下游 key 分账限流的进程内事实源。
 *
 * <p><b>口径（诚实声明）</b>：进程内有界表（{@value #MAX_KEYS} 个 key 封顶，满则
 * fail-fast——配置错误要响亮）；<b>未注册 key 直通</b>（不设虚拟预算 = 不拦，与
 * 「默认关」哲学一致，不是漏洞是边界）；窗口化沿用 export → {@link #reset(String)}
 * 循环（spec 121 同纪律——不自装调度）。
 *
 * <p><b>并发</b>：per-key 单 {@link AtomicLong} CAS 循环——「加完后不越限」原子
 * 判定，拒绝时不留部分扣减。拒绝计数 {@code buzhou.virtual-keys.rejected}
 * （无 tag——key 名天生无界，不进 tag 是纪律）。
 */
public final class VirtualKeys {

    /** 注册表封顶（满 = 配置错误，fail-fast 而非静默吞 key）。 */
    public static final int MAX_KEYS = 256;

    /** 单 key 用量快照（used/limit 井读——账单与告警面）。 */
    public record KeyUsage(String key, long usedTokens, long limitTokens) {
    }

    private final Map<String, AtomicLong> used = new ConcurrentHashMap<>();
    private final Map<String, Long> limits = new ConcurrentHashMap<>();

    private VirtualKeys() {
    }

    /** 独立实例（宿主自管作用域用）。 */
    public static VirtualKeys create() {
        return new VirtualKeys();
    }

    /**
     * 注册 key 与 token 硬顶。
     *
     * @throws IllegalArgumentException key 空白 / 限额 ≤ 0 / 重复注册 / 表满
     */
    public void register(String key, long limitTokens) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("virtual key must not be blank");
        }
        if (limitTokens <= 0) {
            throw new IllegalArgumentException("limitTokens must be positive: " + limitTokens);
        }
        if (limits.putIfAbsent(key, limitTokens) != null) {
            throw new IllegalArgumentException("virtual key already registered: " + key);
        }
        if (used.putIfAbsent(key, new AtomicLong()) != null || limits.size() > MAX_KEYS) {
            limits.remove(key);
            used.remove(key);
            throw new IllegalArgumentException("virtual key registry full (max " + MAX_KEYS + ")");
        }
    }

    /**
     * 尝试扣减（原子：加完后越限则整体拒绝，不留部分扣减）。
     *
     * <p>未注册 key 直通（true）——不设虚拟预算 = 不拦；tokens=0 恒成功（无耗 noop）。
     */
    public boolean trySpend(String key, long tokens) {
        if (tokens < 0) {
            throw new IllegalArgumentException("tokens must not be negative: " + tokens);
        }
        if (tokens == 0) {
            return true;
        }
        AtomicLong spent = used.get(key);
        Long limit = limits.get(key);
        if (spent == null || limit == null) {
            return true; // 未注册 key：直通
        }
        long prev = spent.get();
        while (true) {
            long next = prev + tokens;
            if (next > limit) {
                BuzhouMetricsHolder.metrics().counter("buzhou.virtual-keys.rejected");
                return false;
            }
            if (spent.compareAndSet(prev, next)) {
                return true;
            }
            prev = spent.get();
        }
    }

    /**
     * 扣减或抛 {@link QuotaExceededException}（宿主 ingress/预算闸用）。
     */
    public void spendOrThrow(String key, long tokens) {
        if (!trySpend(key, tokens)) {
            throw new QuotaExceededException("virtual key \"" + key + "\" token quota exceeded: used "
                    + used.get(key).get() + " + " + tokens + " > limit " + limits.get(key));
        }
    }

    /** 单 key 用量快照（未注册返回 null——诚实空值）。 */
    public KeyUsage usage(String key) {
        AtomicLong spent = used.get(key);
        Long limit = limits.get(key);
        if (spent == null || limit == null) {
            return null;
        }
        return new KeyUsage(key, spent.get(), limit);
    }

    /** 用量 top-N（used 降序，同 used key 字典序——输出稳定；未动用的 key 也可见）。 */
    public List<KeyUsage> topUsage(int n) {
        return used.entrySet().stream()
                .map(e -> new KeyUsage(e.getKey(), e.getValue().get(), limits.get(e.getKey())))
                .filter(u -> u.limitTokens() > 0)
                .sorted((a, b) -> {
                    int byUsed = Long.compare(b.usedTokens(), a.usedTokens());
                    return byUsed != 0 ? byUsed : a.key().compareTo(b.key());
                })
                .limit(Math.max(0, n))
                .toList();
    }

    /** 窗口清零（export → reset 循环每窗口一份账；未注册 key no-op）。 */
    public void reset(String key) {
        AtomicLong spent = used.get(key);
        if (spent != null) {
            spent.set(0);
        }
    }

    /** 在册 key 数（测试/健康面）。 */
    public int distinct() {
        return limits.size();
    }
}
