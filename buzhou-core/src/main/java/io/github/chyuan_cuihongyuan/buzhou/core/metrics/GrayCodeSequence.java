package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * 格雷码序列（spec 7039 / U7279 / impl 2291）——Frank Gray
 * 1953 思想（旋转编码器/模拟量数字化同源）：**二进制反射
 * 格雷码 g(i)=i⊕(i>>1)**——相邻码恰一位差（机械计数多位
 * 同时翻转的瞬态歧义病解）。全序列/单项/逆变换（grayToIndex
 * 逐位前缀异或）三面；确定性纯函数；n≤30（序列 2^n 爆炸
 * ——全序列面明示拒绝，单项面不限）。
 *
 * <p>与 BitPacking（message）同族不同面：位打包存储 vs
 * 位翻转序。
 */
public final class GrayCodeSequence {

    private GrayCodeSequence() {
    }

    /** 单项格雷码（i≥0；负数 fail-fast）。 */
    public static long encode(long i) {
        if (i < 0) {
            throw new IllegalArgumentException("序号须非负: " + i);
        }
        return i ^ (i >> 1);
    }

    /** 逆变换（格雷码→序号，逐位前缀异或；负数 fail-fast）。 */
    public static long decode(long gray) {
        if (gray < 0) {
            throw new IllegalArgumentException("格雷码须非负: " + gray);
        }
        long index = gray;
        while (gray > 1) {
            gray >>= 1;
            index ^= gray;
        }
        return index;
    }

    /** n 位全序列（2^n 项；n∈[1,30]——序列爆炸明示拒绝）。 */
    public static List<Long> sequence(int n) {
        if (n < 1 || n > 30) {
            throw new IllegalArgumentException("位宽须在 [1,30]: " + n);
        }
        List<Long> codes = new ArrayList<>();
        for (long i = 0; i < (1L << n); i++) {
            codes.add(encode(i));
        }
        return codes;
    }
}
