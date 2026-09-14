# 1089 — 事件时序单调性审计

**What to build:** EventOrderAudit 纯函数（analyze→Report 逆序对数/最大倒退/首逆序定位）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] EventOrderAudit（core/observability，private 构造静态面）
- [x] EventOrderAuditTest 五测
- [x] spec 1436 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='EventOrderAuditTest'` 5/5 绿。
