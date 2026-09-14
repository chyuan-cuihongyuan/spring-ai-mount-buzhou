# 1092 — Spill 冷热分层访问审计

**What to build:** SpillTieringAudit 纯函数（读事件按 uri 聚合+三桶+占比派生）+ 四测。

**Blocked by:** None.

**Status:** done

- [x] SpillTieringAudit（spill，private 构造静态面）
- [x] SpillTieringAuditTest 四测
- [x] spec 1441 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-spill -am test -Dtest='SpillTieringAuditTest'` 4/4 绿。
