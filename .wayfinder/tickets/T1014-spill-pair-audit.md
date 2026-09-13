---
id: T1014
title: spill 双文件配对完整性巡检的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

spill 双写崩溃留下残缺对——属主会话活着时 sweepOrphans 永远不管，配额被静默吞噬。做配对审计面吗？要不要顺手删？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 8 轮 = effort #707 / spec 707 / impl 607）：`SpillPairAudit.audit(rootDir)` 纯函数只读——Report(findings, dataFiles, metaFiles, dataBytes)，DATA_WITHOUT_META 带字节量/META_WITHOUT_DATA；uri 相对根正斜杠归一；root 缺席=诚实零。只读不删（housekeeper 接线轮再议——538 同纪律）。三层完整性矩阵：sweepOrphans（会话）→本面（文件对）→ReadIntegrity（内容）。
