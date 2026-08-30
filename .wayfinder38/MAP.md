# Wayfinder Map — Buzhou 活跃 run 注册表 gauge（effort #38，50 轮自迭代第 3 轮）

> effort #38，延续 #37（T305–T306 / impl-223）。主线：**#37 fog 毕业生**——
> eval/ab run 无在飞观测面：评估进行中，运维只能靠日志猜「现在有几个 run 在跑」。

## Destination

`EvalRunRegistry`（eval/ab 两 kind 在飞计数，runId 幂等 + Registration close 幂等）
+ gauge `buzhou.eval.runs.active`（tag kind，每 kind 首次 begin 注册）；两 runner
生命周期内自动登记；零新键零构造面变化（BuzhouMetricsHolder 同款全局旋钮）。

## Notes

- 借鉴：LangSmith active-runs 面Prometheus 惯例 gauge 语义。
- 不装 micrometer 时 no-op 零开销（库不强制可观测依赖——项目既定纪律）。

## Decisions so far

- 全局旋钮而非构造注入（构造面已 3-4 参，再侵入装配链；metrics 先例一致）。
- gauge 注册惰性（每 kind 首次 begin）——空库零注册噪音。

## Not yet specified

- 事务性并行批（LangGraph superstep）；会话归档冷层；语义漂移触发压缩；
  outbox due-time 键序；RediSearch 向量缓存；优先级调度（SpawnGate）。

## Out of scope

- 沿用 #7–#37；会话级（非 eval run）在飞 gauge（session index 已有面）。

## Tickets

- [x] [T307 EvalRunRegistry + 两 runner 接线 + gauge](tickets/T307-run-registry.md)（impl-224）
- [x] [T308 4 例红队（生命周期/ab kind/幂等/gauge 值跟踪）+ 文档 + 收口](tickets/T308-run-registry-close.md)
