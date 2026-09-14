# 1078 — 会话历史形态审计

**What to build:** ConversationShapeAudit 纯函数（roleHistogram+consecutiveSameRole+emptyContent+maxTurnGap）+ 七测。

**Blocked by:** None.

**Status:** done

- [x] ConversationShapeAudit（core/message，private 构造静态面）
- [x] ConversationShapeAuditTest 七测
- [x] spec 1425 + README 行（纯函数静态面+嵌套 record——快照面仅增外层类）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ConversationShapeAuditTest'` 7/7 绿。
