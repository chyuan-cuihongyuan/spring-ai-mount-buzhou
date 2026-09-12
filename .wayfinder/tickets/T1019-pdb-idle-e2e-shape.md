---
id: T1019
title: PDB×空闲压缩联动补验的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

IdleSessionMonitor（spec 179 候选）与 SessionAvailabilityFloor（spec 704 闸）联动语义未闭环。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 35 轮 = effort #734 / spec 734 / impl 537，测试域补验轮）：联动用例——sweep 空闲候选在水位不足时逐个被 floor 拒（archived 不增长、计数=候选数）；水位恢复后放行归档。
