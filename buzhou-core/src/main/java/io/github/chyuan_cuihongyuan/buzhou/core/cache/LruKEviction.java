package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * LRU-K 驱逐（spec 1898 / T2997 / impl 1499）——PostgreSQL 缓冲池
 * LRU-K 语义：按「倒数第 K 次访问时间」淘汰——顺序扫描（每键只碰
 * 一次）不再把真热数据冲出缓存。历史不足 K 次的键其第 K 次视作
 * -∞（未证热度先让路）；K=1 退化为传统 LRU。
 *
 * <p>持态小 keeper；容量管理归调用方（本面只回答「逐谁」）。
 */
public final class LruKEviction {

    private final int k;
    private final Map<String, long[]> history = new HashMap<>();

    /**
     * @param k 新近度深度（≥ 1；1 = 传统 LRU，fail-fast）
     */
    public LruKEviction(int k) {
        if (k < 1) {
            throw new IllegalArgumentException("k 不能小于 1：" + k);
        }
        this.k = k;
    }

    /**
     * 记录一次访问。契约：nowMillis 相对上次访问不倒退（fail-fast——
     * 时间倒退即畸形时钟）。
     */
    public void record(String key, long nowMillis) {
        if (nowMillis < 0) {
            throw new IllegalArgumentException("时间不能为负：" + nowMillis);
        }
        long[] ring = history.get(key);
        if (ring == null) {
            ring = new long[k];
            history.put(key, ring);
        }
        long last = ring[k - 1];
        if (last > nowMillis) {
            throw new IllegalArgumentException(String.format(
                    "时间倒退：%d → %d", last, nowMillis));
        }
        long[] next = new long[k];
        System.arraycopy(ring, 1, next, 0, k - 1);
        next[k - 1] = nowMillis;
        history.put(key, next);
    }

    /**
     * 选驱逐受害者：候选中「倒数第 K 次访问时间」最早者；历史不足
     * K 次按 -∞（新键优先让路）。候选为空返回 null。契约：候选键
     * 均已 record 过（未记录键忽略）。
     */
    public String evictVictim(Iterable<String> candidates) {
        String victim = null;
        long victimKeyTime = Long.MAX_VALUE;
        for (String key : candidates) {
            long[] ring = history.get(key);
            if (ring == null) {
                continue;
            }
            long kth = ring[0]; // 倒数第 K 次（环最旧位）；不足 K 次者该位为初始 -∞ 哨兵
            long effective = filled(ring) ? kth : Long.MIN_VALUE;
            if (victim == null || effective < victimKeyTime) {
                victim = key;
                victimKeyTime = effective;
            }
        }
        return victim;
    }

    /** 驱逐后清理历史（调用方真正淘汰键时调用，防止幽灵历史）。 */
    public void forget(String key) {
        history.remove(key);
    }

    /** 环是否已填满 K 个真实时间戳（初始哨兵为 -1 表示缺位）。 */
    private boolean filled(long[] ring) {
        for (long t : ring) {
            if (t < 0) {
                return false;
            }
        }
        return true;
    }
}
