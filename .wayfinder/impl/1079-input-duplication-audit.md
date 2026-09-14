# 1079 — 用户输入重复审计

**What to build:** UserInputDuplicationAudit 纯函数（归一化+复读对/游程+Top 榜）+ 六测。

**Blocked by:** None.

**Status:** done

- [x] UserInputDuplicationAudit（core/message，private 构造静态面）
- [x] UserInputDuplicationAuditTest 六测
- [x] spec 1426 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='UserInputDuplicationAuditTest'` 6/6 绿。
