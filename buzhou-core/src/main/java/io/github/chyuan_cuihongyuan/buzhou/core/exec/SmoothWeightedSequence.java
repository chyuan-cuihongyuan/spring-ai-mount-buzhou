package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayList;
import java.util.List;

/**
 * 平滑加权轮询序列（spec 1828 / T2857 / impl 1429）——NGINX smooth
 * weighted round-robin 思想：按权重比例派发但**交错平滑**——5:1:2 不产生
 * aaaaabc 式扎堆，而是 aabacaad 式均匀穿插（下游负载曲线平、连接复用
 * 高）。算法：每轮全员 current += weight，取最大者派出并 current −=
 * 总权重。零权重不参与（永不派发）；确定性可回放。
 *
 * <p>纯函数零状态、只生成序列不派发（执行归宿主）。
 */
public final class SmoothWeightedSequence {

    private SmoothWeightedSequence() {
    }

    /**
     * 序列生成入口。契约：picks ≥ 0；weights 非空、元素 ≥ 0、总和 > 0
     *（fail-fast）；返回 picks 个下标（按平滑加权序）。
     */
    public static List<Integer> sequence(List<Integer> weights, int picks) {
        if (picks < 0) {
            throw new IllegalArgumentException("picks 不能为负：" + picks);
        }
        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("weights 不能为空");
        }
        long total = 0;
        for (Integer w : weights) {
            if (w == null || w < 0) {
                throw new IllegalArgumentException("权重不能为 null 或负：" + w);
            }
            total += w;
        }
        if (total == 0) {
            throw new IllegalArgumentException("权重总和须大于 0");
        }
        long[] current = new long[weights.size()];
        List<Integer> result = new ArrayList<>(picks);
        for (int i = 0; i < picks; i++) {
            int best = -1;
            for (int j = 0; j < weights.size(); j++) {
                current[j] += weights.get(j);
                if (best < 0 || current[j] > current[best]) {
                    best = j;
                }
            }
            current[best] -= total;
            result.add(best);
        }
        return result;
    }

    /**
     * 派发计数（序列的直方）：counts[i] = i 被派发次数。契约同
     * {@link #sequence}。
     */
    public static long[] counts(List<Integer> weights, int picks) {
        long[] counts = new long[weights == null ? 0 : weights.size()];
        for (int index : sequence(weights, picks)) {
            counts[index]++;
        }
        return counts;
    }
}
