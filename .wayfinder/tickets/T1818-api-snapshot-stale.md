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

1. **定性**：快照门设计即「快照跟随代码」——新增公共类型（added，非破坏）的处置 = regenerateSnapshot + api-surface.md 同步入档（测试 Javadoc 明文指引）。落地会话漏走快照更新步——流程缺口非代码缺陷。
2. **修复**：`docs/api-surface.snapshot.txt` 按字典序补 PerHostConcurrencyGuard 一行（HttpRequestTool 与 SsrfGuard 之间）；`docs/api-surface.md` buzhou-tools 段同步条目（`public final class`，spec 1603 标注）。
3. **并行收敛注记**：修复落工作区后，I 会话快照再生轮（64da66cf）以等价内容先行入库（快照行规范化同文、md 条目文本一致），K 线票追认归属；reactor 联编复验 starter 门绿（注：`-pl` 半反应器 classpath 会假红——该门依赖多模块 /classes 目录扫描，全反应器才是有效口径，设计边界入档）。
4. **护栏注记**：「新增公共类型 → regenerate + md 同步」两步仍靠自觉；预 regenerate 钩子不做——人工核对 diff 是有意设计的闸门。
