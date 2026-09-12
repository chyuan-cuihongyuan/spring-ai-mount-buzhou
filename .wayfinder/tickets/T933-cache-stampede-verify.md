---
id: T933
title: 缓存 miss 惊群合并的验证
type: task
status: closed
assignee: zcode-f
blocked-by: T932
created: 2026-09-13
---

## Question

合并真省了模型调用？失败降级语义钉住了吗？默认关零变化吗？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（F 会话第 42 轮）：① 并发 N 相同 key miss → ScriptedChatModel 调用计数 = 1、N 方同结果、coalescedWaiters = N-1；② leader 失败 → 等待者各自直调（失败不共享，模型调用数 = N）；③ 默认关（coalescing 缺席）→ 既有行为零变化；④ yml 绑定用例（opt-in 声明即装配 coalescer）。`mvn -pl buzhou-resilience -am test` 全绿。
