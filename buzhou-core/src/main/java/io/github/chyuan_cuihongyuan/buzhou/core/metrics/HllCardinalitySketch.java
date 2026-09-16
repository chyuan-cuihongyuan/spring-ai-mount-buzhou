package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * HyperLogLog 基数素描（spec 2001 / T3103 / impl 1552）——Redis HLL /
 * Flajolet 系思想：流式 distinct 计数不求全集——64 位确定性散列前 b 位选
 * 寄存器、尾部前导零 rank 取 max，调和平均出基数估计（小值域线性计数
 * 修正）；素描可合并（寄存器逐位 max——跨实例聚合语义）。相对误差界
 * 1.04/√m 与寄存器数开方成反比（b=12 即 ~1.6% 量级）。
 *
 * <p>确定性哈希（FNV-1a 64 + splitmix64 终结混合，无随机数——同输入同
 * 答案可回放）；synchronized 小临界区（offer/merge/estimate 原子）。
 */
public final class HllCardinalitySketch {

    /** 默认精度（b=12 → 4096 寄存器，理论相对误差界 ≈ 1.6%）。 */
    public static final int DEFAULT_PRECISION = 12;

    /** HLL 理论相对误差系数（1.04/√m——Flajolet 经典界）。 */
    public static final double ERROR_COEFFICIENT = 1.04d;

    /** 线性计数修正触发上界系数（raw ≤ 2.5m 时用 LC——HLL++ 小值域惯例）。 */
    public static final double SMALL_RANGE_FACTOR = 2.5d;

    private final int precision;
    private final byte[] registers;
    private long offered;

    /** 契约：precision ∈ [4,16]（fail-fast——16K 寄存器封顶，4 以下误差失控）。 */
    public HllCardinalitySketch(int precision) {
        if (precision < 4 || precision > 16) {
            throw new IllegalArgumentException("precision 须在 [4,16]：" + precision);
        }
        this.precision = precision;
        this.registers = new byte[1 << precision];
    }

    public HllCardinalitySketch() {
        this(DEFAULT_PRECISION);
    }

    /** 寄存器数 m = 2^b（只读面——容量与误差界换算用）。 */
    public synchronized int registerCount() {
        return registers.length;
    }

    /** 理论相对误差界：1.04/√m（构造期既定，不随数据变）。 */
    public synchronized double relativeErrorBound() {
        return ERROR_COEFFICIENT / Math.sqrt(registers.length);
    }

    /** 累计 offer 次数（含重复——与 estimate 的差即重复率对账面）。 */
    public synchronized long offeredCount() {
        return offered;
    }

    /** 加入一个元素（幂等——重复加入同值寄存器不变）。契约：value 非 null。 */
    public synchronized void offer(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value 不能为 null");
        }
        long h = DeterministicHash.hash64(value);
        int index = (int) (h >>> (Long.SIZE - precision));
        // 尾部 64−b 位的 rank：前导零数 + 1；全零封顶（64−b+1）
        int rank = Math.min(Long.SIZE - precision + 1,
                Long.numberOfLeadingZeros(h << precision) + 1);
        if (rank > registers[index]) {
            registers[index] = (byte) rank;
        }
        offered++;
    }

    /** 基数估计（distinct 计数）——调和平均 + 小值域线性计数修正。 */
    public synchronized long estimate() {
        int m = registers.length;
        double sum = 0.0d;
        int zeros = 0;
        for (byte r : registers) {
            sum += Math.pow(2.0d, -r);
            if (r == 0) {
                zeros++;
            }
        }
        double raw = alphaFor(m) * m * m / sum;
        if (raw <= SMALL_RANGE_FACTOR * m && zeros > 0) {
            return Math.round(m * Math.log((double) m / zeros));
        }
        return Math.round(raw);
    }

    /** 合并同精度素描（寄存器逐位 max——并集语义；A.merge(B) ≈ A∪B）。 */
    public synchronized void merge(HllCardinalitySketch other) {
        if (other == null) {
            throw new IllegalArgumentException("other 不能为 null");
        }
        if (other.precision != precision) {
            throw new IllegalArgumentException(
                    "精度不一致不可合并：" + precision + " vs " + other.precision);
        }
        for (int i = 0; i < registers.length; i++) {
            if (other.registers[i] > registers[i]) {
                registers[i] = other.registers[i];
            }
        }
        offered += other.offered;
    }

    /** HLL α_m 偏差修正（m ≤ 64 经典表值，大 m 渐近式）。 */
    private static double alphaFor(int m) {
        return switch (m) {
            case 16 -> 0.673d;
            case 32 -> 0.697d;
            case 64 -> 0.709d;
            default -> 0.7213d / (1.0d + 1.079d / m);
        };
    }

}
