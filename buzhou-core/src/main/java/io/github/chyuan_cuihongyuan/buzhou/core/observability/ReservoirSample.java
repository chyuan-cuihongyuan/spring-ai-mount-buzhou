package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 水库采样（spec 1849 / T2899 / impl 1450）——Knuth 水库算法（R）：
 * 流长未知下均匀无放回采 k 个——前 k 直接入池，第 i（1-based）个元素以
 * k/i 概率替换池内随机一员，**每个元素终选概率恰 k/n**（数学均匀，与
 * 到达序无关）。种子化 java.util.Random（LCG——跨 JVM 重现，EvalOrderRotator
 * 同口径先例）：同种子同样本，可回放可审计。
 *
 * <p>纯函数零状态（Random 局部于调用）；只采样不裁决。
 */
public final class ReservoirSample {

    private ReservoirSample() {
    }

    /**
     * 采样入口。契约：k ≥ 0（fail-fast）、stream 元素非 null；null 流按
     * 空表；语义：n ≤ k 全量保序；n &gt; k 每元素终选概率 k/n（均匀）。
     */
    public static List<String> sample(int k, long seed, List<String> stream) {
        if (k < 0) {
            throw new IllegalArgumentException("k 不能为负：" + k);
        }
        List<String> window = stream == null ? List.of() : stream;
        List<String> reservoir = new ArrayList<>(Math.min(k, window.size()));
        Random random = new Random(seed);
        for (int i = 0; i < window.size(); i++) {
            String item = window.get(i);
            if (item == null) {
                throw new IllegalArgumentException("流元素不能为 null（位置 " + i + "）");
            }
            if (reservoir.size() < k) {
                reservoir.add(item);
            } else {
                // 第 i+1 个元素以 k/(i+1) 概率替换池内随机一员
                int slot = random.nextInt(i + 1);
                if (slot < k) {
                    reservoir.set(slot, item);
                }
            }
        }
        return List.copyOf(reservoir);
    }
}
