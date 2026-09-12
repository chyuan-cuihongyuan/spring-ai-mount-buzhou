---
id: T1032
title: Todo 陈旧度审计读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

todo 滞留（in_progress 挂 N turn）无读数面——做审计原语吗？轮次口径还是墙钟？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 17 轮 = effort #716 / spec 716 / impl 616）：`TodoStalenessAudit.analyze(items,currentTurn,staleAfterTurns)` 纯函数——per-item age（轮次口径）、滞留=未完成且 age>阈值按 age 降序、byStatus 计数、oldestOpenAge、promptHint() 一行人话供注入面拼装；阈值≤0 只报统计。纯读数不自动清理（TodoItem schema 不动无墙钟）。
