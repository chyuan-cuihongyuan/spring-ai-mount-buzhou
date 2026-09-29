package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * PAA 分段聚合近似（spec 8047 / V8093 / impl 2348）——
 * Keogh 2001 PAA 思想：**等宽分段取均值**（w 段，余数元素
 * 并入末段）——抽稀丢形状（降维保形需求）的病解。
 * w∈[1,n] 越域 fail-fast；确定性纯函数。
 *
 * <p>与 SaxCodec（spec 8046）同族不同面：分段均值连续降维
 * vs 符号离散化。
 */
public final class PaaCodec {

    private PaaCodec() {
    }

    /** 分段均值（段数 w；null/空/w 越域 fail-fast）。 */
    public static double[] transform(double[] series, int segments) {
        if (series == null || series.length == 0) {
            throw new IllegalArgumentException("序列非空");
        }
        if (segments < 1 || segments > series.length) {
            throw new IllegalArgumentException("w ∈[1,n]（实际 " + segments + "）");
        }
        double[] reduced = new double[segments];
        double base = (double) series.length / segments;
        for (int segment = 0; segment < segments; segment++) {
            int from = (int) Math.floor(segment * base);
            int to = segment == segments - 1
                    ? series.length
                    : (int) Math.floor((segment + 1) * base);
            double sum = 0;
            for (int i = from; i < to; i++) {
                sum += series[i];
            }
            reduced[segment] = sum / (to - from);
        }
        return reduced;
    }
}
