---
id: T1534
title: todo 动作分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1533
created: 2026-09-14
---

## Question

J 会话第 40 轮：动作分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（TodoActionStatsTest，复用 InMemorySessionStateStore+ToolContext 骨架）：fresh 五桶全零；四动作各计其桶（list/upsert/remove/clear 各 1、total=4）；未知动作归 other 桶；重复调用累计；快照不可变。定向 `mvn -pl buzhou-tools test -Dtest='TodoActionStatsTest,TodoToolTest'` 绿。
