package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

/**
 * 缓存牺牲率（spec 1866 / T2933 / impl 1467）——体系结构缓存分析惯例
 *（sacrifice ratio / thrashing）：每次插入**驱逐多少存量**（牺牲率 =
  插入期驱逐数/插入数——全联缓存为 0，容量不足时攀升）——与命中率合诊
 * **颠簸**（thrashing：牺牲率高 + 命中率低 = 缓存太小，插入的还没用就
  被下一插入挤走——白忙；该扩容不是调 TTL）。
 *
 * <p>纯函数零状态、只读不调参（容量决策归宿主）。
 */
public final class CacheSacrificeRatio {

    private CacheSacrificeRatio() {
    }

    /** 缓存账契约：四计数 ≥ 0。 */
    public record CacheAccount(long inserts, long evictionsOnInsert,
                               long hits, long misses) {

        public CacheAccount {
            if (inserts < 0 || evictionsOnInsert < 0 || hits < 0 || misses < 0) {
                throw new IllegalArgumentException(String.format(
                        "非法缓存账：inserts=%d, evictions=%d, hits=%d, misses=%d",
                        inserts, evictionsOnInsert, hits, misses));
            }
        }

        /** 牺牲率 = 插入期驱逐/插入（零插入 -1 哨兵——全联理想 0）。 */
        public double sacrificeRatio() {
            return inserts == 0 ? -1d : (double) evictionsOnInsert / inserts;
        }

        /** 命中率（零查找 -1 哨兵）。 */
        public double hitRate() {
            long lookups = hits + misses;
            return lookups == 0 ? -1d : (double) hits / lookups;
        }

        /**
         * 颠簸判定：牺牲率 ≥ sacrificeThreshold 且命中率 &lt; hitThreshold
         *（双条件——单看牺牲率可能是正常满缓存换血、单看命中率低可能是
         * 工作集本就散）；任一读数为哨兵（零插入/零查找——无数据）时
         * 无判（false——无据不定罪）。阈值 ∈ [0,1] 契约。
         */
        public boolean thrashing(double sacrificeThreshold, double hitThreshold) {
            if (Double.isNaN(sacrificeThreshold) || sacrificeThreshold < 0
                    || sacrificeThreshold > 1 || Double.isNaN(hitThreshold)
                    || hitThreshold < 0 || hitThreshold > 1) {
                throw new IllegalArgumentException(String.format(
                        "阈值须在 [0,1]：sacrifice=%s, hit=%s",
                        sacrificeThreshold, hitThreshold));
            }
            double sacrifice = sacrificeRatio();
            double hit = hitRate();
            if (sacrifice < 0 || hit < 0) {
                return false;
            }
            return sacrifice >= sacrificeThreshold && hit < hitThreshold;
        }
    }
}
