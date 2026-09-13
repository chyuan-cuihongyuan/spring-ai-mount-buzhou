---
id: T1108
title: 检索多路改写融合验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1107]
created: 2026-09-13
---

## Question

RRF 累加/去重/故障隔离/封顶如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 4 轮 = effort #803）：MultiQueryRetrieverTest 9 例——双变体命中登顶+RRF 精确值 1/61+1/62/三变体同消息单次出现 3/61 累加/单路异常隔离继续/生成器抛异常回退原查询/空与 null 变体回退/20 变体截断 8/空查询 null/limit≤0 默认/limit 截断/双函数 null fail-fast。
