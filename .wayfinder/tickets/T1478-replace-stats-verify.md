---
id: T1478
title: Hook Replace 载荷应用/丢弃计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1477
created: 2026-09-14
---

## Question

J 会话第 14 轮：Replace 应用/丢弃计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（HookChainReplaceStatsTest，复用 DefaultTurnContext/RecordingHook 同款骨架）：合法载荷（String 改写输入）→ replaceApplied=1 且替换真实生效；非法载荷（Integer）→ replaceDropped=1 且分发继续（CONTINUE 语义不变）；混合多 hook 多轮累计对账；零行为变化（链后续 hook 照常执行）。定向 `mvn -pl buzhou-core test -Dtest='HookChainReplaceStatsTest,HookChainTest'` 绿。
