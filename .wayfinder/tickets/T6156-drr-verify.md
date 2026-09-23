---
id: T6156
title: S 会话 S28 Deficit Round Robin 亏空调度的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6155]
created: 2026-09-24
---

## Question

S28 合同怎么逐一验绿？（spec 5027 / effort #5027 / S28）

## Resolution

**验证通过**：DeficitRoundRobinTest 五测全绿——quantum 记账
（15>10 结转、次轮 20≥15 发出）；队空亏空清零；容量满拒；
未知队列/size≤0 fail-fast；确定性回放。
