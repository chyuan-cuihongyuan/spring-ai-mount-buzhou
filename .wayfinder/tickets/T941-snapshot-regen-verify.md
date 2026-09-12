---
id: T941
title: 快照再生轮的验证
type: task
status: closed
assignee: zcode-f
blocked-by: T940
created: 2026-09-13
---

## Question

再生后门真绿（reactor 口径）？api-surface.md 与快照一致？全仓 verify 通过？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（F 会话第 46 轮）：① 快照 diff 恰 3 行（三个新类，无意外面）；② ApiSurfaceSnapshotTest reactor 口径绿；③ 全仓 `mvn -B -ntp clean verify` 绿（含 JaCoCo ≥70% / enforcer / SpecCoverageTest）。E 会话遗留债务清零。
