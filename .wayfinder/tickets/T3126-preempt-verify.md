---
id: T3126
title: 抢占重算账本的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3125]
created: 2026-09-17
---

## Question

PreemptionLedger 合同（双面账/净收益/浪费率/幂等/空账/畸形）怎么钉住？（spec 2012 / effort #2012 / R13）

## Resolution

**六用例一次全绿**（buzhou-core）：单笔双面账（40:100 → 净赚 60、
浪费率 40/140）/ 多笔累计净负 / 重算幂等（同 victim 一次）+重算率
0.5 / 空账零不除零 / 净收益正负零三态 / 畸形四型 fail-fast。
