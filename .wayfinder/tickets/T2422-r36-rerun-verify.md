---
id: T2422
title: R36 失败项重跑的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2421
created: 2026-09-15
---

## Question

N 会话第 36 轮：如何验收？

## Resolution

EvalRerunFailedTest 两断言：全量（3 项 1 败）→ 失败 id 集重跑 total=1 且
新 runId 与单项；null 子集全量 3 零变化。eval 包 245 用例零回归。
