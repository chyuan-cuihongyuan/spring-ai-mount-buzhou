---
id: T3112
title: P 会话 R6 对账轮的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3111]
created: 2026-09-17
---

## Question

R6 对账轮的四件事怎么逐一验绿？（spec 2005 / effort #2005 / R6）

## Resolution

**验证通过**：全仓 mvn verify 16 模块 BUILD SUCCESS（三门：覆盖门
JaCoCo LINE ≥70% / enforcer 依赖收敛 / 快照门+SpecCoverage+P 对账门）；
快照 diff 恰 +8 行与预期清单一致；PSession2000LedgerAuditTest
spec 2000–2005 六号四件套齐整；push 视网络恢复情况（不通则积压待推，
不阻塞内容轮）。
