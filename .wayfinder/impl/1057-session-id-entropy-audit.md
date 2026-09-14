# 1057 — 会话 id 熵审计

**What to build:** SessionIdEntropyAudit 纯函数（audit 单 id 字母表下界+四档闭集 / auditAll 四桶）+ 六测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SessionIdEntropyAudit（core/session，private 构造静态面）
- [x] SessionIdEntropyAuditTest（UUID/时间戳/可猜串/INVALID/单调链/批量）
- [x] spec 1404 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='SessionIdEntropyAuditTest'` 6/6 绿。
