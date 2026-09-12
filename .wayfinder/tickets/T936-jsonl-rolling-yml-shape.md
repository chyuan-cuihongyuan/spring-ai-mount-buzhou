---
id: T936
title: JSONL 轮转 yml 装配扩散的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-13
---

## Question

spec 642 的 RollingJsonlWriter 已默认保护（64MB×3），但 yml 无细调键——运维不能按磁盘预算调档位或显式关。扩散到哪几个面？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 44 轮 = effort #600 / spec 643 / impl 496）：扩散两个有 yml 装配面的：`buzhou.health.timeline.export-max-bytes / export-max-history`（BuzhouHealthTimelineProperties +2 槽）与 `buzhou.resilience.shadow.detail-max-bytes / detail-max-history`（Shadow record +2 槽）。语义与编程面一致：**键缺席 = 默认 64MB×3；显式 ≤0 = 关**（无界 escape hatch 同口径）。PromptUsageJsonl 无 yml 装配面（纯编程静态工具）——不扩散（非目标）。
