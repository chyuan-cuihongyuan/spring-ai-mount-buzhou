package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

/**
 * 滑动窗口计数器（spec 1884 / T2969 / impl 1485）——Cloudflare
 * sliding window counter：滑动速率 ≈ 上窗计数 ×(1−流逝比) + 本窗
 * 计数。固定窗的边界突发（两窗相接可 2×）被上窗惯性项修正，且
 * 无须滑窗日志的逐请求记账——零状态估计修正。
 *
 * <p>纯函数零状态、确定性；不记账不扣减（归 RateLimitBackend）。
 */
public final class SlidingWindowCounter {

    private SlidingWindowCounter() {
    }

    /**
     * 滑动速率估计：prev×(1−ratio) + curr。ratio=0（窗初）→≈prev
     * （上窗速率惯性延续）；ratio=1（窗末）→ curr。契约：两计数 ≥ 0、
     * ratio ∈ [0,1]（fail-fast）。
     */
    public static double estimate(long prevWindow, long currWindow,
                                  double elapsedRatio) {
        validate(prevWindow, currWindow, elapsedRatio);
        return prevWindow * (1.0 - elapsedRatio) + currWindow;
    }

    /**
     * 准入判定：估计值 ≥ limit 即超（= 限值按满额拒绝语义）。
     * 契约：limit ≥ 0（fail-fast）。
     */
    public static boolean wouldExceed(long prevWindow, long currWindow,
                                      double elapsedRatio, long limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit 不能为负：" + limit);
        }
        return estimate(prevWindow, currWindow, elapsedRatio) >= limit;
    }

    private static void validate(long prevWindow, long currWindow,
                                 double elapsedRatio) {
        if (prevWindow < 0 || currWindow < 0) {
            throw new IllegalArgumentException(String.format(
                    "窗口计数不能为负：prev=%d, curr=%d", prevWindow, currWindow));
        }
        if (elapsedRatio < 0.0 || elapsedRatio > 1.0) {
            throw new IllegalArgumentException(
                    "elapsedRatio 须在 [0,1]：" + elapsedRatio);
        }
    }
}
