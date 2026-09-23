---
id: T6086
title: R 会话 R43 三方合并的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6085]
created: 2026-09-24
---

## Question

R43 合同怎么逐一验绿？（spec 4042 / effort #4042 / R43）

## Resolution

**验证通过**：ThreeWayMergeTest 六测全绿——单侧改取侧/双侧
不同区自动合/双侧同改归一（0 冲突）/同区异改冲突（结构化 +
标记）/同位追加冲突（相邻保守口径）/确定性回放。
