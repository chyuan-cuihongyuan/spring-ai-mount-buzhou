package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * PCG-XSH-RR 感知置换随机数（spec 10027 / X10055 / impl 2430）——
 * O'Neill 2014 思想（「LCG 状态+输出置换」——pcg-random/SplitMix64
 * 家族互补）：**128 位容量 LCG（64 位状态×乘子+流增量）+XSH-RR
 * 输出置换（异或移位+右旋）——小状态通过的统计检验面**（朴素
 * LCG 低位短周期的病解互补）。流 ID 分离序列（(stream≪1)|1 奇增
 * 量）；种子预热一步（标准 pcg32 口径）；同种子同流确定。
 */
public final class PcgXshRr {

    /** PCG 默认乘子（2⁶⁴ 模 LCG 标准常数）。 */
    private static final long MULTIPLIER = 6364136223846793005L;

    /** 输出置换异或移位量（XSH：X=异或 S=移位）。 */
    private static final int XOR_SHIFT_HIGH = 18;
    private static final int XOR_SHIFT_LOW = 27;

    /** 旋转让位点（状态最高 5 位）。 */
    private static final int ROTATE_BITS = 59;

    /** 双精度取样的 24 位定点除数。 */
    private static final double DOUBLE_SCALE = 0x1p-24;

    private long state;
    private final long increment;

    /**
     * 构造（种子+流 ID；预热一步）。
     */
    public PcgXshRr(long seed, long streamId) {
        this.increment = (streamId << 1) | 1;
        this.state = seed + increment;
        nextInt();
    }

    /** 32 位无符号输出（long 承载 [0,2³²)）。 */
    public long nextInt() {
        long oldState = state;
        state = oldState * MULTIPLIER + increment;
        int xorshifted = (int) (((oldState >>> XOR_SHIFT_HIGH) ^ oldState) >>> XOR_SHIFT_LOW);
        int rotation = (int) (oldState >>> ROTATE_BITS);
        return Integer.rotateRight(xorshifted, rotation) & 0xFFFFFFFFL;
    }

    /** [0,1) 双精度（24 位定点）。 */
    public double nextDouble() {
        return (nextInt() >>> 8) * DOUBLE_SCALE;
    }

    /** 64 位输出（两次 32 位拼合）。 */
    public long nextLong() {
        long high = nextInt();
        long low = nextInt();
        return (high << 32) | low;
    }
}
