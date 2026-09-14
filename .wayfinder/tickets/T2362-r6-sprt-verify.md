---
id: T2362
title: R6 A/B 评估 SPRT 序贯提前终止的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2361
created: 2026-09-15
---

## Question

N 会话第 6 轮：如何验收？

## Resolution

PairwiseSprtPolicyTest 四断言：判定器边界（4 胜 CONTINUE/5 胜 PREFER_A/对称
PREFER_B/均势 CONTINUE）；非法 α/β IAE；40 项集成第 5 项停（skipped=35、决策入
summary）；未启用零变化（8 项全量、skipped=0、sprtDecision=null）。eval 包全量
235 用例绿后单轮 commit。
