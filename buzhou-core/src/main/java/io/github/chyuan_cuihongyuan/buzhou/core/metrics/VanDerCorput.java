package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 逆根序列（spec 11001 / Y11003 / impl 2454）——van der Corput 1935 思想
 * （SciPy qmc 同源；HaltonSequence 已占异面：一维基座 vs 多维合成）：**把
 * index 的 base 进制数位逆序置小数点后得 ψ_b(i)∈[0,1)**——一维低差异
 * 序列的最简基座（Halton 每维取一基的合成原料）。
 *
 * <p>长整型域逐位取除（无幂表依赖）；base≥2/count≥0 fail-fast；同入参
 * 复算确定。
 */
public final class VanDerCorput {

    private VanDerCorput() {
    }

    /**
     * 前 count 个逆根点（升序 index 0 起）。
     *
     * @param base 进制基（≥2）
     * @param count 点数（≥0）
     * @throws IllegalArgumentException base&lt;2/count&lt;0
     */
    public static double[] sequence(int base, int count) {
        if (base < 2) {
            throw new IllegalArgumentException("基 ≥2（实际 " + base + "）");
        }
        if (count < 0) {
            throw new IllegalArgumentException("点数非负（实际 " + count + "）");
        }
        double[] points = new double[count];
        for (int i = 0; i < count; i++) {
            points[i] = radicalInverse(base, i);
        }
        return points;
    }

    /** 数位逆序小数化：ψ_b(i)=Σ digit_k·b^{−(k+1)}。 */
    private static double radicalInverse(int base, long index) {
        double digit = 1.0 / base;
        double value = 0.0;
        long remaining = index;
        while (remaining > 0) {
            value += (remaining % base) * digit;
            remaining /= base;
            digit /= base;
        }
        return value;
    }
}
