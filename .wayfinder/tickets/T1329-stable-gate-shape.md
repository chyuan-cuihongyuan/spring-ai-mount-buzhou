---
id: T1321
title: k 次防抖门的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 43 轮：EvalGate.enforce 单次判定——「k 次全过才过」的防抖门变体是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 43 轮 = effort #943 / spec 943 / impl 692）：缺口成立——flaky 数据集/抖动 judge 下单次判定误报率高，CI 需要「连续 k 次全过」的防抖语义。落点 `EvalGate.enforceStable(datasetName, evaluator, threshold, k)`：循环 k 次 enforce（复用既有管线——落盘/历史/指标全继承），k 次全 passed 才 passed=true；返回最后一次 GateResult（其 runId/passRate 为末次值，全量历史经 history() 可查）。校验 k ∈ [1, 16]（HISTORY_CAPACITY 上限——防抖门判定不入史溢出）。单次门语义零变化。
