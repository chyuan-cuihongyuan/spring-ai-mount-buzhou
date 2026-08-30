# Wayfinder Map — Buzhou 工具泳道并发闸（effort #205，B 会话第 28 轮）

> B 会话第 28 轮。主题池「慢工具隔离」：Turn 并发许可按数量发，不按资源类型
> ——3 个慢查询能吃光全部许可把快工具挤死。借鉴线程池隔离舱
> （bulkhead per lane）思想：慢工具走独立泳道闸。

## Destination

ToolLaneRegistry（core/exec：命名泳道 → 共享 Semaphore）+
LaneLimitingToolCallback（装饰器：执行前取泳道许可、finally 归还、超时
抛 IllegalStateException→harness 错误反馈词汇）。慢工具标 slow 泳道小许可，
快工具不受挤。

## Notes

- 号段：B=奇数 spec（本轮 173）；轮次 .wayfinder200+。
- 与 agent 舱（84：Turn 级）/工具熔断（131：失败率）正交：这是工具执行资源分道。

## Decisions so far

- 泳道许可阻塞等待（带超时）——排队优先于拒绝（慢查询本就要等）。

## Not yet specified

- 泳道排队长度观测；yml 泳道配置面。

## Out of scope

- 沿用各轮；动态泳道调参；优先级泳道内抢占。

## Tickets

- [x] [T541 ToolLaneRegistry + 泳道装饰器](tickets/T541-tool-lanes.md)（impl-300）
- [x] [T542 泳道回归（并发封顶/异常归还/独立泳道/超时）](tickets/T542-tool-lanes-tests.md)（impl-300）
