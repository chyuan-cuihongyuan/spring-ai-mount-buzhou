package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * xorshift64 伪随机数（spec 9038 / W9077 / impl 2391）——Marsaglia
 * 2003 xorshift RNG 思想（「Xorshift RNGs」论文 13,7,17 三移位
 * 模板——众多语言内核同源）：**x^=(x<<13)、x^=(x>>>7)、
 * x^=(x<<17) 三移位异或推进——无乘法极简状态机**——LCG 低位
 * 短周期缺陷与 LFSR 单比特输出的中间形态：64 位全宽。种子 0
 * 拒绝（全零态死锁）；确定性纯函数可回放；非密码学明示
 * （统计面——勿用于密钥）。
 *
 * <p>与 SplitMix64（spec 9039）同族不同面：状态推进器 vs
 * 混合定序器（组合成强 RNG 的经典搭配——java.util.SplittableRandom
 * 内核同源）；与 DeterministicHash（同包）互补：哈希面 vs
 * 随机流面。
 */
public final class XorShift64 {

    private XorShift64() {
    }

    /**
     * 一步推进（13,7,17 移位模板——返回新状态即随机值）。
     *
     * @throws IllegalArgumentException 种子 0（全零态死锁拒绝）
     */
    public static long next(long state) {
        if (state == 0) {
            throw new IllegalArgumentException("种子非 0（全零态死锁）");
        }
        long x = state;
        x ^= x << 13;
        x ^= x >>> 7;
        x ^= x << 17;
        return x;
    }

    /** 区间 [0,bound) 均匀映射（拒绝采样无偏——模偏倚防御，OpenJDK 同款带判定）。 */
    public static long nextLong(long state, long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("上界为正（实际 " + bound + "）");
        }
        while (true) {
            long value = next(state);
            state = value;
            long result = value % bound;
            if (value - result + (bound - 1) < 0) {
                // 落入拒绝带（带符号溢出即无符号越界——重推）
                continue;
            }
            return result;
        }
    }
}
