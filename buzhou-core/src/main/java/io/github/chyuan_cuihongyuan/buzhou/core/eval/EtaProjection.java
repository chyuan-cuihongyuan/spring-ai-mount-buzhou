package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * 完成度 ETA 投影（spec 1834 / T2869 / impl 1435）——CI 进度条 / 带宽
 * 估计（rate = done/elapsed，ETA = remaining/rate）思想：长任务（评估
 * run、批量回放、大数据集导入）的「还要多久」由已观测速率线性外推——
 * 投影总时长与进度分数一起读，尾段速率漂移（投影前松后紧）靠调用方
 * 对比多时点投影发现。
 *
 * <p>纯函数零状态、只投影不采样（时钟与计数归宿主）；无时间基准（done=0
 * 或 elapsed=0）时 ETA/投影 -1 哨兵（诚实——不编速率）。
 */
public final class EtaProjection {

    private EtaProjection() {
    }

    /**
     * @param progress 完成度 done/total（0..1）
     * @param etaMillis 剩余时长投影（毫秒；已完成 0；无基准 -1 哨兵）
     * @param projectedTotalMillis 总时长投影 = elapsed + ETA（无基准 -1 哨兵）
     */
    public record Projection(double progress, long etaMillis, long projectedTotalMillis) {
    }

    /**
     * 投影入口。契约：totalUnits ≥ 1、0 ≤ doneUnits ≤ totalUnits、
     * elapsedMillis ≥ 0（fail-fast）；语义：done=0 或 elapsed=0（无速率
     * 基准）→ ETA/投影 -1；done=total → ETA 0；否则线性外推。
     */
    public static Projection estimate(long doneUnits, long totalUnits, long elapsedMillis) {
        if (totalUnits < 1) {
            throw new IllegalArgumentException("totalUnits 不能小于 1：" + totalUnits);
        }
        if (doneUnits < 0 || doneUnits > totalUnits) {
            throw new IllegalArgumentException(String.format(
                    "doneUnits 越界：%d（须在 [0,%d]）", doneUnits, totalUnits));
        }
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("elapsedMillis 不能为负：" + elapsedMillis);
        }
        double progress = (double) doneUnits / totalUnits;
        if (doneUnits == totalUnits) {
            return new Projection(progress, 0L, elapsedMillis);
        }
        if (doneUnits == 0 || elapsedMillis == 0) {
            return new Projection(progress, -1L, -1L);
        }
        long etaMillis = (long) Math.ceil((double) (totalUnits - doneUnits) * elapsedMillis
                / doneUnits);
        return new Projection(progress, etaMillis, elapsedMillis + etaMillis);
    }
}
