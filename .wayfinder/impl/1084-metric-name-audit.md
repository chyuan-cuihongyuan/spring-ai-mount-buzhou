# 1085 — 指标命名校验器

**What to build:** MetricNameAudit 纯函数（validate→NameVerdict 违规闭集首违不短路）+ 六测。

**Blocked by:** None.

**Status:** done

- [x] MetricNameAudit（core/metrics，private 构造静态面，段规则与门同源）
- [x] MetricNameAuditTest 六测
- [x] spec 1431 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='MetricNameAuditTest'` 6/6 绿。
