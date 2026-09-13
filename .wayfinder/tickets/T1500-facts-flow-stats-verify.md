---
id: T1500
title: facts 段导入导出行数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1499
created: 2026-09-14
---

## Question

J 会话第 25 轮：行数读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（FactsFlowStatsTest，InMemorySessionStateStore 直构 + 手工 put fact.* 键）：导出 2 条 → factsExported=2；空会话导出 → 返回 null 不计；导入 2 行 JSON → factsImported=2 且 state 落库可读；坏 JSON 导入 → importFailures=1 且异常照抛（原语义回归）。定向 `mvn -pl buzhou-memory test -Dtest='FactsFlowStatsTest'` 绿 + 既有 FactsExporter 回归绿。
