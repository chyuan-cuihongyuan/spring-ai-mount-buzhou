# Spec 1806 — 检查点滞后读面（effort #1806，R7）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2813–T2814，impl 1407）。借鉴：
> Kafka consumer-group lag（log end offset − committed offset = 重放长度）/
> SQLite WAL checkpoint（未检查点化帧数 = 恢复 replay 成本）。

## Problem Statement

会话有事件产出与检查点两套水位，但「两者差多少」无处可问：崩溃后要重放
多少事件（恢复成本）、哪些会话滞后越限（恢复 SLA 风险面）、追平率多高
（检查点节奏健康度）——没有统一读数，检查点频率只能拍脑袋调。

## Solution

`CheckpointLagReadout`（core/recovery，静态纯函数）：

- `SessionLag(sessionId, eventsProduced, eventsCheckpointed)` 单会话事实（紧凑
  构造器核契约：id 非空、0 ≤ checkpointed ≤ produced；`lag()` = 差值）；
- `analyze(lags)` → `LagReport(lags, totalLag, maxLag, laggiestUser)`：合计
  滞后（全会话重放成本）、最坏单会话（崩溃重放上界，无会话 -1 哨兵）、
  并列取首个（稳定复现）；
- `sessionsBeyond(threshold)` 越限计数 + `caughtUpRatio()` 追平率（无会话
  -1 哨兵）。

## User Stories

1. 作为运维者，maxLag=400 直接读出最坏会话崩溃要重放 400 事件——恢复
   SLA 心里有数。
2. 作为容量治理者，sessionsBeyond(100)=8 → 检查点节奏跟不上这 8 个高产
   会话，该调频或按产出自适应。
3. 作为框架宿主，水位口径（事件序列号/检查点时点）自声明，纯读面零侵入。

## Implementation Decisions

- 纯读面零状态，不动检查点写入路径；滞后本身不是错（持续增长才是），
  趋势裁决归宿主。
- 契约 fail-fast 在 record 构造器；null 按空表。

## Testing Decisions

- 聚合账目+越限+追平率；并列最坏取首；全追平健康态；空表/null 哨兵；
  畸形（负计数/检查点超前/空 id）fail-fast。

## Out of Scope

- 不自动调整检查点频率；不接指标导出。

## Further Notes

- 与 FailureTurnSnapshots 正交：那是失败轮快照内容，这是水位差读数。
