package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * prominence 峰检测（spec 10039 / X10079 / impl 2442）——SciPy find_peaks
 * 思想（信号处理同源）：**严格局部极大 + 等高线 prominence**——自峰向两侧爬升
 * 至更高峰或边界取段内谷底，prominence = 峰高 − max(左谷底, 右谷底)；对基座
 * 高度不敏感（只看显著度）。严格峰口径：两邻严格小（平台无峰明示）。
 *
 * <p>结果按索引升序；null 信号/负阈值 fail-fast；同输入复算确定。
 */
public final class PeakDetector {

    private PeakDetector() {
    }

    /**
     * 峰索引（prominence ≥ 阈值，升序）。
     *
     * @param signal 实信号（长度 ≥3 才可能有内点峰）
     * @param minProminence 显著度阈值（非负）
     * @throws IllegalArgumentException null 信号/负阈值/非有限值
     */
    public static int[] findPeaks(double[] signal, double minProminence) {
        if (signal == null) {
            throw new IllegalArgumentException("信号非 null");
        }
        if (minProminence < 0) {
            throw new IllegalArgumentException("阈值非负（实际 " + minProminence + "）");
        }
        for (int i = 0; i < signal.length; i++) {
            if (!Double.isFinite(signal[i])) {
                throw new IllegalArgumentException("采样非有限（第 " + i + " 位 " + signal[i] + "）");
            }
        }
        int[] candidatePeaks = new int[signal.length];
        int count = 0;
        for (int i = 1; i + 1 < signal.length; i++) {
            if (signal[i] > signal[i - 1] && signal[i] > signal[i + 1]
                    && prominence(signal, i) >= minProminence) {
                candidatePeaks[count++] = i;
            }
        }
        return java.util.Arrays.copyOf(candidatePeaks, count);
    }

    /** 等高线 prominence（峰高 − 两侧较大谷底）。 */
    private static double prominence(double[] signal, int peak) {
        double leftValley = valleyFloor(signal, peak, -1);
        double rightValley = valleyFloor(signal, peak, 1);
        return signal[peak] - Math.max(leftValley, rightValley);
    }

    /**
     * 单侧爬升谷底（step=−1 左/+1 右）：遇更高峰或边界停，段内最小值。
     */
    private static double valleyFloor(double[] signal, int peak, int step) {
        double floor = Double.POSITIVE_INFINITY;
        int i = peak + step;
        while (i >= 0 && i < signal.length && signal[i] <= signal[peak]) {
            floor = Math.min(floor, signal[i]);
            i += step;
        }
        return floor;
    }
}
