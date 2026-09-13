# 759 — 会话面包屑环形读面

**What to build:** EventBreadcrumb 公共 record + BreadcrumbRing 内部有界环 + deliverEvent 双模式漏斗记录 + AgentSession.breadcrumbs() 读面 + 环行为测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] EventBreadcrumb（epochMillis/type，无 payload——敏感红线）
- [x] BreadcrumbRing（32 新→旧 + snapshot/clear，包私有直测）
- [x] DefaultAgentSession.deliverEvent 记录点 + breadcrumbs() 覆写
- [x] AgentSession default breadcrumbs() 空表
- [x] BreadcrumbRingTest（新→旧/有界/不可变/clear）
- [x] spec 1006 + README 行 + API 快照与 api-surface.md 增行

## Done

验证：`mvn -pl buzhou-core test -Dtest='BreadcrumbRingTest,EventDropBreakdownTest'` 全绿。commit 见本轮 `feat(core)` 提交。
