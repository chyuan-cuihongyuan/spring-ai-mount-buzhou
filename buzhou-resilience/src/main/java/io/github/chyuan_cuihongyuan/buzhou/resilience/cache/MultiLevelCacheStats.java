package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

/**
 * 多级缓存命中读面（spec 1830 / T2861 / impl 1431）——Caffeine
 * multi-level / CPU L1-L2 缓存层级思想：L1（进程内，快而小）挡不住的漏
 * 到 L2（跨实例共享 store，慢而大），再漏才是回源——**逐层命中率**与
 * 「L2 命中里有多少本该 L1 拦下」（L1 失职率）分开读：L1 撑爆该升容量、
 * L1 失职高该查预热/失效逻辑、全层漏穿该查键口径。
 *
 * <p>纯函数零状态、只读不裁决（层级配置归宿主）。
 */
public final class MultiLevelCacheStats {

    private MultiLevelCacheStats() {
    }

    /**
     * @param requests 总请求数（四路合计）
     * @param l1Hits   L1 命中
     * @param l2Hits   L1 漏穿后 L2 命中
     * @param misses   全层漏穿（回源）
     */
    public record CacheReport(long requests, long l1Hits, long l2Hits, long misses) {

        public CacheReport {
            boolean malformed = requests < 0 || l1Hits < 0 || l2Hits < 0 || misses < 0
                    || l1Hits + l2Hits + misses != requests;
            if (malformed) {
                throw new IllegalArgumentException(String.format(
                        "非法缓存账：requests=%d, l1=%d, l2=%d, miss=%d（要求非负且四路合计=requests）",
                        requests, l1Hits, l2Hits, misses));
            }
        }

        /** L1 命中率（零请求 -1 哨兵）。 */
        public double l1HitRate() {
            return requests == 0 ? -1d : (double) l1Hits / requests;
        }

        /** 联合命中率 = (l1+l2)/requests（零请求 -1 哨兵）。 */
        public double combinedHitRate() {
            return requests == 0 ? -1d : (double) (l1Hits + l2Hits) / requests;
        }

        /** L1 失职率 = L2 命中占非回源请求比——L2 命中里本可 L1 拦下的份额
         *（l1+l2=0 时 -1 哨兵）。 */
        public double l1DerelictionRate() {
            long served = l1Hits + l2Hits;
            return served == 0 ? -1d : (double) l2Hits / served;
        }
    }

    /** 读面入口。契约在报告构造器（fail-fast）。 */
    public static CacheReport report(long l1Hits, long l2Hits, long misses) {
        return new CacheReport(l1Hits + l2Hits + misses, l1Hits, l2Hits, misses);
    }
}
