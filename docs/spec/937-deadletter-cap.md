# 937 — webhook 死信环形上限

> 来源：I 会话第 37 轮 = effort #937（[T1317](../../.wayfinder/tickets/T1317-deadletter-cap-shape.md) / [T1318](../../.wayfinder/tickets/T1318-deadletter-cap-verify.md) / impl 689）。有界纪律（ErrorSignatures/TagCardinalityGuard 封顶同先例）。

## 背景

outbox 容量管未决记录（outbox.*，spec 24 软上限拒入），死信（dead.* 前缀）是独立前缀——**持续 markDead 会无限累积**（查询有 limit 但存量无界），状态存储被死信慢性侵占。

## 目标

- `WebhookOutbox.MAX_DEAD_LETTERS = 256`（static final）；
- `markDead` 写入前 `evictOldestDeadIfFull`：死信数达上限时按 createdAt 升序丢最旧一条（全量 O(n)，n ≤ 上限+1）；
- 保留最新语义（最新 256 条死信永远可查——排障价值优先）；
- 既有 markDead/deadLetters/requeueDead 语义零变化（仅增量环形清理）。

## 兼容性

行为增量：存量超 256 的既有部署会在下一次 markDead 时收敛到上限（单次最多丢 1 条最旧——渐进收敛无尖峰）。
