---
id: T1253
title: 评估失败率中途剪枝的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-13
---

## Question

I 会话第 2 轮：EvalRunner 当前一次性全量跑完（串行 for / 并行 invokeAll）——评估中途剪枝（Optuna pruner 提前停止思想）是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 2 轮 = effort #901 / spec 901 / impl 654）：缺口成立——整跑无法在已注定失败（如数据集全错/环境故障）时止损算力。落点 `EvalRunner`：opt-in `setPrunePolicy(EvalPrunePolicy)`（默认 null=关闭，零行为变化）。策略 record 两参数：`minItems`（前 N 项观察窗不放行）+ `failRateThreshold`（fail+error 占比阈值）。**仅串行路径生效**（并行 invokeAll 无法低成本中途取消，诚实入档不做伪实现）；触发后剩余项生成 status=`pruned` 结果（与 pass/fail/error 同 record，不扩 EvalRunResult 字段——读取者按 status 区分），run 照常落盘/发事件/计指标 `buzhou.eval.run.pruned` + WARN。Optuna 的相对中位数基线（历史 run 对比）留位后续，第一版做绝对失败率（无历史依赖、语义可独立验证）。
