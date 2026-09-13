# 901 — 评估失败率中途剪枝

> 来源：I 会话第 2 轮 = effort #901（[T1253](../../.wayfinder/tickets/T1253-eval-prune-shape.md) / [T1254](../../.wayfinder/tickets/T1254-eval-prune-verify.md) / impl 654）。借鉴：Optuna [pruner](https://optuna.readthedocs.io/en/stable/tutorial/20_recipes/007_optuna_search_space.html)——注定失败的 trial 提前停止，省下算力。

## Problem Statement

`EvalRunner.run` 一次性跑完全量数据集：数据集全错、被评估 agent 配置崩坏、评估器环境故障时，剩余 N−k 项照样逐项烧模型调用与 token 预算——结果注定失败，纯算力浪费。Optuna 对超参 trial 的 pruner 证明"跑一小段即可判定注定失败并提前停止"是正确止损粒度。

## 目标

- 新公共 record `EvalPrunePolicy`（core.eval，api 面）：`minItems`（观察窗，前 N 项不放行）+ `failRateThreshold`（fail+error 占比阈值，开区间 (0,1)）+ compact constructor 校验；
- `EvalRunner.setPrunePolicy(EvalPrunePolicy)`：opt-in，默认 null 关闭（零行为变化）；
- **仅串行路径生效**：每完成一项检查（已完成数 ≥ minItems 且 (fail+error)/已完成 ≥ 阈值）→ 中止循环；并行路径诚实不做（invokeAll 无低成本中途取消，入档边界）；
- 触发后剩余项生成 status=`pruned` 结果（detail 注明剪枝原因），`EvalRunResult` 不扩字段（读取者按 status 区分）；run 照常落盘 / `eval.run.completed` 事件 / 指标 `buzhou.eval.run.pruned` counter + WARN 单条。

## 兼容性

纯增量：新公共类型 + 新 setter（默认关闭）。默认路径与并行路径行为零变化；`EvalRunItemResult` 新增包级状态常量 `pruned`，既有 pass/fail/error 值不变。
