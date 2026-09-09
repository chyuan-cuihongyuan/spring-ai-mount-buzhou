# Wayfinder Map — Buzhou A/B run 完成事件面（effort #36，50 轮自迭代会话第 1 轮）

> effort #36，延续 #5–#35（累计 181 轮 / T1–T302 / impl 1–221）。
> 本会话目标：**≥50 个完整 wayfinder→spec→tickets→implement 自迭代循环**，
> 每轮借鉴一个高价值 GitHub 项目的思想做小步纵深化（默认零行为变化或 opt-in）。
> 主线：**#35 fog 毕业生**——EvalRunner 有 `eval.run.completed` 事件，PairwiseEvalRunner
> 无对应事件面：A/B 结论完成后订阅方（webhook/看板）不可感知。

## Destination

`compare()` 完成即发 `ab.run.completed`（独立收尾会话 `ab-<runId>-done`，A 路
runtime 持有）；payload 含 runId/datasetName/total/winsA/winsB/ties/errors/
winRateA/winRateB/durationMs；total>0 才发（空集无对比发生）；与落盘正交
（2 参构造同样发）；零新键零新类型。

## Notes

- 借鉴：LangSmith run 生命周期事件面（run created/completed 家族）。
- 与 spec 52 §F（eval.run.completed）同语义同口径——家族扩展而非新协议。

## Decisions so far

- 收尾会话挂 A 路 runtime（基准面持有者；B 路是被比较方，事件归属不对称无争议）。
- 事件与落盘正交：订阅方对不落盘 run 同样可感知（事件面只依赖执行本身）。

## Not yet specified（会话 fog 台账——后续轮种子）

- PairwiseEvalRunner 明细查询 API（abItems/run detail——#37 候选）。
- run 注册表 gauge（活跃 run 观测——#38 候选）。
- 事务性并行批（LangGraph superstep）；会话归档冷层；语义漂移触发压缩；
  outbox due-time 键序；RediSearch 向量语义缓存；优先级调度（SpawnGate）。

## Out of scope

- 沿用 #7–#35；事件重放/事件持久化（outbox 已覆盖 at-least-once 语义）。

## Tickets

- [x] [T303 ab.run.completed 事件发射](../tickets/T303-ab-run-event.md)（impl-222）
- [x] [T304 红队（payload 等值/空集不发/不落盘同样发）+ 文档 + 收口](../tickets/T304-ab-run-close.md)
