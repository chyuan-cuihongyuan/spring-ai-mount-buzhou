---
id: T1048
title: 会话状态 TTL 覆盖审计的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

永生状态键（ttlTurns=null）堆积无读数面——做覆盖审计吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 25 轮 = effort #724 / spec 724 / impl 624）：`StateTtlCoverage.analyze(Map<String,StateEntry>)` 纯函数——totalKeys/persistentKeys/ttlKeys+coverage（total=0 空真 1.0）+byProducer 分行（永生键的 producer 归因——治理焦点）。纯读数不补 TTL。core/cleanup 落点（fsck 族）。
