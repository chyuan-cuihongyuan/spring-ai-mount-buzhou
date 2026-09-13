---
id: T1509
title: 模型预算闸判定分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 30 轮：模型预算闸判定分布读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 30 轮 = effort #1029 / spec 1029 / impl 782）：缺口成立——ModelBudgetGate（spec 530 per-model 预算闸）beforeModel 拦截零计数：预算闸检查多少次、放行多少、因耗尽拦截多少不可见——预算耗尽后的「连续拦截水位」是预算配置合理性（过低/忘记调额）的第一信号；事件面无此闸的专门事件。落点 core/budget：实例级 checks/allowed/blocked 三 AtomicLong（守恒 checks == allowed + blocked；拦截时 ledger 已记账数值由既有 block 文本携带，不重复入桶）+ 嵌套 record `BudgetGateStats(checks, allowed, blocked)` + `stats()`。实例级；嵌套类型不动 API 快照；判定返回值逐位不变。
