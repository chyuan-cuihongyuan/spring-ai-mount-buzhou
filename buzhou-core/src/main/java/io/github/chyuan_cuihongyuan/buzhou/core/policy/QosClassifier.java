package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.List;

/**
 * QoS 资源声明分级（spec 2019 / T3139 / impl 1570）——K8s QoS Classes
 * 思想：按资源声明（request 下限 / limit 上限）三级分类——GUARANTEED
 *（全维 request==limit：保额保量，背压/驱逐时受保护）、BURSTABLE
 *（0&lt;request&lt;limit：有保底可突发）、BEST_EFFORT（零声明：尽力而
 * 为，资源紧张先让位）——驱逐次序与保护策略由分级驱动，不再逐实例
 * 拍脑袋。
 *
 * <p>纯函数零状态、确定性；多维合成遵循 K8s 语义（全 GUARANTEED 才
 * GUARANTEED，任一零声明即 BEST_EFFORT 整体，否则 BURSTABLE）。
 */
public final class QosClassifier {

    /** QoS 三级（驱逐优先级从先让位到受保护）。 */
    public enum QosClass {
        BEST_EFFORT,   // 零声明——资源紧张先让位（evictionRank 最低）
        BURSTABLE,     // 有保底可突发
        GUARANTEED     // 全维保额保量——受保护
    }

    /** 单维资源声明：request 声明下限（0=未声明），limit 声明上限。 */
    public record ResourceRequest(long request, long limit) {
        public ResourceRequest {
            if (request < 0 || limit < 0) {
                throw new IllegalArgumentException("声明须非负：request=" + request + " limit=" + limit);
            }
            if (request > limit) {
                throw new IllegalArgumentException("声明矛盾（request > limit）：" + request + " > " + limit);
            }
        }
    }

    private QosClassifier() {
    }

    /**
     * 分级（K8s 合成语义）：空列表 = BEST_EFFORT（零声明）；全维
     * request==limit&gt;0 = GUARANTEED；全维零声明 = BEST_EFFORT；
     * 其余（含混维）= BURSTABLE。
     */
    public static QosClass classify(List<ResourceRequest> resources) {
        if (resources == null || resources.isEmpty()) {
            return QosClass.BEST_EFFORT;
        }
        boolean allGuaranteed = true;
        boolean allZero = true;
        for (ResourceRequest r : resources) {
            if (r.request() == 0 && r.limit() == 0) {
                allGuaranteed = false; // 零声明维度拉低整体
                continue;
            }
            allZero = false;
            if (r.request() != r.limit()) {
                allGuaranteed = false;
            }
        }
        if (allZero) {
            return QosClass.BEST_EFFORT;
        }
        return allGuaranteed ? QosClass.GUARANTEED : QosClass.BURSTABLE;
    }

    /** 驱逐次序值（越小越先让位）：BEST_EFFORT=0 &lt; BURSTABLE=1 &lt; GUARANTEED=2。 */
    public static int evictionRank(QosClass qosClass) {
        if (qosClass == null) {
            throw new IllegalArgumentException("qosClass 不能为 null");
        }
        return switch (qosClass) {
            case BEST_EFFORT -> 0;
            case BURSTABLE -> 1;
            case GUARANTEED -> 2;
        };
    }

    /** 紧张时该不该让位：rank 低于保护线（默认保 GUARANTEED）即让。 */
    public static boolean shouldYield(QosClass qosClass, QosClass protectedClass) {
        return evictionRank(qosClass) < evictionRank(protectedClass);
    }
}
