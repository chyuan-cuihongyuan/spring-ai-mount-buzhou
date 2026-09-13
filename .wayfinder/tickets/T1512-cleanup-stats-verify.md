---
id: T1512
title: 会话级联清理聚合计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1511
created: 2026-09-14
---

## Question

J 会话第 30 轮：级联清理聚合计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CleanupStatsTest，Buzhou.inMemoryStores 直构骨架）：删除一个会话 → deleteCalls=1、cleanedTargets = 目标数；带一个抛错贡献者 → failuresByTarget 含贡献者名且其余目标照清；多会话累计对账；fresh 零值。定向 `mvn -pl buzhou-core test -Dtest='CleanupStatsTest,SessionCleanerTest'` 绿（后者存在则回归）。
