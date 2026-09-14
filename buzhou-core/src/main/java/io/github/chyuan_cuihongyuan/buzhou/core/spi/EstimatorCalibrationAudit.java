package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Token 估算校准偏差审计（spec 819 / T1139，预测校准思想——估算值 vs 真值
 * 的系统性偏差量化）：启发式估算器（{@code CharHeuristicTokenEstimator}）
 * 与模型回报的真实 usage 成对入账——均值相对误差/偏高偏低占比/近窗 P95
 * 绝对误差——「预算按估算设、账单按真实来」的缺口从感觉变数字。
 *
 * <p>纯记账：{@code record(estimated, actual)}（负值对忽略）；相对误差 =
 * (estimated − actual)/actual（正=高估——预算偏松、负=低估——预算偏紧，
 * 口径 javadoc 声明）；绝对误差样本 = |estimated − actual|。近窗
 * {@value #WINDOW} 对（挤出最老）。actual=0 的对忽略（相对误差无定义）。
 * @since 1.0.0
 */
public final class EstimatorCalibrationAudit {

    /** 近窗样本容量。 */
    public static final int WINDOW = 128;

    /** 不可变校准读数。 */
    public record Calibration(long pairs, double meanRelativeError, double biasOverRatio,
                              double recentP95AbsError, boolean empty) {
    }

    private static final class Pair {
        final double relativeError;
        final double absError;

        Pair(double relativeError, double absError) {
            this.relativeError = relativeError;
            this.absError = absError;
        }
    }

    private final Deque<Pair> window = new ArrayDeque<>(WINDOW);
    private final ReentrantLock lock = new ReentrantLock();
    private long pairs;
    private double relativeSum;
    private long overCount;
    private long underCount;

    /** 记一对（估算, 实际）；两值均 ≥0 且 actual &gt; 0 才计入。 */
    public void record(int estimated, int actual) {
        if (estimated < 0 || actual <= 0) {
            return;
        }
        double relativeError = (double) (estimated - actual) / actual;
        lock.lock();
        try {
            pairs++;
            relativeSum += relativeError;
            if (estimated > actual) {
                overCount++;
            } else if (estimated < actual) {
                underCount++;
            }
            if (window.size() >= WINDOW) {
                window.pollFirst();
            }
            window.addLast(new Pair(relativeError, Math.abs(estimated - actual)));
        } finally {
            lock.unlock();
        }
    }

    /** 校准读数（无样本时 empty=true 全 0）。 */
    public Calibration audit() {
        lock.lock();
        try {
            if (pairs == 0) {
                return new Calibration(0, 0, 0, 0, true);
            }
            List<Double> recentAbs = new ArrayList<>(window.size());
            for (Pair p : window) {
                recentAbs.add(p.absError);
            }
            recentAbs.sort(Double::compare);
            int rank = (int) Math.ceil(0.95 * recentAbs.size());
            double p95 = recentAbs.get(Math.min(Math.max(rank, 1), recentAbs.size()) - 1);
            return new Calibration(pairs, relativeSum / pairs,
                    (double) overCount / pairs, p95, false);
        } finally {
            lock.unlock();
        }
    }

    /** 偏低估算占比（低估会顶爆预算——运维重点口径）。 */
    public double biasUnderRatio() {
        lock.lock();
        try {
            return pairs == 0 ? 0 : (double) underCount / pairs;
        } finally {
            lock.unlock();
        }
    }
}
