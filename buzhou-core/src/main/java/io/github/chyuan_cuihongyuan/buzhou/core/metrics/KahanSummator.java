package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Kahan 补偿求和（spec 7032 / U7265 / impl 2284）——Kahan
 * 1965 补偿求和思想：**低位移存被舍入位，下一轮补回**
 * ——朴素连加浮点误差随 n 线性放大（10⁶ 次加法误差可
 * 达 ulp×n 量级）的病解（误差 O(1) 独立于 n——确定性
 * 每一步可复现）。double 域（不假装任意精度——明示）。
 *
 * <p>与 WelfordAccumulator（同包）同族不同面：方差单遍
 * 稳定累计 vs 求和补偿。
 */
public final class KahanSummator {

    private double sum;
    private double compensation;
    private long count;

    /** 加一项（补偿位回收）。 */
    public void add(double value) {
        double adjusted = value - compensation;
        double next = sum + adjusted;
        compensation = (next - sum) - adjusted;
        sum = next;
        count++;
    }

    /** 和读数。 */
    public double value() {
        return sum;
    }

    /** 计数读数。 */
    public long count() {
        return count;
    }
}
