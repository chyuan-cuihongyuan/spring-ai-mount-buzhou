---
id: T1036
title: 评估通过率漂移基线的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

通过率突变无人告警——加跨 run 基线漂移面吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 19 轮 = effort #718 / spec 718 / impl 618）：opt-in `setDriftBaseline(window,warnShift)`——run 完成后取同数据集早于本次的最近 window 次 passRate 均值，|Δ|≥warnShift → WARN+drift.alerts 计数+lastDriftDelta() 读数；无历史跳过；复用 eval.run.* 落盘零新存储；只告警不阻断。Evidently drift 思想。
