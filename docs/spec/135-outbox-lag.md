# Spec 135 — outbox 积压滞后面（effort #96）

> wayfinder map：`.wayfinder96/MAP.md`（T483–T484）。借鉴：Kafka consumer lag
> （`consumergroup_lag` 面板直觉）——「积压多少 / 最老的等了多久」一读即知
> 投递是否停摆。

## Problem Statement

webhook outbox 的投递健康现在有两副眼镜：due 审计（spec 96，索引一致性）与
死信查询（spec 24）。但「现在积压多少、最老的一条已等多久」这一运维第一问
没有直接读数——退避中的记录（nextAttemptAt 在未来）连 `due()` 都不出现，
恰是停摆最严重的部分反而最不可见。

## Solution

`WebhookOutboxLag`（core/webhook，公共类）：

- **读数**`read(scanLimit)`：`Lag(pendingCount, oldestPendingAgeMillis, oldestEventId,
  deadCount)`——pending 走 countByPrefix 下推；最老积压扫 OUTBOX_PREFIX 取
  min(createdAt)（<b>含退避中</b>——入队即计时）；死信计数同步给出。
- **停摆判定**`stalled(threshold, scanLimit)`：最老积压 age ≥ 阈值（无积压 = false）。
- **gauge 绑定**`bindGauges()`：`buzhou.webhook.outbox.pending` /
  `buzhou.webhook.outbox.oldest-age-ms`（Supplier 活读）——显式绑定（autoconfig
  接线后续按需）。
- 诚实边界：最老积压扫描为全量读值（容量软上限内可接受；count 侧下推不受影响）。

## User Stories

1. 作为运维，面板上 pending 与 oldest-age 两个数字告诉我投递是否健康——
   age 持续爬升 = 停摆，比翻审计日志快一个数量级。
2. 作为运维，stalled(5min) 可直接接告警——阈值语义清晰（最老积压等了超过 5 分钟）。
3. 作为宿主，死信数并列可见——「积压在涨且死信在涨」与「只是慢」一眼可分。

## Implementation Decisions

- `WebhookOutbox` 增包内 `pendingOldest(scanLimit)`（scanByPrefix 解析取 min
  createdAt；损坏记录跳过——隔离归 due() 路径既有语义）。
- `Lag` 为不可变 record；age 由 Clock 注入计算（测试确定性）。
- gauge 用 Supplier 活读（无缓存——读时即时）。

## Testing Decisions

- 空 outbox：无积压 age=-1、stalled=false。
- 积压三条 + 时钟推进：pending=3、age=推进量、oldestEventId 正确。
- 投递（delete）后计数回落；markDead 后 deadCount 升、pending 降。
- stalled 阈值翻转（推进时钟跨阈值）；退避后移（nextAttemptAt 未来）仍计入 age。
- 先例：WebhookOutboxDueIndexTest（包内直构 outbox）。

## Out of Scope

- per-sink lag（多目的地后续）；lag 历史/直方图；autoconfig 定时采样。

## Further Notes

- 与 spec 96 due 审计互补：审计管索引对账，本面管实时滞后。
