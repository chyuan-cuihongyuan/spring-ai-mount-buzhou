# Spec 2033 — 维护触发器（effort #2033，R34）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3167–T3168，impl 1584）。
> 借鉴：Postgres autovacuum——死元组比例阈值触发 + 间隔兜底。

## Problem Statement

死数据清理（已消费事件 / 已投递 outbox / 过期索引项）：每次操作后即
时清理是写放大；永不清理则死数据无限陈化（低流量场景比例永不达标）。
触发时机需要「比例 + 间隔」双口径。

## Solution

`MaintenanceTrigger`（core/recovery，纯判定无副作用——动作归调用方）：

- `shouldTrigger(dead, live, now)`：**死/活 ≥ 阈值**（默认 0.2，
  autovacuum 惯例；live=0 且 dead>0 全死必清；双零不触——无事可做）
  **或距上次触发 ≥ 最大间隔**（默认 24h；从未触发自 0 起算——低流量
  兜底）；
- `noteTriggered(now)` 触发记账（次数 + 时刻——触发频率即维护健康度，
  间隔重新起算）；
- 读数：triggerCount / lastTriggeredAt；契约：threshold ∈ (0,1]、
  maxInterval > 0、计数/时刻非负 fail-fast。

## User Stories

1. 作为清理作者，比例触发省写放大、间隔兜底防陈化——双口径一处
   定义。
2. 作为 SRE，triggerCount 频率读数——过频（阈值过敏感）或零（从未
   触发=兜底失效）都可诊断。

## Testing Decisions

- 10%/19% 不触、恰 20% 触（>= 语义）、50% 触；live=0 全死触、双零不
  触；间隔 500 不触 1000 触；从未触发自 0 起算；触发后间隔重算；畸形
  六型 fail-fast。

## Out of Scope

- 不做阈值自适应（autovacuum_vacuum_cost-based 缩放留白）；不接具体
  清理作业（接线归后续轮）。

## Further Notes

- 与归档 TTL 治理（#65）/保留策略（#3）互补：那些定「清什么」，本件
  定「何时清」。
