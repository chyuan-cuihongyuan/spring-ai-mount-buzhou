# Wayfinder Map — Buzhou tag 基数守卫（effort #111，A 会话第 6 轮）

> A 侧编号策略：轮次 #111+ 远端号段 / spec 偶数段 132+ / 票号 T451+ 顺延
> （B 侧 T471+ 偏移已声明——互不重叠）。B 侧预告主题（PII yml、舱聚合、
> 工具熔断、请求合并）避让。

## Destination

「tag 值有界」从纪律变机制：装饰器式守卫把 per-(指标名, tag 键) 去重值集
封顶，越限折 __overflow__（样本不丢、维度细分丢）——时序库 cardinality
不被一个越界值打爆。

## Notes

- 借鉴 Grafana Loki label cardinality limit；热路径不抛不格式化（守卫失守
  不放大为指标路径故障）；并发双插最坏超限 1-2 值（ErrorSignatures 同先例）。

## Decisions so far

- [TagCardinalityGuard](tickets/T457-cardinality-guard.md) — 装饰任意
  BuzhouMetrics（counter/timer/gauge 三面）+ folds() 守卫面 + 指标名空间
  512 满则新名全折。

## Not yet specified

- 守卫接入 BuzhouMetricsHolder 默认装配（opt-in 键）；fold 事件告警接线。

## Out of scope

- per-tag-key 差异化封顶配置；守卫自身进 micrometer registry。

## Tickets

- [x] [T457 tag 基数守卫](tickets/T457-cardinality-guard.md)（impl-278）
- [x] [T458 收口提交](tickets/T458-cardinality-close.md)（impl-278）
