---
id: T1457
title: 策略层级归属读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 4 轮：策略层级归属解析（spring config insights layer attribution）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 4 轮 = effort #1003 / spec 1003 / impl 756）：缺口成立——LayeredPolicy.get 按 binding > yml > defaults 三层优先级解析，但「生效值来自哪一层」不可见（配置排查要肉眼对三层）。落点 core.policy：新公共 record `PolicyLayerAttribution(dottedKey, Layer, value)`（嵌套枚举 `Layer = DEFAULTS | YML | BINDING | ABSENT`，ABSENT⇔value==null 构造校验）+ `LayeredPolicy.getAttributed(dottedKey)`；get() 重构为归因路径薄封装（同序同判，逐位零行为变化）。**无计数器**——纯函数诊断面，无生产调用方则计数为假面；getMap 的逐叶归因 out of scope（合并语义复杂留档）。
