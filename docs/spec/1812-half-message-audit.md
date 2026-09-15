# Spec 1812 — 事务半消息审计（effort #1812，R13）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2825–T2826，impl 1413）。借鉴：
> RocketMQ 事务消息——半消息两阶段（本地事务成功才 commit 放行）+ broker
> 周期回查滞留半消息兜底，「发消息」与「做事务」的原子性不靠分布式事务。

## Problem Statement

「做事」与「发事件」跨两个系统（如本地落盘 + webhook 通知）：事成了消息
没发、或消息发了事没成——现有 CompensatingBatch 是事后补偿，缺**事前意图
留痕 + 回查兜底**的裁决面：半消息滞留多少（未裁决面）、多少超回查阈
（该回查）、裁决率多高（本地事务健康度），没有统一读数。

## Solution

`HalfMessageAudit`（core/transaction，静态纯函数）：

- `Intent(key, state, ageMillis)` 单意图事实（三态 HALF/COMMITTED/
  ROLLED_BACK；构造器核契约：key 非空白、state 非 null、age ≥ 0）；
- `audit(staleThresholdMillis, intents)` → `Census(total, halves, committed,
  rolledBack, staleHalves)`：超阈半消息（HALF 且 age ≥ 阈，含边界）即回查
  候选；
- `resolutionRatio()`/`pendingRatio()`（无意图 -1 哨兵）。

## User Stories

1. 作为事务编排者，staleHalves=3 → 三个意图滞留超阈该回查补裁决——不靠
   人翻日志。
2. 作为运维者，pendingRatio 持续高 = 本地事务裁决慢（下游卡），resolutionRatio
   贴 1 = 两阶段闭合健康。
3. 作为框架宿主，键与年龄口径自声明，纯读面零侵入。

## Implementation Decisions

- 纯读面零状态，只审计不裁决（回查动作归宿主）；与 CompensatingBatch
  互补（事前意图 vs 事后补偿）。
- fail-fast：负阈/空 key/负年龄/null 状态；null 列表按空表。

## Testing Decisions

- 三态账目+超阈+两比率；阈含边界（≥）且已裁决不受阈影响；空表/null 哨兵；
  畸形四型 fail-fast。

## Out of Scope

- 不实现半消息存储/回查执行器（接线归后续轮）。

## Further Notes

- core/transaction 自 CompensatingBatch/InstrumentedUnitOfWork 后第三个
  事务语义件。
