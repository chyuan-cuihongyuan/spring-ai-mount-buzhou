---
id: T2607
title: 裁判位置偏差读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

成对裁判位置自洽度的形状怎么裁决？（spec 1703 / effort #1703 / R4）

## Resolution

**静态纯函数 `JudgePositionBias`（core/eval）**：`PairJudgement(id,
verdictAB, verdictBA)`（Verdict 闭集 A/B/TIE）→ `analyze` 吐 `BiasReport`
四桶（consistent 镜像一致/firstWinsBoth/secondWinsBoth/mixedTie）+
biasRatio（双赢和/pairs，空哨兵 −1）。借鉴 MT-Bench/FastChat 位置偏差检验。
双 TIE 记一致、单 TIE 记 mixedTie（不稳定信号）——纯读面不改 PairwiseJudge。
