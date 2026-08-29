package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 轮作用域工具结果 memo 表（spec 147 / T501，Hystrix request caching 借鉴）：
 * beforeTurn 清零（{@link TurnMemoHook}）——每轮白纸，零 TTL/容量/失效问题。
 * computeIfAbsent 复读秒回同值；<b>失败不 memo</b>（异常是可重试信号非可复用值）。
 */
public final class TurnMemo {

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    /** 复读命中即回存值；未命中执行 supplier 入表（异常原样上抛不入表）。 */
    public String computeIfAbsent(String key, Supplier<String> execution) {
        String hit = cache.get(key);
        if (hit != null) {
            hits.incrementAndGet();
            return hit;
        }
        misses.incrementAndGet();
        String value = execution.get();
        cache.put(key, value);
        return value;
    }

    /** 轮清零（TurnMemoHook beforeTurn 调用）。 */
    public void clear() {
        cache.clear();
    }

    /** 轮内命中数（观测面）。 */
    public long hits() {
        return hits.get();
    }

    /** 执行数（=未命中；观测面）。 */
    public long misses() {
        return misses.get();
    }

    /** 轮内表大小（观测面）。 */
    public int size() {
        return cache.size();
    }
}
