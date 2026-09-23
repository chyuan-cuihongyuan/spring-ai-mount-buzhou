package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Count-Min 素描（spec 4001 / T6003 / impl 2102）——流式点查计数
 * 概率近似思想（Cormode-Muthukrishnan 2005；Cassandra/ClickHouse
 * 内建同款）：d 行 × w 列计数矩阵，每行独立散列落一格；increment
 * 全行加 n，estimate 取 d 行最小——非负增量下**只高估不低估**（单侧
 * 误差），误差以概率 1−δ 界于 ε·N（d=⌈ln(1/δ)⌉、w=⌈e/ε⌉ 配平）。
 *
 * <p>「每个键精确记一个 long」的内存病在基数爆炸场景（工具签名/错误
 * 签名/查询词频）不可持续——本件以 O(d·w) 固定内存换全键可查的近似
 * 计数。行散列自 {@link DeterministicHash} 基值按行乘黄金比步进
 * splitmix 终结（确定性可回放，同输入同输出）。
 *
 * <p>与 MisraGriesSketch 成对：彼件确定性找频繁项（只保 k 个候选），
 * 本件任意键可查但只近似——「要名单」用彼件、「要点查」用本件；
 * 与 HllCardinalitySketch 互补（基数去重计数 vs 带重计数）。
 */
public final class CountMinSketch {

    /** 行散列步进（splitmix64 黄金比常数，行间去相关）。 */
    private static final long GOLDEN = 0x9e3779b97f4a7c15L;

    private final int depth;
    private final int width;
    private final long[][] table;
    private long totalCount;

    /** 定构（depth 行 × width 列；depth≥1、width≥1 否则 fail-fast）。 */
    public CountMinSketch(int depth, int width) {
        if (depth < 1 || width < 1) {
            throw new IllegalArgumentException("depth/width 必须 ≥1（实际 " + depth + "×" + width + "）");
        }
        this.depth = depth;
        this.width = width;
        this.table = new long[depth][width];
    }

    /** 键计数 +n（n≥0；全行加记）。 */
    public void increment(String key, long n) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
        if (n < 0) {
            throw new IllegalArgumentException("n 必须 ≥0（负值走 decay 语义另议）");
        }
        long base = DeterministicHash.hash64(key);
        for (int row = 0; row < depth; row++) {
            table[row][columnOf(row, base)] += n;
        }
        totalCount += n;
    }

    /** 键计数估计：d 行取最小——非负增量下恒 ≥ 真值（单侧误差）。 */
    public long estimate(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key 非空");
        }
        long base = DeterministicHash.hash64(key);
        long min = Long.MAX_VALUE;
        for (int row = 0; row < depth; row++) {
            min = Math.min(min, table[row][columnOf(row, base)]);
        }
        return min;
    }

    /** 总增量守恒账（Σ increment n——与行/列噪声无关的精确值）。 */
    public long totalCount() {
        return totalCount;
    }

    /** 行数读数。 */
    public int depth() {
        return depth;
    }

    /** 列数读数。 */
    public int width() {
        return width;
    }

    /** 行内列下标：基值按行黄金比步进 + splitmix 终结，取无符号模。 */
    private int columnOf(int row, long base) {
        long z = base + (row + 1) * GOLDEN;
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        z = z ^ (z >>> 31);
        return (int) Long.remainderUnsigned(z, width);
    }
}
