---
id: T6127
title: S 会话 S14 Group Commit 组提交的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

顺序化持久怎么合并落盘且持久上沿可证？（spec 5013 /
effort #5013 / S14）

## Resolution

**GroupCommitLog（core/recovery）**：group commit 思想——
append 进当前组分配递增 LSN，sync 一次落盘整组（返回
GroupSync 区间），durableUpto 单调上沿；空组空同步；组空
切换语义显式。
