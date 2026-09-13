---
id: T1279
title: gate 判定环形历史读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 15 轮：EvalGate.enforce 每次判定即返回——判定轨迹（何时/何数据集/阈值/通过率/判定结果）不留痕。环形历史读面是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 15 轮 = effort #914 / spec 914 / impl 667）：缺口成立——CI 门禁拒了 PR，host 主侧无法回答「最近拒了几次/趋势如何/阈值是否定严了」。落点 `EvalGate` 实例环形历史：`GateDecision(Instant at, datasetName, runId, threshold, passRate, passed)` + `HISTORY_CAPACITY=16` 有界环（J 会话 R5 MaintenanceGate history 同款纪律：新→旧不可变快照）+ `history()` 读面；enforce 尾部单点记录（判定与记录同临界区语义简化——EvalGate 非并发热点，synchronized 足够诚实）。内存有界、默认随实例存活（无持久化——持久化归 run 记录本身 spec 52 口径不变）。
