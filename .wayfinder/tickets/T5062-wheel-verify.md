---
id: T5062
title: Q 会话 R31 时间轮的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5061]
created: 2026-09-18
---

## Question

R31 合同怎么逐一验绿？（spec 3030 / effort #3030 / R31）

## Resolution

**验证通过**：HashedWheelTimersTest 七测全绿——到期边界三段、
多轮滞槽（80ms 轮长延迟 300：240 不出 300 出）、槽回绕、同刻
id+跨刻 deadline 序、2000 随机操作守恒逐次+终局清空、粒度边界
（delay 0 下一 tick）、参数三路 fail-fast。
