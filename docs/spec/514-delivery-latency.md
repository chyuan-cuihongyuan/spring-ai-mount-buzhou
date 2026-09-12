# Spec 514 — 投递时延分位数（effort #514）

> wayfinder map：`.wayfinder/maps/effort-514.md`（T779–T780）。E 会话第 15 轮。

## Problem Statement

webhook 投递面有 lag 滞后（135——还没送的看得到）、投递成功的**耗时
分布**不可见：事件从产生到送达要多久没有 p50/p95/p99 读数。

## Solution

`webhook.WebhookDeliveryLatency`（416 分位族同法）：

- 样本滚动窗（有界 512）；`record(latencyMillis)` 非负才收（时钟回拨防御）。
- `snapshot()` → `Snapshot(deliveredCount, p50, p95, p99, max)`——exact
  最近秩；零样本分位 = null（诚实空值不画零假象，416 同口径）。
- 接线：WebhookEventForwarder DELIVERED 分支记
  `now − record.createdAtEpochMs()`；`setDeliveryLatency` setter（105
  setIncludeTypes 同法）默认 null 零变化。

## User Stories

1. 作为运维，我想看已投递事件的时延分布， so 「送是送到了但延迟 20 分钟」
   的劣化可见（135 只看积压）。

## Implementation Decisions

- 只记成功投递样本（死信/退避中不是「送达」——语义诚实）。
- 进程内窗口（512）——重启清零（观察面口径）。

## Testing Decisions

- 1..100 样本 exact 分位；零样本 null；窗口有界+负值忽略；forwarder
  本地 HTTP E2E 成功投递后样本落位。

## Out of Scope

- per-type 时延；持久化历史；自动告警。

## Further Notes

- 新公共类型 `WebhookDeliveryLatency`（嵌套 `Snapshot`）随轮 regenerate
  快照 + api-surface.md 加行。
