---
id: T1487
title: taint 信息流控制生命周期计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 19 轮：taint 信息流控制生命周期计数读面（FIDES 判定分布）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 19 轮 = effort #1018 / spec 1018 / impl 771）：缺口成立——taint 双钩子（读侧打标 TaintTrackingHook、写门 TaintWriteGateHook，FIDES 信息流控制最小落地）零计数：打了多少次标、首次标 TRUSTED→UNTRUSTED 跃迁几次、写门判定的四分桶（写侧检查/可信放行/已批准放行/拦截）全程不可见——FIDES 策略调优（写侧清单是否过大、审批积压）无据。落点 buzhou-guard taint 包：TaintTrackingHook 嵌套 `TaintMarkStats(marksApplied, firstMarks)` + stats()；TaintWriteGateHook 嵌套 `GateStats(checkedWriteCalls, allowedTrusted, allowedApproved, blocked)`（守恒 checked == trusted + approved + blocked）+ stats()。实例级；嵌套类型不动 API 快照；行为逐位不变。
