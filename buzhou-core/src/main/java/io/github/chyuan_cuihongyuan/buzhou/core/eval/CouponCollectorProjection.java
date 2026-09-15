package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * 收藏家覆盖期望（spec 1855 / T2911 / impl 1456）——概率论 coupon
 * collector（收藏家问题）：随机抽样中**见全 k 类**期望需 k·H(k) 轮
 *（H 为调和级数）——均匀抽样下收齐 10 类期望约 29 轮而非 10 轮（长尾
 * 在最后一类）；已见 s 类后期望余 k·(H(k) − H(s)) 轮。映射到评测/技能
 * 目录覆盖：「跑多少轮才能全类见过」由期望说话——覆盖测试的轮数预算
 * 不拍脑袋，「还差几轮收齐」随进度可读。
 *
 * <p>纯函数零状态（double 全程防溢出）；只投影不采样。
 */
public final class CouponCollectorProjection {

    private CouponCollectorProjection() {
    }

    /**
     * 见全 totalKinds 类的期望轮数 = k × H(k)。契约：totalKinds ≥ 0
     *（0 类无抽样语义，返回 0）。
     */
    public static double expectedDraws(int totalKinds) {
        if (totalKinds < 0) {
            throw new IllegalArgumentException("totalKinds 不能为负：" + totalKinds);
        }
        return totalKinds * harmonic(totalKinds);
    }

    /**
     * 已见 distinctSeen 类后期望余轮 = k × (H(k) − H(s))。契约：
     * 0 ≤ distinctSeen ≤ totalKinds（fail-fast）。
     */
    public static double expectedRemaining(int distinctSeen, int totalKinds) {
        if (totalKinds < 0 || distinctSeen < 0 || distinctSeen > totalKinds) {
            throw new IllegalArgumentException(String.format(
                    "非法覆盖进度：seen=%d, total=%d（要求 0 ≤ seen ≤ total）",
                    distinctSeen, totalKinds));
        }
        return totalKinds * (harmonic(totalKinds) - harmonic(distinctSeen));
    }

    /** 调和级数 H(n) = 1 + 1/2 + … + 1/n（H(0)=0）。 */
    private static double harmonic(int n) {
        double sum = 0;
        for (int i = 1; i <= n; i++) {
            sum += 1.0 / i;
        }
        return sum;
    }
}
