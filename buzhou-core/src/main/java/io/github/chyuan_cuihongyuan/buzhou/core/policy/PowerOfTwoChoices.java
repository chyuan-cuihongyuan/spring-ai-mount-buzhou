package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.Random;

/**
 * PowerOfTwoChoices 二择一负载均衡（spec 6047 / T6293 / impl 2247）——
 * Mitzenmacher 二次选择思想：**随机抽两个候选、放较轻者**——
 * 装载指数从单次随机的 O(ln n/ln ln n) 降到 O(ln ln n)（非对称
 * 性引理，"the power of two random choices"）——单次随机哈希
 * 长尾热桶（尾延迟放大）与全局最小扫描（O(n) 每次放置）两种病
 * 的同解。并列取下标小者（完全确定无随机残余）；种子化 Random
 * （同种子同放置序列——确定性可回放）。
 *
 * <p>与 ConsistentHashRing（同包）同族不同面：键映射粘滞
 * （环不移位）vs 负载感知放置（每次查询即时均衡）；与
 * LeastLoaded 全局扫描不同面：全量比较 vs 二样本地比较。
 */
public final class PowerOfTwoChoices {

    private final long[] loads;
    private final Random rng;
    private long placed;

    /** bins∈[1,2^20]；种子注入（同种子同序列）。越域 fail-fast。 */
    public PowerOfTwoChoices(int bins, long seed) {
        if (bins < 1 || bins > (1 << 20)) {
            throw new IllegalArgumentException("桶数须在 [1,2^20]: " + bins);
        }
        this.loads = new long[bins];
        this.rng = new Random(seed);
    }

    /**
     * 放置一个负载单元：随机抽两个<b>不同</b>桶（同桶重抽——无放回
     * 二择，等效单次随机击中重桶的病根被排除），取装载较小者
     * （并列取下标小者），装载 +1 并返回桶下标。
     */
    public int place() {
        int i = rng.nextInt(loads.length);
        int j = rng.nextInt(loads.length);
        if (j == i && loads.length > 1) {
            j = (j + 1 + rng.nextInt(loads.length - 1)) % loads.length;
        }
        int choice;
        if (loads[i] < loads[j]) {
            choice = i;
        } else if (loads[j] < loads[i]) {
            choice = j;
        } else {
            choice = Math.min(i, j);
        }
        loads[choice]++;
        placed++;
        return choice;
    }

    /** 桶装载读数（越域 fail-fast）。 */
    public long loadOf(int bin) {
        if (bin < 0 || bin >= loads.length) {
            throw new IllegalArgumentException("桶下标越域 [0," + loads.length + "): " + bin);
        }
        return loads[bin];
    }

    /** 最大桶装载（非对称性显形面）。 */
    public long maxLoad() {
        long max = 0;
        for (long load : loads) {
            max = Math.max(max, load);
        }
        return max;
    }

    /** 桶数读数。 */
    public int binCount() {
        return loads.length;
    }

    /** 累计放置读数（守恒面——各桶装载之和）。 */
    public long placed() {
        return placed;
    }
}
