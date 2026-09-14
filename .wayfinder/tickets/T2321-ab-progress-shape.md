---
id: T2321
title: A/B 对比进度读面（spec 1534 扩散）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2319
created: 2026-09-15
---

## Question

M 会话第 39 轮：A/B compare 的进度观测？

## Resolution

**用户常设授权 AFK（可推翻）**

CompareProgress(runId, done, total, hostCancelled)——串行每项后/波间两处过程快照 + 聚合后终态快照（A/B 的 skipped 是 null 占位无对象——终态统一 done=total，与取消终态一致语义）；progress() 读面（spec 1534 同款）。
