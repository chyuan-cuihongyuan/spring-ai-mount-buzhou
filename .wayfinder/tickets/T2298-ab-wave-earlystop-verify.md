---
id: T2298
title: A/B 波间早停的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2297
created: 2026-09-15
---

## Question

M 会话第 26 轮：波间早停如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core test -Dtest=PairwiseCancelTest,PairwiseEvalRunnerTest,PairwiseSprtPolicyTest` 绿（16 用例——取消/SPRT/粘住/并行同序零回归）。
