---
id: T6094
title: R 会话 R47 fail2ban 封禁递升的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6093]
created: 2026-09-24
---

## Question

R47 合同怎么逐一验绿？（spec 4046 / effort #4046 / R47）

## Resolution

**验证通过**：BanEscalationTest 六测全绿——满限禁 + 窗滑动
豁免；递升三连封顶；禁期失败不复禁；解禁重计；独立 offender
互不牵连；定构畸形 fail-fast。
