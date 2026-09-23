---
id: T6085
title: R 会话 R43 三方合并的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

双端并发编辑怎么自动归一并显形真冲突？（spec 4042 /
effort #4042 / R43）

## Resolution

**ThreeWayMerge（core/policy）**：git merge-file/diff3 思想——
LCS 行级 diff 出双侧 hunk，单侧取侧、双侧同改归一、相接或
重叠异改即冲突（保守相邻口径）；git 风格标记物化 + 结构化
Conflict 读数。
