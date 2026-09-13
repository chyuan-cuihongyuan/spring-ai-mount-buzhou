---
id: T1068
title: 数据集指纹变更信号的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
指纹变了无人提示——加信号吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 35 轮 = effort #734 / spec 734 / impl 634）：EvalRunner 内 checkFingerprintChange——当前 vs 最近历史 run 指纹不同置位 lastFingerprintChanged()+计数+INFO；首跑 false；降级跳过。diff 明细归 EvalRunDiff。
