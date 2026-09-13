# 1027 — 手动压缩操作分布读面

> 来源：J 会话第 28 轮 = effort #1027（[T1505](../../.wayfinder/tickets/T1505-compact-op-stats-shape.md) / [T1506](../../.wayfinder/tickets/T1506-compact-op-stats-verify.md) / impl 780）。与 R9/R25 同族：操作结果从**逐次返回值**升为**累计水位**（Kubernetes 事件聚合思想——逐事件有了，聚合水位补齐）。

## Problem Statement

ManualCompactor.compact（spec 20 宿主侧手动压缩，与 compact_now 工具同管线）逐次返回 CompactResult（skipped/foldedMessages/error 字段俱全），但无跨调用聚合：手动压缩累计完成/跳过/失败几次、累计折入多少消息不可见——宿主侧运维（压缩频繁失败=摘要管线异常；skipped 占比高=触发时机过密）无水位。

## 目标

- `ManualCompactor` 增量（buzhou-memory compact 包，实例级）：`compactAttempts` / `completed` / `skipped` / `failed` / `foldedMessages` 五 AtomicLong——守恒不变量 **completed + skipped + failed == attempts**（foldedMessages 为完成路径折入消息累计）。
- 嵌套 record `CompactOpStats(long attempts, long completed, long skipped, long failed, long foldedMessages)` + `stats()` 快照。
- compact 语义逐位不变（幂等 skip、异常折入 failed 结果——仅加计数）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 按 sessionId 分桶（会话数无界——基数纪律；单会话水位可由调用方按 sessionId 自行调 compact 后查返回值）。
- 模型侧 compact_now 工具计数（工具调用计数已由 spec 13 全局 counter 覆盖）。
