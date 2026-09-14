package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * per-host 并发上限闸（spec 1603 / T2357，Nginx {@code limit_conn} 思想）：同一 host
 * 的在飞 http_request 并发数超上限即拒绝（快速失败不排队——limit_conn 语义是拒绝，
 * 保护目标服务与本进程连接资源不被单 host 打满）。
 *
 * <p>计数实现：host → CAS 计数器。归零条目留在 map（零值 AtomicLong ≈ 数十字节；
 * host 集合受 SSRF 放行域与模型行为约束，量级可控——不做激进清理，避免释放竞争）。
 * {@code maxPerHost} ≤ 0 = 关（不建计数、恒放行——默认零行为变化）。
 * @since 1.0.0
 */
public final class PerHostConcurrencyGuard {

    private final int maxPerHost;
    private final ConcurrentHashMap<String, AtomicLong> inFlight = new ConcurrentHashMap<>();

    public PerHostConcurrencyGuard(int maxPerHost) {
        this.maxPerHost = maxPerHost;
    }

    /** 上限读数（≤0 = 关）。 */
    public int maxPerHost() {
        return maxPerHost;
    }

    /**
     * 进入 host（并发 +1）：当前在飞已达上限 → false（调用方快速失败）。
     * 关闭状态恒 true（零开销零行为）。
     */
    public boolean tryEnter(String host) {
        if (maxPerHost <= 0 || host == null) {
            return true;
        }
        AtomicLong count = inFlight.computeIfAbsent(host, k -> new AtomicLong());
        long now = count.incrementAndGet();
        if (now > maxPerHost) {
            count.decrementAndGet();
            return false;
        }
        return true;
    }

    /** 离开 host（并发 -1；与 tryEnter 成对调用，失败路径也必须离开）。 */
    public void exit(String host) {
        if (maxPerHost <= 0 || host == null) {
            return;
        }
        AtomicLong count = inFlight.get(host);
        if (count != null) {
            count.decrementAndGet();
        }
    }

    /** 当前在飞读数（观测/测试面；关闭状态恒 0）。 */
    public long inFlight(String host) {
        if (maxPerHost <= 0 || host == null) {
            return 0;
        }
        AtomicLong count = inFlight.get(host);
        return count == null ? 0 : count.get();
    }
}
