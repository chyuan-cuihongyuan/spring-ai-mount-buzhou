# Wayfinder Map — Buzhou 摘要折入 trigger 溯源（effort #56，50 轮自迭代第 21 轮）

> effort #56，延续 #55（T355–T356 / impl-241）。主线：**spec 90 fog 项「漂移事件
  payload 字段」**——摘要折入（spec 70/90 双信号）在观测面与微压缩不可区分：
  memory.compacted 只有微压缩路径，摘要折入零事件，运维不知道折入因何触发。

## Destination

`CompactionListener` 新 default 方法 `onSummaryFolded(sessionId, summary, trigger)`
（trigger ∈ budget/backlog/drift；default 空——lambda 兼容）；IVP 在
summaryBridge.save 成功后回调（try-catch lenient 同款）；MemoryModule 装配匿名类
双写 `memory.summary.folded` 事件（payload：trigger/generation/coversUpToTurn）。

## Notes

- 借鉴：自身 spec 70/90 双信号的观测闭环收口（Spring Cloud Stream 事件溯源
  payload 惯例——判据来源进 payload）。

## Decisions so far

- trigger 判定顺序：budget 优先（预算压是硬性）→ drift → backlog（两者互斥到达——
  判据并联但一次折入标注主因）。

## Not yet specified

- 摘要折入速率指标（counter buzhou.memory.summary.folded tag trigger——观测面
  另议）； breaker 开路时的折入失败事件。

## Out of scope

- 沿用 #7–#55；压缩策略变更。

## Tickets

- [x] [T359 onSummaryFolded + IVP 回调 + MemoryModule 双写](tickets/T359-summary-folded-trigger.md)（impl-242）
- [x] [T360 红队（backlog/drift 标注 + lambda 兼容）+ memory 全量回归 + 收口](tickets/T360-trigger-close.md)
