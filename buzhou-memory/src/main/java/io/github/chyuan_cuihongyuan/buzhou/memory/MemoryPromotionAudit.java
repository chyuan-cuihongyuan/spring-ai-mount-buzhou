package io.github.chyuan_cuihongyuan.buzhou.memory;

import java.util.List;

/**
 * 记忆层代晋升审计（spec 1803 / T2807 / impl 1404）——JVM 分代 GC 晋升诊断
 * 思想：年轻代（便宜高频 GC）与老年代（贵低频 GC）之间看「晋升率」——
 * 晋升率健康 = 年轻代过滤掉短命对象；**过早晋升**（当轮全数晋升、零原地
 * 保留）= 年轻代缓冲形同虚设，经典容量错配信号。映射到记忆分层：微压缩
 * = 年轻代（便宜、轮界高频）、九段摘要 = 老年代（贵、低频）、直接归档 =
 * 越级冷存。晋升率常高 = 微压缩策略没拦住该拦的；过早晋升周期堆积 =
 * 摘要被短命内容灌水，信息密度稀释。
 *
 * <p>纯函数零状态：输入为逐压缩周期事实（口径由调用方声明），只读不裁决
 * （分层策略调整归宿主）。
 */
public final class MemoryPromotionAudit {

    private MemoryPromotionAudit() {
    }

    /**
     * 单压缩周期事实契约：四计数非负且后三者之和 = microCompacted（每条微压缩
     * 产物去向唯一：晋升摘要 / 直接归档 / 原地保留）。
     */
    public record CycleFacts(int microCompacted, int promotedToSummary,
                             int archivedDirect, int retainedInPlace) {

        public CycleFacts {
            boolean nonNegative = microCompacted >= 0 && promotedToSummary >= 0
                    && archivedDirect >= 0 && retainedInPlace >= 0;
            if (!nonNegative || promotedToSummary + archivedDirect + retainedInPlace
                    != microCompacted) {
                throw new IllegalArgumentException(
                        "非法周期事实：micro=" + microCompacted + ", promoted=" + promotedToSummary
                                + ", archived=" + archivedDirect + ", retained=" + retainedInPlace
                                + "（要求四者非负且去向之和=micro）");
            }
        }

        /** 当轮是否过早晋升：有产出且零原地保留（年轻代缓冲失效）。 */
        boolean prematurePromotion() {
            return microCompacted > 0 && retainedInPlace == 0;
        }
    }

    /** @param prematurePromotionCycles 过早晋升周期数（全晋升零保留的轮数） */
    public record PromotionReport(int cycles, long totalMicroCompacted, long totalPromoted,
                                  long totalArchivedDirect, long totalRetainedInPlace,
                                  long prematurePromotionCycles) {

        /** 晋升率 = promoted/totalMicro（无产出 -1 哨兵）。 */
        public double promotionRate() {
            return totalMicroCompacted == 0 ? -1d
                    : (double) totalPromoted / totalMicroCompacted;
        }

        /** 越级归档率 = archivedDirect/totalMicro（无产出 -1 哨兵）。 */
        public double directArchiveRate() {
            return totalMicroCompacted == 0 ? -1d
                    : (double) totalArchivedDirect / totalMicroCompacted;
        }
    }

    /**
     * 晋升账目入口。null 按空表（哨兵 -1）；周期事实逐条核契约，畸形即
     * fail-fast（读面不吞脏事实）。
     */
    public static PromotionReport analyze(List<CycleFacts> cycles) {
        List<CycleFacts> window = cycles == null ? List.of() : cycles;
        long micro = 0;
        long promoted = 0;
        long archived = 0;
        long retained = 0;
        long premature = 0;
        for (CycleFacts c : window) {
            micro += c.microCompacted();
            promoted += c.promotedToSummary();
            archived += c.archivedDirect();
            retained += c.retainedInPlace();
            if (c.prematurePromotion()) {
                premature++;
            }
        }
        return new PromotionReport(window.size(), micro, promoted, archived, retained, premature);
    }
}
