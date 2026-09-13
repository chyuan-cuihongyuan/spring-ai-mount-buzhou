---
id: T1107
title: 检索多路改写融合的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

查询改写维度如何补位？与既有 RRF 双信号融合如何辨义切分？生成器故障语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 4 轮 = effort #803 / spec 803 / impl 556）：`MultiQueryRetriever`——variants/base 双函数注入；跨变体 RRF k=60、消息 id 去重累加；变体封顶 8；生成器空/异常回退单路、单路检索故障隔离（fail-open 双层）；mode=multi-rrf 留痕；与 605 的「单查询双信号」正交（多查询各排名融合）。
