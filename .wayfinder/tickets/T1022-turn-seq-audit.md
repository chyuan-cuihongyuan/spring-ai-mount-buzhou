---
id: T1022
title: 消息序列连续性审计的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

store 级丢数据/重复在读取侧表现为「上下文缺一段」——序列连续性无审计面。做纯函数原语吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 12 轮 = effort #711 / spec 711 / impl 611）：`TurnSequenceAudit.audit(List<Marker>)` 纯函数——Marker(turnSeq,seqInTurn) 由调用方从任意 store 投影；单遍状态机判 GAP/DUPLICATE/OUT_OF_ORDER 三类；起始（0,0）连续约定；空表零发现 null fail-fast。不接 store 不修复（housekeeper 接线留后续——538 节奏）。Kafka offset 审计思想。
