package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 尾时延放大读面（spec 1894 / T2989 / impl 1495）——The Tail at
 * Scale：请求触达 N 个盒子、单盒分位 q，端到端「全盒都快」概率 =
 * q^N——幂次放大。q=0.99、N=100 → 仅 36.6%：每盒 99 分位全绿
 * 挡不住端到端大面积慢。SLO 分解即反解：target^(1/N)。
 *
 * <p>纯函数零状态；独立性假设（相关失效只会更差——诚实边界）。
 */
public final class TailAmplification {

    private TailAmplification() {
    }

    /**
     * 端到端概率：q^N——请求触达的全部盒子均在各自分位内的概率。
     * 契约：perBoxQuantile ∈ (0,1]、boxCount ≥ 1（fail-fast）。
     */
    public static double endToEndProbability(double perBoxQuantile, int boxCount) {
        validateQuantile(perBoxQuantile, "perBoxQuantile");
        if (boxCount < 1) {
            throw new IllegalArgumentException("boxCount 不能小于 1：" + boxCount);
        }
        return Math.pow(perBoxQuantile, boxCount);
    }

    /**
     * SLO 反解：端到端要 target，单盒需要 target^(1/N) 分位。
     * 契约：endToEndTarget ∈ (0,1]、boxCount ≥ 1（fail-fast）。
     */
    public static double requiredPerBoxQuantile(double endToEndTarget, int boxCount) {
        validateQuantile(endToEndTarget, "endToEndTarget");
        if (boxCount < 1) {
            throw new IllegalArgumentException("boxCount 不能小于 1：" + boxCount);
        }
        return Math.pow(endToEndTarget, 1.0 / boxCount);
    }

    private static void validateQuantile(double value, String name) {
        if (value <= 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " 须在 (0,1]：" + value);
        }
    }
}
