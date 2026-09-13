---
id: T1511
title: 会话级联清理聚合计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 30 轮：会话级联清理聚合计数读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 30 轮 = effort #1030 / spec 1030 / impl 783）：缺口成立——SessionCleaner（impl-35 级联清理协调器）逐次返回 SessionCleanupResult（cleaned/failures 明细俱全）但**无跨调用聚合**：删了多少会话、哪个清理目标（message-store/session-lease-store/贡献者…）反复失败无水位——单个 store 实现持续故障的信号被逐次 ERROR 日志稀释。落点 core/cleanup：实例级 deleteCalls/cleanedTargets/failedTargets 三计数 + failuresByTarget 分桶（ConcurrentHashMap，目标名固定有界）+ 嵌套 record `CleanupStats` + `stats()` 快照。实例级；嵌套类型不动 API 快照；清理行为逐位不变。
