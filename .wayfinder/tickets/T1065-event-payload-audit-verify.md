---
id: T1065
title: 事件 payload 大小审计验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1064]
created: 2026-09-13
---

## Question
字节口径与聚合如何证明？

## Resolution
**用户常设授权 AFK（可推翻）**

验证（G 会话第 33 轮 = effort #732）：两类型降序+max 与合计一致/空表/null fail-fast。buzhou-core 全模块零回归（C 会话排除集）。
