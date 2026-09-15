package io.github.chyuan_cuihongyuan.buzhou.core.cache;

/**
 * 频次衰减竞速（spec 1858 / T2917 / impl 1459）——Redis LFU counter
 * decay 思想：访问频次计数器周期性减半——**老热点会自然退烧**，新热点
 * 按每周期命中数线性增长；两者相向而行，新热点顶掉老热点需要的周期数
 * （overtake period）就是缓存自适应性的读数：顶替周期太长 = 衰减太慢
 *（老热点赖着）、太短 = 抖动（谁也坐不稳）。纯数学，无随机。
 *
 * <p>纯函数零状态；衰减口径「每周期减半、向下取整、封底 0」。
 */
public final class FrequencyDecay {

    /** 衰减竞速求解的周期上限（保险丝——newHits ≥ 1 时数学上必在
     * log2(old)+1 周期内分出，上限只是防御）。 */
    public static final int MAX_RACE_PERIODS = 1024;

    private FrequencyDecay() {
    }

    /**
     * 衰减后计数：每周期减半（向下取整），封底 0。契约：counter ≥ 0、
     * periods ≥ 0（fail-fast）。
     */
    public static long decayed(long counter, long periods) {
        if (counter < 0 || periods < 0) {
            throw new IllegalArgumentException(String.format(
                    "入参不能为负：counter=%d, periods=%d", counter, periods));
        }
        long value = counter;
        for (long i = 0; i < periods && value > 0; i++) {
            value /= 2;
        }
        return value;
    }

    /**
     * 顶替周期：最小 t ≥ 1 使 newHitsPerPeriod × t &gt; decayed(counter, t)
     *——新热点（每周期 newHits 次命中）累计超越老热点（衰减中）的周期数。
     * 契约：counter ≥ 0、newHitsPerPeriod ≥ 0；newHits = 0 或不可达 → -1
     * 哨兵（永不过顶）。
     */
    public static long overtakePeriod(long counter, long newHitsPerPeriod) {
        if (counter < 0 || newHitsPerPeriod < 0) {
            throw new IllegalArgumentException(String.format(
                    "入参不能为负：counter=%d, hits=%d", counter, newHitsPerPeriod));
        }
        if (newHitsPerPeriod == 0) {
            return -1;
        }
        for (long t = 1; t <= MAX_RACE_PERIODS; t++) {
            if (newHitsPerPeriod * t > decayed(counter, t)) {
                return t;
            }
        }
        return -1;
    }
}
