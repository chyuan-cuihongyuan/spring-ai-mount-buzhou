# 1522 — 评估并行路径失败率剪枝（spec 901 边界收口）

> 来源：M 会话第 25 轮 = effort #1522（impl 1125）。分波提交（cooperative batching）思想。

## 背景

spec 901 失败率剪枝此前仅串行路径生效（并行 invokeAll 无低成本中途取消——「并行诚实不生效」入档边界）：配置剪枝的并行大 run 无法止损。

## 目标

- 并行路径分波执行：items 按 workers 分块，每波 invokeAll 完成后按串行同款观察窗语义（已完成 ≥ minItems 且 fail+error 占比 ≥ threshold）检查——达阈值剩余项标 pruned 不再起波；
- 未配 EvalPrunePolicy 零行为（单波全量等价）；取消（spec 1505）与剪枝正交（取消优先）。

## 兼容性

opt-in 增强：配剪枝的并行 run 从「不剪」变「波间剪」；未配置零变化。
