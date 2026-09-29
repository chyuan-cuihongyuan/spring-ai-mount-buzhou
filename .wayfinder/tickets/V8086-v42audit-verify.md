---
id: V8086
title: V 会话 V42 周期对账的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8085]
created: 2026-09-30
---

## Question

V42 对账怎么验绿？（spec 8042 / effort #8042 / V42）

## Resolution

**验证通过**：快照门 diff 仅 +5；对账门四测绿（spec 8000–8041
零缺位）；组合定向 verify 绿。
