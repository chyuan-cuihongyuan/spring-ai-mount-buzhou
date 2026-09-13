# 948 — outbox 重试次数分布读面

> 来源：I 会话第 46 轮 = effort #948（impl 695）。重试积压结构可见——「积压集中在首轮还是深轮」一读即知。

## 背景

`WebhookOutbox` 有 pendingCount（总量）与 pendingOldest（最老），但重试积压的**结构**不可见：退避重试集中在 attempts=1（新故障）还是深轮（持续故障）分不清。

## 目标

- `WebhookOutbox.retryDistribution()` 包级读面：attempts → 条数（TreeMap 升序）；损坏记录跳过（隔离归 due 路径口径）；
- `appendRetry(OutboxRecord)` 包级退避记录落盘（测试/恢复工具用）；
- `entry(OutboxRecord)` 可见性放宽为包级（同包测试可达）。

## 兼容性

纯读面 + 包级可见性放宽；行为零变化。
