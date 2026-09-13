package io.github.chyuan_cuihongyuan.buzhou.core.eval;

/**
 * impl-654 / spec 901：评估失败率中途剪枝策略（Optuna pruner 提前停止思想——
 * 注定失败的 run 提前止损算力）。
 *
 * <p>语义：串行跑完 {@code minItems} 项（观察窗）后，若已完成项中
 * {@code fail + error} 占比 ≥ {@code failRateThreshold}，则中止剩余项
 * （剩余项 status = {@code pruned}，不执行不烧预算）。仅串行路径生效
 * （{@code run(dataset, evaluator, 1)} 或单项数据集）——并行路径诚实不做
 * （invokeAll 无低成本中途取消）。
 *
 * @param minItems          观察窗：至少完成该数量的项才允许剪枝判定（≥ 1）
 * @param failRateThreshold 失败率阈值（fail+error / 已完成，开区间 (0,1)）
 */
public record EvalPrunePolicy(int minItems, double failRateThreshold) {

    public EvalPrunePolicy {
        if (minItems < 1) {
            throw new IllegalArgumentException("minItems 须 ≥ 1（观察窗语义），收到 " + minItems);
        }
        if (failRateThreshold <= 0 || failRateThreshold >= 1) {
            throw new IllegalArgumentException(
                    "failRateThreshold 须 ∈ 开区间 (0,1)，收到 " + failRateThreshold);
        }
    }
}
