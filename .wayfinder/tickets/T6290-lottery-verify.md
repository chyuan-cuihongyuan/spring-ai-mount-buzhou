---
id: T6288
title: T 会话 T44 Lottery Scheduler 彩票调度的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6289]
created: 2026-09-28
---

## Question

T44 合同怎么逐一验绿？（spec 6044 / effort #6044 / T44）

## Resolution

**验证通过**：LotterySchedulerTest 四测全绿——{1,3} 票 4000
抽收敛 ±10%；同种子双实例序列全等；注销排除+读数归位；
票数 0/重复注册/缺席 fail-fast。
