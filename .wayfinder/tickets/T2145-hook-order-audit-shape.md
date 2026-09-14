---
id: T2145
title: Hook 顺序碰撞审计（HookOrderAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 23 轮：钩子装配脆性审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：ChainComposition 同序按名字典序兜底——分发确定但保序是「名字巧合」，重命名即变序；无碰撞审计面。

形状裁决：HookOrderAudit 纯函数（core/hook）——analyze(List<BuzhouHook>)→Report(hookCount/collisions/collisionCount)；OrderGroup(order/hookNames 字典序)仅列 ≥2 同序组、组间 order 升序；空清单哨兵；只读不裁决（修复=显式错开 order）。

Out of scope：运行期阻断；合成命名；阶段级区分。
