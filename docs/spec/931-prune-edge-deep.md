# 931 — 剪枝边界深验

> 来源：I 会话第 32 轮 = effort #929 续（[T1303](../../.wayfinder/tickets/T1303-prune-edge-shape.md) / [T1304](../../.wayfinder/tickets/T1304-prune-edge-verify.md) / impl 684）。薄加固轮（G 会话深验模式）。

## 背景

spec 901 剪枝已测恰停/守恒/默认关/并行不生效——边界组合（minItems==total、阈值极小、与 memoization 共存）未覆盖。

## 目标

`PruneEdgeDeepTest` 四场景：
1. `minItems == total`：跑满观察窗恰触发但无剩余项——run 完整（total 项全真实执行）不残缺；
2. 阈值极小（0.01）：首个非 pass 项即触发剪枝（首 fail 后 failRate=1/1 ≥ 0.01）；
3. memoization 共存：memo 命中项照常产出结果参与裁决；pruned 项不写入 memo（止损语义不破坏记忆化——二次 run 重新完整评估）；
4. 组合序：Expectations 门（run 前）与剪枝（run 中）独立——门先拦，剪枝不越门。

## 兼容性

纯测试轮；零生产代码变更（边界实证缺陷按先例修复）。
