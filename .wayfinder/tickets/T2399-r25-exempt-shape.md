---
id: T2399
title: R25 危险工具 HITL 豁免征询的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2398
created: 2026-09-15
---

## Question

N 会话第 25 轮：GuardExemptionRegistry 首个消费者选哪个 hook、豁免粒度多粗？

## Resolution

选 **危险工具 hook + 工具名粒度**（mechanism=dangerous-tool、subject=toolName）。
危险工具是 HITL 疲劳的主要来源（高频安全工具反复确认）；工具名粒度足够
（参数指纹粒度的豁免操作成本高于收益——登记价值在「这个工具可信」）。
征询点在授权标记之后：已走完一次确认的 session 级授权语义优先，豁免兜底。
