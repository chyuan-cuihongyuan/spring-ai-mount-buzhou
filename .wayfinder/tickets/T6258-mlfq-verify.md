---
id: T6258
title: T 会话 T29 MLFQ 多级反馈队列的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6257]
created: 2026-09-26
---

## Question

T29 合同怎么逐一验绿？（spec 6028 / effort #6028 / T29）

## Resolution

**验证通过**：MlfqTest 五测全绿——长任务逐级沉底+量子翻倍
+底级不降；新任务插队；同级 FIFO；完成移出；fail-fast。
