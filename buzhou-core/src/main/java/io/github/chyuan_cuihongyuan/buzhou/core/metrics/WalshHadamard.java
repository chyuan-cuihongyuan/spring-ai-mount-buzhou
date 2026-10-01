package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Walsh–Hadamard 变换（spec 11002 / Y11005 / impl 2455）——Walsh 1923/
 * Hadamard 1893 思想（量子计算/信号同源）：**原位蝶形 (a+b, a−b) 宽 1 起
 * 逐层倍增**的非归一 ±1 方波基正交变换——FFT 的方波镜像面（无三角函数
 * 全加减，O(n log n)）。对合性质 H(H(x))=n·x。
 *
 * <p>2 幂长 ≥2 约束；null/非 2 幂/非有限值 fail-fast；同输入逐位复算确定。
 */
public final class WalshHadamard {

    private WalshHadamard() {
    }

    /**
     * 原位非归一 Hadamard 变换。
     *
     * @param x 实信号（长度 2 的幂且 ≥2）
     * @throws IllegalArgumentException null/非 2 幂/非有限值
     */
    public static void transform(double[] x) {
        validate(x);
        for (int width = 1; width < x.length; width <<= 1) {
            for (int start = 0; start < x.length; start += width * 2) {
                for (int offset = start; offset < start + width; offset++) {
                    double a = x[offset];
                    double b = x[offset + width];
                    x[offset] = a + b;
                    x[offset + width] = a - b;
                }
            }
        }
    }

    private static void validate(double[] x) {
        if (x == null) {
            throw new IllegalArgumentException("信号非 null");
        }
        if (x.length < 2 || Integer.bitCount(x.length) != 1) {
            throw new IllegalArgumentException("长度为 2 的幂且 ≥2（实际 " + x.length + "）");
        }
        for (int i = 0; i < x.length; i++) {
            if (!Double.isFinite(x[i])) {
                throw new IllegalArgumentException("采样非有限（第 " + i + " 位 " + x[i] + "）");
            }
        }
    }
}
