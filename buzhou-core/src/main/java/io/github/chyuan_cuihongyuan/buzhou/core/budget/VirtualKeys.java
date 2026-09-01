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
    /** 耗尽态：一次超额尝试后锁定（部分消耗永远凑不满限额——诚实表达「下次也不行」）。 */
    private final java.util.Set<String> exhausted = ConcurrentHashMap.newKeySet();
    /** spec 315 / T621：共享计数后端（null = 进程内既有行为逐位不变）。 */
    private final io.github.chyuan_cuihongyuan.buzhou.core.spi.VirtualKeyBudgetBackend backend;

    private VirtualKeys() {
        this(null);
    }

    private VirtualKeys(io.github.chyuan_cuihongyuan.buzhou.core.spi.VirtualKeyBudgetBackend backend) {
        this.backend = backend;
    }

    /** 独立实例（宿主自管作用域用）。 */
    public static VirtualKeys create() {
        return new VirtualKeys();
    }

    /**
     * 共享后端实例（spec 315 / T621）：计数面委托后端（多实例共享额度）；
     * 限额声明（register）仍本地。后端不可达时 trySpend fail-closed（宁可拒绝不可超支）。
     */
    public static VirtualKeys withBackend(
            io.github.chyuan_cuihongyuan.buzhou.core.spi.VirtualKeyBudgetBackend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("backend 非空（无后端请用 create()）");
        }
        return new VirtualKeys(backend);
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
            throw new IllegalArgumentException("tokens must not be negative");
        }
        if (tokens == 0) {
            return true;
        }
        if (backend != null) {
            return trySpendShared(key, tokens);
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
                exhausted.add(key); // spec 148：锁定耗尽态（150/200 类部分消耗不冒充可续）
                return false;
            }
            if (spent.compareAndSet(prev, next)) {
                return true;
            }
            prev = spent.get();
        }
    }

    /** spec 315 / T621：共享后端路径——未注册直通（与进程内口径一致）；后端异常 fail-closed。 */
    private boolean trySpendShared(String key, long tokens) {
        Long limit = limits.get(key);
        if (limit == null) {
            return true; // 未注册 key：直通
        }
        boolean spent;
        try {
            spent = backend.trySpend(key, tokens, limit);
        } catch (RuntimeException e) {
            BuzhouMetricsHolder.metrics().counter("buzhou.virtual-keys.rejected");
            exhausted.add(key); // fail-closed：后端不可达宁可拒绝（诚实表达「下次也可能不行」）
            return false;
        }
        if (!spent) {
            BuzhouMetricsHolder.metrics().counter("buzhou.virtual-keys.rejected");
            exhausted.add(key);
        }
        return spent;
    }

    /**
     * 扣减或抛 {@link QuotaExceededException}（宿主 ingress/预算闸用）。
     */
    public void spendOrThrow(String key, long tokens) {
        if (!trySpend(key, tokens)) {
            KeyUsage current = usage(key);
            throw new QuotaExceededException("virtual key \"" + key + "\" token quota exceeded: used "
                    + (current == null ? "?" : current.usedTokens()) + " + " + tokens
                    + " > limit " + (current == null ? "?" : current.limitTokens()));
        }
    }

    /** 单 key 用量快照（未注册返回 null——诚实空值）。 */
    public KeyUsage usage(String key) {
        Long limit = limits.get(key);
        if (limit == null) {
            return null;
        }
        if (backend != null) {
            return new KeyUsage(key, backendUsed(key), limit);
        }
        AtomicLong spent = used.get(key);
        if (spent == null) {
            return null;
        }
        return new KeyUsage(key, spent.get(), limit);
    }

    /** 全量用量视图（spec 214 §A / T578：topUsage 同序全量——健康/导出便利面）。 */
    public List<KeyUsage> usageAll() {
        return topUsage(Integer.MAX_VALUE);
    }

    /** 用量 top-N（used 降序，同 used key 字典序——输出稳定；未动用的 key 也可见）。 */
    public List<KeyUsage> topUsage(int n) {
        if (backend != null) {
            return limits.entrySet().stream()
                    .map(e -> new KeyUsage(e.getKey(), backendUsed(e.getKey()), e.getValue()))
                    .sorted((a, b) -> {
                        int byUsed = Long.compare(b.usedTokens(), a.usedTokens());
                        return byUsed != 0 ? byUsed : a.key().compareTo(b.key());
                    })
                    .limit(Math.max(0, n))
                    .toList();
        }
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

    /** 后端已用读取（异常按 0——观测面不放大后端故障）。 */
    private long backendUsed(String key) {
        try {
            return backend.usedTokens(key);
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    /** 窗口清零（export → reset 循环每窗口一份账；未注册 key no-op；耗尽态同清）。 */
    public void reset(String key) {
        if (backend != null && limits.containsKey(key)) {
            try {
                backend.reset(key);
            } catch (RuntimeException ignored) {
                // 观测/换窗面不放大后端故障——下次 reset 再试
            }
        }
        AtomicLong spent = used.get(key);
        if (spent != null) {
            spent.set(0);
        }
        exhausted.remove(key);
    }

    /** 全量窗口清零（spec 194 §A / T556：整窗换窗——所有 key 用量与耗尽态同清；限额表保留）。 */
    public void resetAll() {
        if (backend != null) {
            limits.keySet().forEach(key -> {
                try {
                    backend.reset(key);
                } catch (RuntimeException ignored) {
                    // 同 reset：换窗面不放大
                }
            });
        }
        used.values().forEach(counter -> counter.set(0));
        exhausted.clear();
    }

    /**
     * 耗尽态（spec 148 §A / T501）：已用 ≥ 限额，或上次扣减尝试越限被拒——
     * 「部分消耗永远凑不满」的诚实表达；预算闸据此拦截下一次调用。
     */
    public boolean isExhausted(String key) {
        if (exhausted.contains(key)) {
            return true;
        }
        Long limit = limits.get(key);
        if (backend != null) {
            return limit != null && backendUsed(key) >= limit;
        }
        AtomicLong spent = used.get(key);
        return spent != null && limit != null && spent.get() >= limit;
    }

    /** 在册 key 数（测试/健康面）。 */
    public int distinct() {
        return limits.size();
    }
}
