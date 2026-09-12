---
id: T940
title: E 会话合并后快照门红的收口裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-13
---

## Question

E 会话 PR #19 合并时快照 union 但「快照待 regenerate 复验」——F 会话 41–43 轮三个新公共类（SessionForkKeys / ResponseCacheCoalescer / RollingJsonlWriter）未入快照，ApiSurfaceSnapshotTest 红（reactor/CI 必挂）。怎么收口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 46 轮 = effort #600 / spec 645 / impl 498）：显式 regenerateSnapshot（-Dbuzhou.api-snapshot.regenerate=true，spec 615 硬化门合规触发）再快照 + api-surface.md 同步补两行（RollingJsonlWriter 入 core 主段、ResponseCacheCoalescer 入 resilience 段；SessionForkKeys 第 41 轮已补）+ 全仓 `mvn verify` 周期性复验（16 模块 + JaCoCo 70% 门 + enforcer）。
