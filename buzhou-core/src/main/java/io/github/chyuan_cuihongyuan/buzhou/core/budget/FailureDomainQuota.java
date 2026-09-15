package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.util.List;

/**
 * 失败域配额（spec 1837 / T2875 / impl 1438）——k8s failure-domain /
 * 供应链「分域配额+中央备货」思想：预算按失败域（实例/机房/模型供应方）
 * 分桶——单域过载或故障只烧自己的桶，其余域零影响；桶满可向**全局保留**
 * 借用（备货共享但总量封顶），保留也尽才拒——「一域之失不殃及池鱼」
 * 与「备货兜底」同时成立。
 *
 * <p>纯函数零状态、只裁决不记账（用量归属归宿主）。
 */
public final class FailureDomainQuota {

    private FailureDomainQuota() {
    }

    /** 准入三态：FROM_BUCKET 域桶内 / BORROW_RESERVE 借全局保留 / DENY 拒。 */
    public enum Admission {

        /** 域桶有余——本域额度内放行。 */
        FROM_BUCKET,

        /** 域桶满但全局保留有余——借用放行（一域之失不殃及池鱼的兜底）。 */
        BORROW_RESERVE,

        /** 域桶与保留皆尽——拒。 */
        DENY
    }

    /**
     * 单位准入裁决。契约：四计数 ≥ 0（fail-fast）；语义：域桶先花
     *（domainUsed &lt; domainQuota），桶满借保留（reserveUsed &lt;
     * reserveQuota），皆尽拒。
     */
    public static Admission admit(long domainUsed, long domainQuota,
                                  long reserveUsed, long reserveQuota) {
        validateNonNegative("domainUsed", domainUsed);
        validateNonNegative("domainQuota", domainQuota);
        validateNonNegative("reserveUsed", reserveUsed);
        validateNonNegative("reserveQuota", reserveQuota);
        if (domainUsed < domainQuota) {
            return Admission.FROM_BUCKET;
        }
        if (reserveUsed < reserveQuota) {
            return Admission.BORROW_RESERVE;
        }
        return Admission.DENY;
    }

    /** 单域用量契约：domain 非空白、两计数 ≥ 0。 */
    public record DomainUsage(String domain, long used, long quota) {

        public DomainUsage {
            if (domain == null || domain.isBlank() || used < 0 || quota < 0) {
                throw new IllegalArgumentException(String.format(
                        "非法域用量：domain=%s, used=%d, quota=%d", domain, used, quota));
            }
        }
    }

    /**
     * @param domains 域数
     * @param atCap   桶满（used ≥ quota）域数
     * @param borrowing 借用中（used &gt; quota）域数
     * @param tightest 最紧域（used/quota 最高者 id；无域 null）
     * @param reserveUtilization 保留利用率（保留 0 时 -1 哨兵）
     */
    public record DomainCensus(int domains, long atCap, long borrowing,
                               String tightest, double reserveUtilization) {
    }

    /** 域普查。null 按空表；并列最紧取首（入参序）。 */
    public static DomainCensus census(long reserveUsed, long reserveQuota,
                                      List<DomainUsage> usages) {
        validateNonNegative("reserveUsed", reserveUsed);
        validateNonNegative("reserveQuota", reserveQuota);
        List<DomainUsage> window = usages == null ? List.of() : usages;
        long atCap = 0;
        long borrowing = 0;
        String tightest = null;
        double tightestRatio = -1;
        for (DomainUsage u : window) {
            double ratio = u.quota() == 0
                    ? (u.used() == 0 ? 0d : Double.POSITIVE_INFINITY)
                    : (double) u.used() / u.quota();
            if (u.used() >= u.quota()) {
                atCap++;
            }
            if (u.used() > u.quota()) {
                borrowing++;
            }
            if (ratio > tightestRatio) {
                tightestRatio = ratio;
                tightest = u.domain();
            }
        }
        double reserveUtil = reserveQuota == 0 ? -1d
                : (double) reserveUsed / reserveQuota;
        return new DomainCensus(window.size(), atCap, borrowing, tightest, reserveUtil);
    }

    private static void validateNonNegative(String name, long value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " 不能为负：" + value);
        }
    }
}
