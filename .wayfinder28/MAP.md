# Wayfinder Map — Buzhou 评估并行执行（effort #28）

> effort #28，延续 #5–#27（累计 174 轮 / T1–T286 / impl 1–213）。
> 主线：**评估项并行执行**——EvalRunner 逐项串行（每项一个隔离 eval 会话，天然可并
> 行）；大数据集 wall-clock 线性。借鉴 LangSmith / DeepEval 的并行评估执行
> （parallelism 可配、结果按项序聚合确定性不变）。

## Destination

`run(dataset, evaluator, parallelism)` 重载（默认路径 parallelism=1 零变化）：虚拟线程
池并行执行项（每项仍独占隔离会话）；结果列表按数据集项序聚合（与串行字节级同序）；
run 记录/事件口径零变化；并行度上限 32 防失控；零新键（API 参数非配置）。

## Notes

- 外部事实源：LangSmith eval 并行执行；DeepEval n_jobs 语义。本地裁定：结果序确定
  （按项序 index 收集——并行只影响执行序不影响记录序）。
- 诚实边界：共享模型端点的吞吐/限流是外部约束——高并行度可能触发宿主限流（错误
  三态如实入账）。

## Decisions so far

- ExecutorService per-run（虚拟线程）+ invokeAll；项内异常已在 runItem 三态收敛。
- 汇总/落盘/事件在全部项完成后一次进行（与串行同口径）。

## Not yet specified

- 断点续跑/失败重试策略；并行度配置键化（API 面已够）。

## Out of scope

- 沿用 #7–#27；分布式评估；新配置键。

## Tickets

- [ ] [T287 run 三参重载（虚拟线程并行 + 项序聚合）](tickets/T287-parallel.md)
- [ ] [T288 红队（结果序确定性/并行正确/默认零变化）+ 文档 + verify + 收口](tickets/T288-parallel-close.md)
