---
id: T1045
title: API 快照再生 G 会话中点收口的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

spec 705-746 各轮累计 8 个新公共类未入快照——下次全量 verify 前需再生。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 48 轮 = effort #748 / spec 748 / impl 550）：全量 reactor regenerate（spec 615 口径）+ api-surface.md 同步 8 行（ConfigDiff/RollingMaxCounter/ForkLineageWalker/SessionExportConditional/MessageStoreContract/WebhookRateLimiter/ToolDenialLog/RouteStages）——diff 零移除零意外。
