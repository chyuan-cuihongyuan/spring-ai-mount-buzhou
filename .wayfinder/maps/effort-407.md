# Wayfinder Map — Buzhou 在线采样入评测集（effort #407，D 会话第 8 轮）

> D 会话第 8 轮。勘察（2026-09-08）：eval 族回流全是**拉式**——
> SessionTrajectoryImporter（宿主点名会话批量回流）/FeedbackImporter
> （负反馈回流）。生产流量**自动采样**入集缺失：评测集靠人工挑会话，
> 覆盖面受宿主直觉限制；长尾真实问法进不了回归集。
> 勘察换题记录（连续两次）：原拟「依赖图并行提示」——Harness-
> ToolCallingManager 已有并行 executor+maxConcurrencyPerTurn+serialGroups，
> 弃；「会话快照 DR」——SessionArchiver（同店冷存）+ spec 28（单会话
> JSON 导出/导入/续聊）已覆盖，弃。

## Destination

`core.eval.TurnSamplerHook`（Honeycomb head sampling 借鉴——按率采流量
进数据集）：afterTurn 观察位（order 900 尾观察）——确定性采样
`hash(sessionId:turn) % 100 < rate-percent`（同轮同判可复现）；input/
response 任一空白跳过；min-input-chars 过滤短问；采样即 addItem
（provenance=sessionId+turn——与拉式回流同溯源域，跨路去重天然成立）；
数据集未建等异常 fail-soft（采样是旁路——计数 buzhou.eval.sampling-failed
不炸轮）。装配 yml `buzhou.eval.sampling.{enabled,dataset,rate-percent,
min-input-chars}` 声明即挂 hook + 暴露 EvalDatasetStore bean（宿主建集用）。

## Notes

- 号段：spec 407 / T705–T706 / impl-380。
- 借鉴源：Honeycomb（28k★ 生态标准）head-based deterministic sampling
> ——同键同判、率可调；LangSmith production→dataset 自动回流同思想。
- 纪律：采样语义是「进候选池」——golden 与否仍是人工判断（机制不预设，
> 与 TrajectoryImporter 同口径）；错误偏向采样（tail-based）为扩散候选。

## Out of scope

- 错误偏向/尾采样（onTurnError 语义另议）；跨实例采样率共享；自动评分
> 入集（eval 族已有 gate）；数据集容量上界治理。

## Tickets

- [x] [T705 TurnSamplerHook](../tickets/T705-turn-sampler-hook.md)
- [x] [T706 yml 装配 + store bean](../tickets/T706-sampling-assembly.md)
