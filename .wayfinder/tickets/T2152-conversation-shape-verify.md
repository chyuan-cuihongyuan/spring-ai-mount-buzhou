---
id: T2152
title: 形态审计异常口径的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2151
created: 2026-09-14
---

## Question

如何证明异常口径（连续/空内容/跳变）与正常形态不误报？

## Resolution

**用户常设授权 AFK（可推翻）**

`ConversationShapeAuditTest` 七测全绿（`mvn -pl buzhou-core -am test`）：空输入哨兵；健康会话零异常；连续 USER 异常计 1；**连续 TOOL 正常不误报**（并行工具形态）；空内容两条显形；turnGap=5 跳变显形；直方降序。评审修正：初版 BuzhouMessage 构造 9 参不全（实际 12 参）编译错修正。
