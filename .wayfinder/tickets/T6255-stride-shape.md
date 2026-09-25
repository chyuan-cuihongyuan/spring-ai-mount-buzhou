---
id: T6255
title: T 会话 T28 Stride Scheduler 步幅调度的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

比例份额怎么确定性无随机？（spec 6027 /
effort #6027 / T28）

## Resolution

**StrideScheduler（core/concurrent，源码 T24 预载）**：
stride = BASE/tickets，pass += stride 单调记账，serve 恒选
pass 最小（并列 id 小）；长程次数精确∝票数；remove/passOf/
ticketsOf 读数；重复/缺席/票数≤0 fail-fast。
