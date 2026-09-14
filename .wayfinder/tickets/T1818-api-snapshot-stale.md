---
id: T1818
title: API 快照过期（PerHostConcurrencyGuard 未入册致 starter 门红）——R6 对账显形
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-15
---

## Question

R6 全仓 verify（隔离 worktree，固定提交点）在 buzhou-spring-boot-starter 失败：`ApiSurfaceSnapshotTest.publicTypeUniverseMatchesSnapshot` 红——【非破坏】新增公共类型 1 项 `buzhou-tools|...http.PerHostConcurrencyGuard`（N 会话 R4 引入，快照未随更）。谁的责任边界、如何收口？

## Resolution

**用户常设授权 AFK（可推翻）**

处置（K 会话 R6 对账显形，独立票独立 commit）：

1. **定性**：快照门设计即「快照跟随代码」——新增公共类型（added，非破坏）的处置 = regenerateSnapshot + api-surface.md 同步入档（测试 Javadoc 明文指引）。N 会话落地新类时漏走快照更新步——流程缺口非代码缺陷。
2. **修复**：`docs/api-surface.snapshot.txt` 按字典序补 `buzhou-tools|...PerHostConcurrencyGuard` 一行（H < P < S，位于 HttpRequestTool 与 SsrfGuard 之间）；`docs/api-surface.md` buzhou-tools 段同步条目（`public final class`，spec 1603 标注）。
3. **验证**：隔离 worktree 重跑 starter 测试类确认门绿（reactor classpath 口径，单模块跑按设计跳过）。
4. **护栏注记**：本票后「新增公共类型 → regenerate + md 同步」两步仍靠自觉——自动化（快照 diff 提示进 CI 失败消息已有；预 regenerate 钩子不做，人工核对 diff 是有意设计的闸门）。
