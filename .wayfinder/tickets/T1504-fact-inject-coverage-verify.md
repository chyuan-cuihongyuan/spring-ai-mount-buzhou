---
id: T1504
title: 事实注入覆盖读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1503
created: 2026-09-14
---

## Question

J 会话第 27 轮：注入覆盖读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（FactInjectCoverageTest，InMemorySessionStateStore + 内联 FactDefinition 骨架）：无上限渲染全注入 injected=事实数；maxChars 约束下省略计数与指针附尾文本并存；空仓渲染 empty 不计；renders 计非空产出。定向 `mvn -pl buzhou-guard test -Dtest='FactInjectCoverageTest'` 绿。
