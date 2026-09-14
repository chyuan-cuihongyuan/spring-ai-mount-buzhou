# 1443 — Saga 运行静态读数

> 来源：L 会话第 43 轮 = effort #1443（票 T2187 / T2188 / impl 1095）。借鉴：Seata 事务度量（全局事务/分支事务成功率的漏斗——补偿型事务的健康第一读数）。

## Problem Statement

`CompensatingBatch`（spec 623 补偿型批量事务）只有按步 metrics counter（compensated/compensation-failed 带 step tag）：**运行级漏斗**（多少次 run、多少全成功、多少触发补偿、补偿自身失败断点几次）无聚合面——「saga 补偿高发」的容量/设计问题静默。

## 目标

- `CompensatingBatch` 静态读数增量（CompensatingBatch.sagaStats()，ToolArgsValidator 先例）：
  - 漏斗：`runs` / `successes` / `compensationRuns` / `stepsExecuted` + `compensationFailures`（补偿自身失败=人工介入断点）；
  - 守恒式：`runs = successes + compensationRuns`（conserved() 派生）；
  - `lastFailedStep`：末次失败步名（currentStep 追踪——进步更新、失败定格、成功清理）；
  - `SagaStats` 嵌套 record + `resetSagaStatsForTest()`。
- 埋点：run 主路径四点 + unwind 补偿失败分支一点；补偿/断点/上抛语义逐位不变。

## 兼容性

纯增量读面：倒序补偿/停止回退/异常透传语义逐位不变。

## Out of Scope

- per-step 深度分布（已有 step tag counter——避免双轨分裂）。
- 补偿耗时（时延归 store 延迟环族）。
- UnitOfWork 维度区分（InstrumentedUnitOfWork 1436 已计量事务层）。
