package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

/**
 * 客户端自适应节流（spec 1911 / T3023 / impl 1512）——Google SRE
 * 自适应节流公式：拒发概率 = max(0, (requests − K×accepts) /
 * (requests+1))。后端过载时（接受数跟不上请求数×K）请求在客户端
 * 就地概率性拒发——比到了服务端再拒绝省一个网络来回，重试风暴
 * 在源头衰减。健康期概率恒 0 零干扰。
 *
 * <p>纯函数零状态（历史计数归调用方）；dice 注入确定性可回放。
 */
public final class ClientThrottleProbability {

    private ClientThrottleProbability() {
    }

    /**
     * 拒发概率：max(0, (requests − K×accepts)/(requests+1))。
     * 契约：requests/accepts ≥ 0、ratioK ≥ 1（fail-fast）。
     */
    public static double rejectProbability(long requests, long accepts,
                                           double ratioK) {
        if (requests < 0) {
            throw new IllegalArgumentException("requests 不能为负：" + requests);
        }
        if (accepts < 0) {
            throw new IllegalArgumentException("accepts 不能为负：" + accepts);
        }
        if (ratioK < 1.0) {
            throw new IllegalArgumentException("ratioK 不能小于 1：" + ratioK);
        }
        double p = (requests - ratioK * accepts) / (requests + 1.0);
        return Math.max(0.0, p);
    }

    /**
     * 掷骰判定：dice ∈ [0,1) < 概率即拒发（确定性可回放）。
     * 契约：dice ∈ [0,1]（fail-fast）。
     */
    public static boolean shouldDrop(double probability, double dice) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException(
                    "probability 须在 [0,1]：" + probability);
        }
        if (dice < 0.0 || dice > 1.0) {
            throw new IllegalArgumentException("dice 须在 [0,1]：" + dice);
        }
        return dice < probability;
    }
}
