package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * 乱序接纳窗（spec 1899 / T2999 / impl 1500）——QuestDB
 * out-of-order 语义：时间线水位（最新事件时间）向后开一扇宽 W 的
 * 接纳窗——窗内迟到数据就地接纳、窗前数据拒收（历史已定稿）、
 * 窗沿之上即顺序到达。窗宽是「乱序容忍 ↔ 定稿速度」的换挡杆。
 *
 * <p>纯函数零状态；水位推进单调（取大不回退）。
 */
public final class OutOfOrderWindow {

    private OutOfOrderWindow() {
    }

    /** 接纳判定：FRESH 顺序到达 / LATE_ACCEPTED 窗内迟到 / TOO_OLD 窗前拒收。 */
    public enum Accept { FRESH, LATE_ACCEPTED, TOO_OLD }

    /**
     * 三态判定：eventTime ≥ highWater → FRESH；≥ highWater − window
     * → LATE_ACCEPTED（窗沿含下）；否则 TOO_OLD。契约：window ≥ 0、
     * 时点 ≥ 0（fail-fast）。
     */
    public static Accept classify(long eventTimeMillis, long highWaterMillis,
                                  long windowMillis) {
        if (windowMillis < 0) {
            throw new IllegalArgumentException("window 不能为负：" + windowMillis);
        }
        if (eventTimeMillis < 0 || highWaterMillis < 0) {
            throw new IllegalArgumentException(String.format(
                    "时点不能为负：eventTime=%d, highWater=%d",
                    eventTimeMillis, highWaterMillis));
        }
        if (eventTimeMillis >= highWaterMillis) {
            return Accept.FRESH;
        }
        if (eventTimeMillis >= highWaterMillis - windowMillis) {
            return Accept.LATE_ACCEPTED;
        }
        return Accept.TOO_OLD;
    }

    /**
     * 水位推进：取大不回退（迟到事件不拖低水位）。
     */
    public static long advance(long highWaterMillis, long eventTimeMillis) {
        if (eventTimeMillis < 0) {
            throw new IllegalArgumentException(
                    "eventTime 不能为负：" + eventTimeMillis);
        }
        return Math.max(highWaterMillis, eventTimeMillis);
    }
}
