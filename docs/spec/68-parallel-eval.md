# Spec 68 — 评估并行执行（effort #28）

> wayfinder map：`.wayfinder28/MAP.md`（T287–T288）。OSS 借鉴：LangSmith / DeepEval
> 并行评估执行。

## Problem Statement

EvalRunner 逐项串行执行：每项独立会话（天然隔离），大数据集评估 wall-clock 随项数
线性增长——CI 窗口与迭代节奏被拖慢。

## Solution

`run(dataset, evaluator, parallelism)` 重载：虚拟线程池并行执行项（每项仍独占隔离
会话）；结果按数据集项序聚合（与串行输出同序——确定性不因并行漂移）；并行度
clamp 1..32；旧签名 = 1 零行为变化。汇总统计、run 记录落盘、完成事件与串行同口径
（全部项完成后一次进行）。

## User Stories

1. 作为评估作者，我要可配并行度，所以大数据集评估 wall-clock 缩短。
2. 作为 CI 用户，我要结果序与串行一致，所以断言/diff 不因并行漂移。
3. 作为红队，我要默认路径字节级零变化，所以升级零风险。
4. 作为红队，我要项失败不炸整跑（既有三态），所以并行下错误语义保持。

## Implementation Decisions

- Executors.newVirtualThreadPerTaskExecutor per-run + invokeAll（项内异常已收敛三态）。
- 结果按 index 收集进定长数组再转 List（序确定）。

## Testing Decisions

- 并行红队：多项数据集 parallelism=4 结果与串行逐项等值同序；默认路径与既有输出一致。

## Out of Scope

- 分布式评估；失败重试/断点续跑；新配置键。

## Further Notes

- 高并行度可能触发宿主模型端限流——错误三态如实入账（诚实边界）。
