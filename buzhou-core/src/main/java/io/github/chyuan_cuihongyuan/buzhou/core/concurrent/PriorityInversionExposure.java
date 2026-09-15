package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优先级反转暴露读面（spec 1821 / T2843 / impl 1422）——OS 优先级反转
 * （Mars Pathfinder 教训）：低优先级持有者挡住高优先级等待者，中优先级
 * 任务插队运行——关键路径被无关负载拖死而系统看似无故障。rank 口径同
 * Unix nice：**值越小越关键**；holderRank > waiterRank 即反转（持有者不如
 * 等待者关键），gap = 差值（拖死强度）。暴露读数让「关键轮次卡在批量
 * 杂活持有的资源上」显形为可计数的风险面。
 *
 * <p>纯函数零状态、只读不裁决（优先级继承/天花板协议归宿主）。
 */
public final class PriorityInversionExposure {

    private PriorityInversionExposure() {
    }

    /** 在持资源契约：resourceId 非空白。 */
    public record HeldResource(String resourceId, int holderRank) {

        public HeldResource {
            if (resourceId == null || resourceId.isBlank()) {
                throw new IllegalArgumentException("resourceId 不能为空");
            }
        }
    }

    /** 等待者契约：resourceId 非空白。 */
    public record Waiter(String resourceId, int waiterRank) {

        public Waiter {
            if (resourceId == null || resourceId.isBlank()) {
                throw new IllegalArgumentException("resourceId 不能为空");
            }
        }
    }

    /**
     * @param resources            在持资源数（去重后）
     * @param waiters              等待者总数
     * @param inversions           反转等待数（持有者不如等待者关键）
     * @param worstRankGap         最坏反转差（holder−waiter；无反转 0）
     * @param unknownResourceWaiters 引用未持有资源的等待者数（未被挡，诚实入账）
     */
    public record Exposure(int resources, int waiters, long inversions, long worstRankGap,
                           long unknownResourceWaiters) {

        /** 反转率 = inversions/waiters（无等待者 -1 哨兵）。 */
        public double inversionRatio() {
            return waiters == 0 ? -1d : (double) inversions / waiters;
        }
    }

    /**
     * 暴露账目入口。契约：null 任一按空表；同资源多次在持以首见为准
     *（互斥语义下不应发生，首见即审计基线）。反转判定：holderRank >
     * waiterRank（值小=关键）。
     */
    public static Exposure analyze(List<HeldResource> held, List<Waiter> waiters) {
        List<HeldResource> heldWindow = held == null ? List.of() : held;
        List<Waiter> waitWindow = waiters == null ? List.of() : waiters;
        Map<String, Integer> holderByResource = new HashMap<>();
        for (HeldResource r : heldWindow) {
            holderByResource.putIfAbsent(r.resourceId(), r.holderRank());
        }
        long inversions = 0;
        long worstGap = 0;
        long unknown = 0;
        for (Waiter w : waitWindow) {
            Integer holderRank = holderByResource.get(w.resourceId());
            if (holderRank == null) {
                unknown++;
                continue;
            }
            if (holderRank > w.waiterRank()) {
                inversions++;
                worstGap = Math.max(worstGap, holderRank - w.waiterRank());
            }
        }
        return new Exposure(holderByResource.size(), waitWindow.size(),
                inversions, worstGap, unknown);
    }
}
