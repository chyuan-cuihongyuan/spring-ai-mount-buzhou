# Spec 77 — 活跃 run 注册表 gauge（effort #38）

> wayfinder map：`.wayfinder38/MAP.md`（T307–T308）。#37 fog 毕业生。
> 借鉴：LangSmith active-runs 观测面。

## Problem Statement

评估 run 进行中无在飞观测面：运维不知道「现在有几个 eval/ab run 在跑」，评估
风暴（并行 run 失控）只能事后从日志反推。

## Solution

`EvalRunRegistry`：eval/ab 两 kind 在飞 run 计数（runId 集合）；EvalRunner 与
PairwiseEvalRunner 在 run 生命周期内自动 begin/close；每 kind 首次 begin 经
BuzhouMetricsHolder 注册 gauge `buzhou.eval.runs.active`（tag kind）。全局旋钮
（`global()`/`install()`），构造面零变化。

## User Stories

1. 作为运维，我要 gauge 看在飞 run 数，所以评估风暴可告警。
2. 作为宿主，我要 runner 自动登记，所以零接线成本。
3. 作为测试作者，我要 runId/close 双幂等，所以并行测试不串计数。

## Implementation Decisions

- 全局旋钮模式（BuzhouMetricsHolder 先例——构造面不再侵入）。
- gauge 惰性注册（每 kind 首次 begin；空库零注册噪音）。
- tag 只放 kind（有界枚举；严禁 runId 进 tag——项目既定纪律）。

## Testing Decisions

- 模型调用钩子断言在飞窗口内计数=1、出界归零（eval 与 ab 各一例）；
- runId 重复 begin 只计一次、Registration 重复 close 不误删；
- 假 metrics 捕获 gauge 注册（name+tag）且 supplier 值随在飞数走。

## Out of Scope

- 会话级在飞 gauge；histogram（run 时长已有 timer 面）；跨实例聚合（单实例语义）。

## Further Notes

- 与 spec 68 并行 eval 正交：registry 计 run 数不计数项数。
