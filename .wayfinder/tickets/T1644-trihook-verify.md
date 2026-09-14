---
id: T1644
title: guard 三 hook 链顺序协作组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1643
created: 2026-09-15
---

## Question

J 会话第 94 轮：三 hook 链组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（TriHookChainReadoutTest，HookEnvironment 骨架）：三 hook 链调用后双 stats 各自守恒 + 链路语义计数一致。定向 `mvn -pl buzhou-guard -am test -Dtest='TriHookChainReadoutTest'` 绿。
