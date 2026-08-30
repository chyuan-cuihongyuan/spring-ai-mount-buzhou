# Wayfinder Map — Buzhou 文档治理收口（effort #35，本会话第 20 轮）

> effort #35，延续 #5–#34（累计 181 轮 / T1–T300 / impl 1–220）。
> 主线：**README 能力面同步**——README「生产级纵深」止于 effort #6（spec 32）；spec
> 33–74 的能力增量（共享原子性/缓存排序/评估闭环/OLAP 导出/崩溃自愈/边界压缩）未
> 进 README。文档滞后 = 用户不可见。

## Destination

README 增「生产级纵深（effort #7–#35 增量）」分组表（共享与原子性 / 缓存与前缀 /
评估闭环 / 观测与分析 / 恢复与压缩——精选主线 + spec 链接）；文档节补 spec 16–74
范围说明；全仓 verify 绿收口；fog 台账入 MAP（下一会话种子）。

## Notes

- 治理轮无代码变更（文档 + 收口）；精选不穷举（详单在 docs/spec/ 与 ops-runbook）。

## Decisions so far

- 分组叙事按「用户可得的能力」而非时间序。

## Not yet specified（会话 fog 台账——下会话种子）

- 事务性并行批（LangGraph superstep）；会话归档冷层；语义漂移触发压缩（embedding 真检测）；
  outbox due-time 键序结构；Redis 向量语义缓存（RediSearch）；优先级调度（SpawnGate
  重构）；A/B run 完成事件面；run 注册表 gauge；PairwiseEvalRunner 明细 API。

## Out of scope

- 沿用 #7–#34。

## Tickets

- [x] [T301 README 增量纵深表 + 文档节补范围](tickets/T301-readme.md)（impl-221）
- [x] [T302 里程碑 verify + 会话收口](tickets/T302-close.md)
