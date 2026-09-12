# 739 — 限速×死信路径隔离补验

> 来源：G 会话第 40 轮 = effort #740（spec 718/24 补验）/ [T1029](../../.wayfinder/tickets/T1029-ratelimit-deadletter-shape.md) / [T1030](../../.wayfinder/tickets/T1030-ratelimit-deadletter-verify.md) / impl 542。

## 背景

限速 defer 与死信（FATAL）路径共享投递循环——defer 必须不干扰死信状态机（defer 在 attemptOnce 之前，死信在 outcome 之后）。

## 目标（测试域补验轮）

- defer 记录零 attempts 增长、零死信迁移；
- 令牌恢复后记录正常走投递/重试/死信全路径；
- 限速下死信重放（requeueDead）产物仍受闸约束（不绕闸）。

## 兼容性

纯测试域增量。
