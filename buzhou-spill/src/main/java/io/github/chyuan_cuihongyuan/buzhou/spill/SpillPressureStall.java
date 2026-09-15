package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.List;

/**
 * Spill 压力失速读面（spec 1801 / T2803 / impl 1402）——Linux 内核 PSI
 * （Pressure Stall Information）思想：资源压力不看水位而看「谁在等」——
 * some 档（至少一个任务失速）量化吞吐损失，full 档（全部非空闲任务失速）
 * 量化进度损失。映射到 spill：每个采样窗记录（活跃会话数，因 spill 溢写/
 * 回读被拖住的会话数），somePct 高 = 局部会话在为溢写付延迟税，fullPct
 * 高 = 整个运行时都在等磁盘——两档分開回答「该加内存阈值余量还是该换
 * 更快存储」。
 *
 * <p>纯函数零状态、只读不裁决（阈值调整归宿主）；样本即事实，采集口径
 * （轮边界/溢写 tick）由调用方声明。
 */
public final class SpillPressureStall {

    private SpillPressureStall() {
    }

    /**
     * 单采样窗事实：activeSessions 个活跃会话中 stalledSessions 个因 spill
     * 失速（等溢写/等回读）。契约：两者非负且 stalled ≤ active；active 为 0
     * 的空闲窗合法（进分母不进分子）。
     */
    public record StallSample(int activeSessions, int stalledSessions) {

        public StallSample {
            if (activeSessions < 0 || stalledSessions < 0 || stalledSessions > activeSessions) {
                throw new IllegalArgumentException(
                        "非法采样窗：active=" + activeSessions + ", stalled=" + stalledSessions
                                + "（要求 0 ≤ stalled ≤ active）");
            }
        }
    }

    /**
     * @param windows      观测窗总数（分母）
     * @param someWindows  至少一个会话失速的窗数（some 档分子）
     * @param fullWindows  活跃全会话失速的窗数（full 档分子；active=0 空闲窗永不计入）
     * @param worstStalled 失速会话数峰值
     * @param worstActive  峰值所在窗的活跃会话数（0 表示无观测窗）
     */
    public record PsiReport(int windows, long someWindows, long fullWindows,
                            int worstStalled, int worstActive) {

        /** some 档占比（空观测 -1 哨兵）。 */
        public double somePct() {
            return windows == 0 ? -1d : (double) someWindows / windows;
        }

        /** full 档占比（空观测 -1 哨兵）。 */
        public double fullPct() {
            return windows == 0 ? -1d : (double) fullWindows / windows;
        }

        /** 峰值窗失速面 = worstStalled/worstActive（无观测或峰值窗空闲 -1 哨兵）。 */
        public double worstStallRatio() {
            return worstActive == 0 ? -1d : (double) worstStalled / worstActive;
        }
    }

    /**
     * 失速账目入口。null 按空表（哨兵 -1）；样本逐窗核契约，畸形即 fail-fast
     * （读面不吞脏事实）。
     */
    public static PsiReport analyze(List<StallSample> samples) {
        List<StallSample> window = samples == null ? List.of() : samples;
        long some = 0;
        long full = 0;
        int worstStalled = 0;
        int worstActive = 0;
        for (StallSample s : window) {
            if (s.stalledSessions() >= 1) {
                some++;
            }
            if (s.activeSessions() >= 1 && s.stalledSessions() == s.activeSessions()) {
                full++;
            }
            if (s.stalledSessions() > worstStalled
                    || (s.stalledSessions() == worstStalled && s.activeSessions() > worstActive)) {
                worstStalled = s.stalledSessions();
                worstActive = s.activeSessions();
            }
        }
        return new PsiReport(window.size(), some, full, worstStalled, worstActive);
    }
}
