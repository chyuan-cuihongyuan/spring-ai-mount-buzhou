package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.function.DoubleUnaryOperator;

/**
 * 三分搜索（spec 7042 / U7285 / impl 2294）——单峰函数
 * 三分求极值思想（凸优化坐标下降同源）：**每次迭代比较
 * m1/m2 双探点、舍去不含极值的 1/3 区间** O(log((r−l)/ε))
 * ——网格扫描 ε 粒度（精度换算力爆炸）与牛顿法（需导数、
 * 多峰陷阱）两种病的折中解。单峰假设明示（多峰不承诺
 * 全局极值——诚实边界）；确定性纯函数；区间倒置/eps≤0
 * fail-fast。
 *
 * <p>与 InterpolationSearch（同包）同族不同面：离散存在性
 * 定位 vs 连续单峰极值。
 */
public final class TernarySearch {

    private TernarySearch() {
    }

    /** 单峰极大（f 在 [l,r] 先升后降；迭代至区间 < eps；越域 fail-fast）。 */
    public static double maximize(DoubleUnaryOperator f, double l, double r, double eps) {
        requireValid(f, l, r, eps);
        while (r - l > eps) {
            double m1 = l + (r - l) / 3;
            double m2 = r - (r - l) / 3;
            if (f.applyAsDouble(m1) < f.applyAsDouble(m2)) {
                l = m1;
            } else {
                r = m2;
            }
        }
        return (l + r) / 2;
    }

    /** 单峰极小（对称面）。 */
    public static double minimize(DoubleUnaryOperator f, double l, double r, double eps) {
        requireValid(f, l, r, eps);
        return maximize(x -> -f.applyAsDouble(x), l, r, eps);
    }

    private static void requireValid(DoubleUnaryOperator f, double l, double r, double eps) {
        if (f == null) {
            throw new IllegalArgumentException("函数非空");
        }
        if (l > r) {
            throw new IllegalArgumentException("区间倒置: [" + l + "," + r + "]");
        }
        if (eps <= 0 || Double.isNaN(eps)) {
            throw new IllegalArgumentException("eps 须为正: " + eps);
        }
    }
}
