---
id: T963
title: API 快照 diff 破坏性分级的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

快照门（spec 615）现按集合全等一刀切失败——失败信息不区分「新增类型未入档」（非破坏，正常迭代）与「公共类型消失」（破坏性，须审查）。oasdiff 式破坏性分级怎么落？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 7 轮 = effort #706 / spec 706 / impl 509）：分类器为**纯函数**（测试域内 `SnapshotDiff` record：added/removed 有序清单 + breaking() + gradeMessage()）——expected 有 actual 无 = **removed（破坏性）**；actual 有 expected 无 = **added（非破坏）**。门语义不变（任何 diff 仍失败——不给静默漂移留门），升级的是失败信息：分级计数 + 破坏性清单前置 + 分类处置指引（added→regenerate 即可；removed→审查 api-surface.md + 0.x 语义评估）。借鉴 oasdiff / OpenAPI diff breaking-change grading。
