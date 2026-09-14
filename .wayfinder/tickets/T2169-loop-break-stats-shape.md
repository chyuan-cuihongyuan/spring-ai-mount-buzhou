---
id: T2169
title: 工具循环打断分布读面（ToolLoopBreakerHook 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 34 轮：循环打断分布面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：ToolLoopBreakerHook 只有全局 counter；per-tool 打断分布与打断时 run 长度水位缺失。

形状裁决：Hook 手术式增量——brokenByTool 计数表（256 折叠纪律）+brokenByToolSnapshot 降序典序+brokenTotal+maxRunObserved 水位（打断时点 run 长度单调）+resetBrokenForTest 清分布不清会话 run 状态；打断路径单点记账放行零动作，Block 语义逐位不变。

Out of scope：per-session 分桶；参数相似度；自动升级。
