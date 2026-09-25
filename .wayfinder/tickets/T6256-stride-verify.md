---
id: T6256
title: T 会话 T28 Stride Scheduler 步幅调度的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6255]
created: 2026-09-26
---

## Question

T28 合同怎么逐一验绿？（spec 6027 / effort #6027 / T28）

## Resolution

**验证通过**：StrideSchedulerTest 五测全绿——{1,1,2} 400 次
恰 {100,100,200}；两客户端严格交替；同注册序列全等；
remove 排除；fail-fast。
