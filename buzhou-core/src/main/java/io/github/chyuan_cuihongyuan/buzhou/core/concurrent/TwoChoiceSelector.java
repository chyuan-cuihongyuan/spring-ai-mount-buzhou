package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.random.RandomGenerator;

/**
 * 二择选择器（spec 3022 / T5045 / impl 2023）——Power of Two
 * Choices 思想（Azar 1994 / Mitzenmacher：随机两问取轻者）：随机
 * 抽两候选、选负载小者（并列取先抽）——**O(1) 决策换最大负载从
 * Θ(ln n / ln ln n) 降到 Θ(ln ln n / ln ln ln n)**（朴素随机单抽
 * 最大链长 Θ(ln n/ln ln n) 的根治）。「工具端点/模型上游/工作者」
 * 多目标派单的经典轻武器：不维护全局队列、不扫描全表，只比两个。
 *
 * <p>纯函数：loads 快照由调用方维护（派单方自增回写）；负载
 * 非负、数组非空校验；RandomGenerator 注入确定性回放。
 */
public final class TwoChoiceSelector {

    private TwoChoiceSelector() {
    }

    /**
     * 二择：独立抽两下标（重抽保证相异），返回负载小者（并列取
     * 先抽）——负载全相等时退化为均匀随机。
     */
    public static int pick(int[] loads, RandomGenerator rng) {
        if (loads == null || loads.length == 0) {
            throw new IllegalArgumentException("loads 非空");
        }
        for (int load : loads) {
            if (load < 0) {
                throw new IllegalArgumentException("负载非负：" + load);
            }
        }
        if (loads.length == 1) {
            return 0;
        }
        int first = rng.nextInt(loads.length);
        int second = rng.nextInt(loads.length);
        while (second == first) {
            second = rng.nextInt(loads.length);
        }
        return loads[second] < loads[first] ? second : first;
    }
}
